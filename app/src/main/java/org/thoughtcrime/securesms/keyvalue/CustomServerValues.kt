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
    private const val KEY_SFU_URL = "customServer.sfuUrl"
  }

  var serverUrl: String by stringValue(KEY_SERVER_URL, "")

  /**
   * Optional URL of a self-hosted calling SFU (Signal-Calling-Service) used for group calls / call links. The SFU
   * is a separate service from the chat server, so it is not derived from [serverUrl]. When blank, group calling
   * falls back to the build-time default (Signal's public SFU), which is unreachable on an isolated deployment.
   */
  var sfuUrl: String by stringValue(KEY_SFU_URL, "")

  val isConfigured: Boolean
    get() = serverUrl.isNotBlank()

  public override fun onFirstEverAppLaunch() = Unit

  public override fun getKeysToIncludeInBackup(): List<String> = listOf(KEY_SERVER_URL, KEY_SFU_URL)
}
