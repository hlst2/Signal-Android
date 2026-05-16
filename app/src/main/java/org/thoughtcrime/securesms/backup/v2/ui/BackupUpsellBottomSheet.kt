/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.backup.v2.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

/**
 * Stub in server-private fork. Paid backup tier was removed, so this upsell never displays.
 *
 * Public API (`create()`) preserved so existing callers (Megaphones.java
 * buildBackupMediaSizeUpsellMegaphone / buildBackupLowStorageUpsellMegaphone) keep
 * compiling. The corresponding megaphone schedules should also be flipped to NEVER
 * elsewhere; this is the belt-and-suspenders side.
 */
class BackupUpsellBottomSheet : DialogFragment() {

  companion object {
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun create(showPostPaymentSheet: Boolean): DialogFragment = BackupUpsellBottomSheet()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = super.onCreateDialog(savedInstanceState)
    dismissAllowingStateLoss()
    return dialog
  }
}
