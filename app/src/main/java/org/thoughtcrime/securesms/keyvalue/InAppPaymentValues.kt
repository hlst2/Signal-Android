package org.thoughtcrime.securesms.keyvalue

import java.util.Currency
import java.util.Locale

/**
 * Stub for the original InAppPaymentValues. server-private has no donations,
 * subscriptions, IAP, or paid backups, so almost everything that lived here is gone.
 *
 * The few methods that remain on the surface area are no-ops or trivial defaults so
 * the handful of callers that survived the cleanup (BackupRepository,
 * AccountDataArchiveProcessor, BadgeRepository) still link.
 */
class InAppPaymentValues internal constructor(store: KeyValueStore) : SignalStoreValues(store) {

  override fun onFirstEverAppLaunch() = Unit

  override fun getKeysToIncludeInBackup(): MutableList<String> = mutableListOf()

  fun getRecurringDonationCurrency(): Currency = Currency.getInstance(Locale.getDefault())

  fun setDisplayBadgesOnProfile(value: Boolean) = Unit

  fun updateLocalStateForManualCancellation(@Suppress("UNUSED_PARAMETER") type: Any) = Unit
}
