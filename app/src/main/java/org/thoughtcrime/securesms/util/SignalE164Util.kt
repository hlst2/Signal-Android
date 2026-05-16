/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.util

import org.signal.core.util.BidiUtil
import org.signal.core.util.E164Util
import org.signal.core.util.LRUCache
import org.signal.core.util.Util
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore

/**
 * A wrapper around [E164Util] that automatically handles fetching our own number and caching formatters.
 */
object SignalE164Util {

  private val cachedFormatters: MutableMap<String, E164Util.Formatter> = LRUCache(2)
  private val defaultFormatter: E164Util.Formatter by lazy {
    E164Util.Formatter(
      localNumber = null,
      localAreaCode = null,
      localRegionCode = Util.getSimCountryIso(AppDependencies.application).orElse("US")
    )
  }

  /**
   * Formats the number for human-readable display. e.g. "(555) 555-5555"
   *
   * server-private fork: on a self-hosted deployment the account's stored "E164" is
   * actually a synthetic identifier derived from the user's display name, not a real
   * phone number. libphonenumber throws NumberParseException on those. Fall back to
   * the raw input so the UI shows _something_ instead of taking down the activity.
   */
  @JvmStatic
  fun prettyPrint(input: String): String {
    return try {
      BidiUtil.forceLtr(getFormatter().prettyPrint(input))
    } catch (e: Exception) {
      BidiUtil.forceLtr(input)
    }
  }

  /**
   * Returns the country code for the local number, if present. Otherwise, it returns 0.
   */
  fun getLocalCountryCode(): Int {
    return try {
      getFormatter().localNumber?.countryCode ?: 0
    } catch (e: Exception) {
      0
    }
  }

  /**
   * Formats the number as an E164, or null if the number cannot be reasonably interpreted as a phone number.
   *
   * server-private fork: see getFormatter() — synthetic identifiers cause libphonenumber to throw, and the
   * exception used to escape into registerAccountLocally → trustedPush → RecipientTable.getAndPossiblyMerge,
   * aborting the registration's local bookkeeping after a successful server-side register and leaving the
   * checkpoint stuck at SERVICE_REGISTRATION_COMPLETED. Catch and return null (the existing "not a phone
   * number" semantics) so callers handle it gracefully.
   */
  @JvmStatic
  fun formatAsE164(input: String): String? {
    return try {
      getFormatter().formatAsE164(input)
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Formats the number as an E164, or null if the number cannot be reasonably interpreted as a phone number,
   * or if the number is a shortcode (<= 6 digits, excluding leading '+' and zeroes).
   */
  @JvmStatic
  fun formatNonShortCodeAsE164(input: String): String? {
    return try {
      val formatter = getFormatter()
      formatter.formatAsE164(input)?.takeIf { !formatter.isValidShortNumber(input) }
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Returns true if the input string can be considered an E164. Specifically, it returns true if we could figure out how to format it as an E164.
   */
  @JvmStatic
  fun isPotentialE164(input: String): Boolean {
    return formatAsE164(input) != null
  }

  /**
   * Performs the same check as [isPotentialE164], with the additional validation to check if there are more than 6 digits in the number.
   * When counting digits, leading zeroes and '+' will be ignored.
   */
  fun isPotentialNonShortCodeE164(input: String): Boolean {
    return formatNonShortCodeAsE164(input) != null
  }

  /**
   * server-private fork: the account's stored "E164" on private deployments is a synthetic
   * identifier derived from the user's display name. [E164Util.createFormatterForE164] calls
   * [com.google.i18n.phonenumbers.PhoneNumberUtil.parse] under the hood and throws
   * NumberParseException on anything that doesn't look like a phone number. Catch and fall
   * back to the [defaultFormatter] (which uses null localNumber + the SIM country) so the
   * exception doesn't escape into registration's onSuccessfulRegistration path and abort
   * the local bookkeeping with the checkpoint still at SERVICE_REGISTRATION_COMPLETED.
   */
  private fun getFormatter(): E164Util.Formatter {
    val localNumber = SignalStore.account.e164 ?: return defaultFormatter
    val formatter = cachedFormatters[localNumber]
    if (formatter != null) {
      return formatter
    }

    synchronized(cachedFormatters) {
      val formatter = cachedFormatters[localNumber]
      if (formatter != null) {
        return formatter
      }

      val newFormatter = try {
        E164Util.createFormatterForE164(localNumber)
      } catch (e: Exception) {
        defaultFormatter
      }
      cachedFormatters[localNumber] = newFormatter
      return newFormatter
    }
  }
}
