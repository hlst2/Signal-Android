package org.thoughtcrime.securesms.keyvalue

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.thoughtcrime.securesms.payments.Balance
import org.thoughtcrime.securesms.payments.Entropy

/**
 * Stub in server-private fork. The entire wallet (MobileCoin) feature was
 * removed: no mobilecoin SDK calls, no on-disk ledger, no payment lock.
 * Public surface preserved so the handful of callers outside the deleted
 * payments package (StorageSyncHelper, DeleteAccountViewModel,
 * AppSettingsState, PrivacySettingsViewModel, ConversationUpdateItem,
 * AttachmentKeyboardFragment) keep compiling.
 */
class PaymentsValues internal constructor(store: KeyValueStore) : SignalStoreValues(store) {

  public override fun onFirstEverAppLaunch() = Unit

  public override fun getKeysToIncludeInBackup(): MutableList<String> = mutableListOf()

  fun mobileCoinPaymentsEnabled(): Boolean = false

  var paymentLock: Boolean = false

  val paymentsAvailability: PaymentsAvailability = PaymentsAvailability.DISABLED_REMOTELY

  val paymentsEntropy: Entropy? = null

  val userConfirmedMnemonic: Boolean = false

  private val liveBalance = MutableLiveData(Balance.ZERO)
  fun liveMobileCoinBalance(): LiveData<Balance> = liveBalance

  fun setEnabledAndEntropy(@Suppress("UNUSED_PARAMETER") enabled: Boolean, @Suppress("UNUSED_PARAMETER") entropy: Entropy?) = Unit
}
