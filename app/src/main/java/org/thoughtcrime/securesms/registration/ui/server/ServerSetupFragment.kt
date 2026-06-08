/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.registration.ui.server

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.signal.core.ui.logging.LoggingFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import org.signal.registration.R as RegistrationR

/**
 * First screen on a fresh install of the private fork. Collects the URL of the user's self-hosted
 * Signal-Server (e.g. `https://10.0.1.50:8443`) and persists it to [SignalStore.customServer]. After the
 * URL is saved, the network module is reset so subsequent calls pick it up, and the user is sent to the
 * regular [WelcomeFragment].
 */
class ServerSetupFragment : LoggingFragment(R.layout.fragment_registration_server_setup) {

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Only auto-skip this screen once the account is actually registered. Skipping merely because a URL was
    // saved would permanently strand an install if the user previously entered a wrong/unreachable URL, since
    // there would be no way back to correct it. Pre-fill the prior value (below) so it can be edited instead.
    if (SignalStore.account.isRegistered) {
      findNavController().safeNavigate(ServerSetupFragmentDirections.goToWelcome())
      return
    }

    val urlField = view.findViewById<TextInputEditText>(R.id.server_setup_url)
    val urlLayout = view.findViewById<TextInputLayout>(R.id.server_setup_url_layout)
    val sfuField = view.findViewById<TextInputEditText>(R.id.server_setup_sfu)
    val continueButton = view.findViewById<MaterialButton>(R.id.server_setup_continue)

    urlField.setText(SignalStore.customServer.serverUrl)
    sfuField.setText(SignalStore.customServer.sfuUrl)

    continueButton.setOnClickListener {
      val raw = urlField.text?.toString()?.trim().orEmpty()
      val normalized = normalize(raw)
      if (normalized == null) {
        urlLayout.error = getString(RegistrationR.string.ServerSetupFragment_invalid_url)
        return@setOnClickListener
      }
      urlLayout.error = null

      // SFU URL is optional. If present, normalize it the same way (https-only); if blank, clear it.
      val rawSfu = sfuField.text?.toString()?.trim().orEmpty()
      val normalizedSfu = if (rawSfu.isBlank()) "" else normalize(rawSfu)
      if (normalizedSfu == null) {
        urlLayout.error = getString(RegistrationR.string.ServerSetupFragment_invalid_url)
        return@setOnClickListener
      }

      SignalStore.customServer.serverUrl = normalized
      SignalStore.customServer.sfuUrl = normalizedSfu
      AppDependencies.resetNetwork()

      findNavController().safeNavigate(ServerSetupFragmentDirections.goToWelcome())
    }
  }

  private fun normalize(raw: String): String? {
    if (raw.isBlank()) return null
    // Reject plaintext HTTP outright: the Signal stack mandates TLS (RESTRICTED_TLS) for every connection, so an
    // http:// URL can never connect and would only produce a confusing later failure. Default missing schemes to https.
    if (raw.startsWith("http://")) return null
    val withScheme = if (raw.startsWith("https://")) raw else "https://$raw"
    return withScheme.trimEnd('/').takeIf { it.length > "https://".length + 1 }
  }
}
