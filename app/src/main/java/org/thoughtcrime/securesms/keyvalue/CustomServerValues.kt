/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.keyvalue

/**
 * Stores the runtime-configurable URL for a private Signal-Server deployment. The user enters this on first
 * boot. All sub-services (chat, storage, CDN, etc.) are derived from the same base URL — the private server
 * is expected to route paths internally.
 */
class CustomServerValues(store: KeyValueStore) : SignalStoreValues(store) {

  companion object {
    private const val KEY_SERVER_URL = "customServer.url"
  }

  var serverUrl: String by stringValue(KEY_SERVER_URL, "")

  val isConfigured: Boolean
    get() = serverUrl.isNotBlank()

  public override fun onFirstEverAppLaunch() = Unit

  public override fun getKeysToIncludeInBackup(): List<String> = listOf(KEY_SERVER_URL)
}
