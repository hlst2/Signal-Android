package org.thoughtcrime.securesms;

import android.content.Context;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.dependencies.AppDependencies;
import org.thoughtcrime.securesms.jobmanager.JobManager;
import org.thoughtcrime.securesms.jobs.DeleteAbandonedAttachmentsJob;
import org.thoughtcrime.securesms.jobs.EmojiSearchIndexDownloadJob;
import org.thoughtcrime.securesms.jobs.QuoteThumbnailBackfillJob;
import org.thoughtcrime.securesms.jobs.StickerPackDownloadJob;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
import org.thoughtcrime.securesms.migrations.ApplicationMigrations;
import org.thoughtcrime.securesms.migrations.QuoteThumbnailBackfillMigrationJob;
import org.thoughtcrime.securesms.stickers.BlessedPacks;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.signal.core.util.Util;

/**
 * Rule of thumb: if there's something you want to do on the first app launch that involves
 * persisting state to the database, you'll almost certainly *also* want to do it post backup
 * restore, since a backup restore will wipe the current state of the database.
 */
public final class AppInitialization {

  private static final String TAG = Log.tag(AppInitialization.class);

  private AppInitialization() {}

  public static void onFirstEverAppLaunch(@NonNull Context context) {
    Log.i(TAG, "onFirstEverAppLaunch()");

    TextSecurePreferences.setAppMigrationVersion(context, ApplicationMigrations.CURRENT_VERSION);
    TextSecurePreferences.setJobManagerVersion(context, JobManager.CURRENT_VERSION);
    TextSecurePreferences.setLastVersionCode(context, BuildConfig.VERSION_CODE);
    TextSecurePreferences.setHasSeenStickerIntroTooltip(context, true);
    SignalStore.settings().setPassphraseDisabled(true);
    TextSecurePreferences.setReadReceiptsEnabled(context, true);
    TextSecurePreferences.setTypingIndicatorsEnabled(context, true);
    AppDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    SignalStore.onFirstEverAppLaunch();
    enqueueBlessedPacksIfSupported();
  }

  public static void onPostBackupRestore(@NonNull Context context) {
    Log.i(TAG, "onPostBackupRestore()");

    AppDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    SignalStore.onPostBackupRestore();
    SignalStore.onFirstEverAppLaunch();
    SignalStore.onboarding().clearAll();
    SignalStore.settings().setPassphraseDisabled(true);
    SignalStore.notificationProfile().setHasSeenTooltip(true);
    TextSecurePreferences.onPostBackupRestore(context);
    SignalStore.settings().setPassphraseDisabled(true);
    enqueueBlessedPacksIfSupported();
    EmojiSearchIndexDownloadJob.scheduleImmediately();
    DeleteAbandonedAttachmentsJob.enqueue();

    if (SignalStore.misc().startedQuoteThumbnailMigration()) {
      AppDependencies.getJobManager().add(new QuoteThumbnailBackfillJob());
    } else {
      AppDependencies.getJobManager().add(new QuoteThumbnailBackfillMigrationJob());
    }
  }

  /**
   * Temporary migration method that does the safest bits of {@link #onFirstEverAppLaunch(Context)}
   */
  public static void onRepairFirstEverAppLaunch(@NonNull Context context) {
    Log.w(TAG, "onRepairFirstEverAppLaunch()");

    TextSecurePreferences.setAppMigrationVersion(context, ApplicationMigrations.CURRENT_VERSION);
    TextSecurePreferences.setJobManagerVersion(context, JobManager.CURRENT_VERSION);
    TextSecurePreferences.setLastVersionCode(context, BuildConfig.VERSION_CODE);
    TextSecurePreferences.setHasSeenStickerIntroTooltip(context, true);
    SignalStore.settings().setPassphraseDisabled(true);
    AppDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    SignalStore.onFirstEverAppLaunch();
    enqueueBlessedPacksIfSupported();
  }

  /**
   * server-private fork: BlessedPacks live on cdn.signal.org. A private deployment has no CDN
   * configured (BuildConfig.SIGNAL_CDN_URL is intentionally blank), so enqueuing these jobs only
   * leads to StickerPackDownloadJob throwing IllegalArgumentException at OkHttp and killing the
   * process. Skip the seed entirely on private deployments.
   */
  private static void enqueueBlessedPacksIfSupported() {
    if (SignalStore.customServer().isConfigured()) {
      Log.i(TAG, "Private deployment; skipping BlessedPacks seed (no CDN).");
      return;
    }
    AppDependencies.getJobManager().addAll(BlessedPacks.getFirstInstallJobs());
  }
}
