package org.thoughtcrime.securesms;

import android.net.Uri;
import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.components.settings.app.AppSettingsActivity;
import org.thoughtcrime.securesms.linkdevice.LinkDeviceRepository;

public class DeviceProvisioningActivity extends PassphraseRequiredActivity {

  @SuppressWarnings("unused")
  private static final String TAG = Log.tag(DeviceProvisioningActivity.class);

  @Override
  protected void onPreCreate() {
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
  }

  @Override
  protected void onCreate(Bundle bundle, boolean ready) {
    AlertDialog dialog = new MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.DeviceProvisioningActivity_link_a_signal_device))
        .setMessage(getString(R.string.DeviceProvisioningActivity_to_link_a_desktop_or_ipad_to_this_signal_account))
        .setPositiveButton(R.string.DeviceProvisioningActivity_continue, (dialog1, which) -> {
          final Uri linkUri = getIntent() != null ? getIntent().getData() : null;
          if (linkUri != null) {
            // Complete the link directly from the provisioning URI (sgnl://linkdevice?uuid=...&pub_key=...),
            // mirroring the QR-scan path (LinkDeviceRepository.addDevice). This lets the link complete via the
            // deep link without a camera scan. No message-history sync (null backup key).
            SignalExecutors.BOUNDED.execute(() -> {
              try {
                Object result = LinkDeviceRepository.INSTANCE.addDevice(linkUri, null);
                Log.i(TAG, "Deep-link addDevice result: " + result.getClass().getSimpleName());
              } catch (Throwable t) {
                Log.w(TAG, "Deep-link addDevice failed", t);
              }
            });
          } else {
            startActivity(AppSettingsActivity.linkedDevices(this));
          }
          finish();
        })
        .setNegativeButton(android.R.string.cancel, (dialog12, which) -> {
          dialog12.dismiss();
          finish();
        })
        .setOnDismissListener(dialog13 -> finish())
        .create();

    dialog.show();
  }
}
