/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.dependencies

import android.content.Context
import org.signal.core.util.billing.BillingDependencies
import org.signal.core.util.billing.BillingError

/**
 * Stubbed BillingDependencies for server-private fork (no paid backup tier, no
 * Google Play Billing). BillingFactory.create returns BillingApi.Empty when
 * `Environment.Backups.supportsGooglePlayBilling()` is false (which it always is
 * in this fork), so these methods should never actually be called.
 */
object GooglePlayBillingDependencies : BillingDependencies {

  private const val BILLING_PRODUCT_ID_NOT_AVAILABLE = -1000

  override val context: Context get() = AppDependencies.application

  override suspend fun getProductId(): String = throw BillingError(BILLING_PRODUCT_ID_NOT_AVAILABLE)

  override suspend fun getBasePlanId(): String = "monthly"
}
