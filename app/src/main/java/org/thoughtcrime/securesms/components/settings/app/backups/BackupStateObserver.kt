/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.backups

import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.thoughtcrime.securesms.backup.v2.ui.subscription.MessageBackupsType
import org.thoughtcrime.securesms.keyvalue.SignalStore
import kotlin.time.Duration.Companion.seconds

/**
 * In server-private fork there is no paid backup tier and no subscription state
 * to observe from the server. This is a stub that always reports either
 * [BackupState.ActiveFree] (when backups are locally enabled) or [BackupState.None].
 *
 * Public surface preserved: `(scope)` and `(scope, useDatabaseFallbackOnNetworkError)`
 * constructors, `backupState: StateFlow<BackupState>`, and the
 * `getNonIOBackupState()` / `notifyBackupStateChanged()` companion helpers.
 */
class BackupStateObserver(
  @Suppress("UNUSED_PARAMETER") scope: CoroutineScope,
  @Suppress("UNUSED_PARAMETER") useDatabaseFallbackOnNetworkError: Boolean = false
) {

  companion object {
    private const val DEFAULT_FREE_TIER_MEDIA_DAYS = 30
    private val staticScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @JvmStatic
    @JvmOverloads
    @Suppress("UNUSED_PARAMETER")
    fun notifyBackupStateChanged(scope: CoroutineScope = staticScope) = Unit

    @WorkerThread
    @JvmStatic
    fun getNonIOBackupState(): BackupState = computeFreeTierBackupState()

    private fun computeFreeTierBackupState(): BackupState {
      val tier = SignalStore.backup.backupTier
      return if (tier == null) {
        BackupState.None
      } else {
        BackupState.ActiveFree(
          messageBackupsType = MessageBackupsType.Free(mediaRetentionDays = DEFAULT_FREE_TIER_MEDIA_DAYS),
          renewalTime = 0.seconds
        )
      }
    }
  }

  // Reported once; nothing produces updates because there's no remote subscription state.
  val backupState: StateFlow<BackupState> = MutableStateFlow(computeFreeTierBackupState()).asStateFlow()
}
