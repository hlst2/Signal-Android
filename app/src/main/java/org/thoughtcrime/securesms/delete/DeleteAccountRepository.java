package org.thoughtcrime.securesms.delete;

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
      Log.i(TAG, "deleteAccount: attempting to delete user data and close process...");

      if (!ServiceUtil.getActivityManager(AppDependencies.getApplication()).clearApplicationUserData()) {
        Log.w(TAG, "deleteAccount: failed to delete user data");
        onDeleteAccountEvent.accept(DeleteAccountEvent.LocalDataDeletionFailed.INSTANCE);
      }
    });
  }
}
