package org.thoughtcrime.securesms.delete;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import org.signal.core.util.E164Util;
import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.database.GroupTable;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.database.model.GroupRecord;
import org.thoughtcrime.securesms.dependencies.AppDependencies;
import org.thoughtcrime.securesms.groups.GroupManager;
import org.thoughtcrime.securesms.net.SignalNetwork;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.whispersystems.signalservice.api.NetworkResultUtil;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.IOException;

public class DeleteAccountRepository {
  private static final String TAG = Log.tag(DeleteAccountRepository.class);

  public DeleteAccountRepository() {}

  @NonNull String getRegionDisplayName(@NonNull String region) {
    return E164Util.getRegionDisplayName(region).orElse("");
  }

  int getRegionCountryCode(@NonNull String region) {
    return PhoneNumberUtil.getInstance().getCountryCodeForRegion(region);
  }

  public void deleteAccount(@NonNull Consumer<DeleteAccountEvent> onDeleteAccountEvent) {
    SignalExecutors.BOUNDED.execute(() -> {
      Log.i(TAG, "deleteAccount: attempting to leave groups...");

      int groupsLeft = 0;
      try (GroupTable.Reader groups = SignalDatabase.groups().getGroups()) {
        GroupRecord groupRecord = groups.getNext();
        onDeleteAccountEvent.accept(new DeleteAccountEvent.LeaveGroupsProgress(groups.getCount(), 0));
        Log.i(TAG, "deleteAccount: found " + groups.getCount() + " groups to leave.");

        while (groupRecord != null) {
          if (groupRecord.getId().isPush() && groupRecord.isActive()) {
            if (!groupRecord.isV1Group()) {
              GroupManager.leaveGroup(AppDependencies.getApplication(), groupRecord.getId().requirePush(), true);
            }
            onDeleteAccountEvent.accept(new DeleteAccountEvent.LeaveGroupsProgress(groups.getCount(), ++groupsLeft));
          }

          groupRecord = groups.getNext();
        }

        onDeleteAccountEvent.accept(DeleteAccountEvent.LeaveGroupsFinished.INSTANCE);
      } catch (Exception e) {
        Log.w(TAG, "deleteAccount: failed to leave groups", e);
        onDeleteAccountEvent.accept(DeleteAccountEvent.LeaveGroupsFailed.INSTANCE);
        return;
      }

      Log.i(TAG, "deleteAccount: successfully left all groups.");
      Log.i(TAG, "deleteAccount: attempting to delete account from server...");

      try {
        NetworkResultUtil.toBasicLegacy(SignalNetwork.account().deleteAccount());
      } catch (IOException e) {
        if (e instanceof NonSuccessfulResponseCodeException && ((NonSuccessfulResponseCodeException) e).code == 4401) {
          Log.i(TAG, "deleteAccount: WebSocket closed with expected status after delete account, moving forward as delete was successful");
        } else {
          Log.w(TAG, "deleteAccount: failed to delete account from signal service, bail", e);
          onDeleteAccountEvent.accept(DeleteAccountEvent.ServerDeletionFailed.INSTANCE);
          return;
        }
      }

      Log.i(TAG, "deleteAccount: successfully removed account from server");
      Log.i(TAG, "deleteAccount: scheduling launcher restart so user lands on ServerSetupFragment after wipe...");

      // server-private fork: clearApplicationUserData() kills the process. Without this scheduled
      // intent the launcher would be left empty and the user would have to manually re-tap the app
      // icon to land on ServerSetupFragment. Schedule a one-shot AlarmManager wakeup ~500ms out so
      // Android relaunches us automatically after the wipe completes.
      Application application = AppDependencies.getApplication();
      scheduleLauncherRestart(application);

      Log.i(TAG, "deleteAccount: attempting to delete user data and close process...");
      if (!ServiceUtil.getActivityManager(application).clearApplicationUserData()) {
        Log.w(TAG, "deleteAccount: failed to delete user data");
        onDeleteAccountEvent.accept(DeleteAccountEvent.LocalDataDeletionFailed.INSTANCE);
      }
    });
  }

  private static void scheduleLauncherRestart(@NonNull Context context) {
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
    if (launchIntent == null) {
      Log.w(TAG, "scheduleLauncherRestart: no launch intent for package; user will have to reopen manually.");
      return;
    }
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

    PendingIntent pendingIntent = PendingIntent.getActivity(
        context,
        0,
        launchIntent,
        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (alarmManager == null) {
      Log.w(TAG, "scheduleLauncherRestart: no AlarmManager service; user will have to reopen manually.");
      return;
    }
    alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);
  }
}
