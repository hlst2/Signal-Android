/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.backup.v2.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

/**
 * Stub in server-private fork. Paid backup tier was removed, so the
 * "your media backups subscription expired" sheet can never legitimately fire.
 * Kept so [BackupAlertBottomSheet.create] still resolves the `MediaBackupsAreOff`
 * branch to a DialogFragment.
 */
class MediaBackupsAreOffBottomSheet : DialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = super.onCreateDialog(savedInstanceState)
    dismissAllowingStateLoss()
    return dialog
  }
}
