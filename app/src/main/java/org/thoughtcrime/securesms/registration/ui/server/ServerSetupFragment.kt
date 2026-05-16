/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.registration.ui.server

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.signal.core.ui.logging.LoggingFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.registration.data.network.RegisterAccountResult
import org.thoughtcrime.securesms.registration.ui.RegistrationCheckpoint
import org.thoughtcrime.securesms.registration.ui.RegistrationViewModel
import org.thoughtcrime.securesms.registration.ui.welcome.WelcomeUserSelection
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import org.signal.registration.R as RegistrationR

/**
 * First screen on a fresh install of the private fork. Collects both the URL of the user's
 * self-hosted Signal-Server (e.g. `https://10.0.1.50:8443`) and the display name to register as,
 * then attempts the private registration in one shot. This lets the user catch a mistyped server
 * URL before being walked through the welcome / permissions screens.
 *
 * On a successful register the user is sent to [WelcomeFragment]. On failure the inputs stay on
 * screen so the URL or name can be corrected.
 */
class ServerSetupFragment : LoggingFragment(R.layout.fragment_registration_server_setup) {

  private val sharedViewModel by activityViewModels<RegistrationViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // If we've already registered and the user reopens the registration flow somehow,
    // skip straight to the welcome screen and let the activity-level observer carry them
    // through to MainActivity. Exception: when re-registering (e.g. after a 401 deregisters
    // the account), we must show the setup screen again so the user re-confirms the URL +
    // name and we re-hit /v1/registration/private to refresh credentials. Otherwise the
    // checkpoint never advances past INITIALIZATION on the new RegistrationActivity instance
    // and the Welcome→MainActivity gate never opens.
    if (SignalStore.account.isRegistered && !sharedViewModel.isReregister) {
      findNavController().safeNavigate(ServerSetupFragmentDirections.goToWelcome())
      return
    }

    val urlField = view.findViewById<TextInputEditText>(R.id.server_setup_url)
    val urlLayout = view.findViewById<TextInputLayout>(R.id.server_setup_url_layout)
    val nameField = view.findViewById<TextInputEditText>(R.id.server_setup_display_name)
    val nameLayout = view.findViewById<TextInputLayout>(R.id.server_setup_display_name_layout)
    val continueButton = view.findViewById<MaterialButton>(R.id.server_setup_continue)
    val progress = view.findViewById<ProgressBar>(R.id.server_setup_progress)

    urlField.setText(SignalStore.customServer.serverUrl)

    continueButton.setOnClickListener {
      val raw = urlField.text?.toString()?.trim().orEmpty()
      val normalized = normalize(raw)
      val displayName = nameField.text?.toString()?.trim().orEmpty()

      var hasError = false
      if (normalized == null) {
        urlLayout.error = getString(RegistrationR.string.ServerSetupFragment_invalid_url)
        hasError = true
      } else {
        urlLayout.error = null
      }
      if (displayName.isBlank()) {
        nameLayout.error = getString(RegistrationR.string.EnterDisplayNameFragment_blank)
        hasError = true
      } else {
        nameLayout.error = null
      }
      if (hasError || normalized == null) return@setOnClickListener

      SignalStore.customServer.serverUrl = normalized
      AppDependencies.resetNetwork()

      sharedViewModel.registerPrivateAccount(requireContext(), displayName)
    }

    sharedViewModel.uiState.observe(viewLifecycleOwner) { state ->
      val inProgress = state.inProgress
      continueButton.isEnabled = !inProgress
      urlField.isEnabled = !inProgress
      nameField.isEnabled = !inProgress
      progress.visibility = if (inProgress) View.VISIBLE else View.GONE

      val error = state.registerAccountError
      if (error != null && error !is RegisterAccountResult.Success) {
        Toast.makeText(requireContext(), RegistrationR.string.EnterDisplayNameFragment_failed, Toast.LENGTH_LONG).show()
        sharedViewModel.registerAccountErrorShown()
      }

      // Registration succeeded — go straight to permissions, then MainActivity. The legacy
      // WelcomeFragment ("Continue / Transfer or restore") doesn't apply to a private deployment
      // and we already collected the user's display name on this screen.
      if (state.registrationCheckpoint >= RegistrationCheckpoint.SERVICE_REGISTRATION_COMPLETED && !inProgress) {
        findNavController().safeNavigate(
          ServerSetupFragmentDirections.goToGrantPermissions(WelcomeUserSelection.CONTINUE)
        )
      }
    }
  }

  private fun normalize(raw: String): String? {
    if (raw.isBlank()) return null
    val withScheme = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
    return withScheme.trimEnd('/').takeIf { it.length > "https://".length + 1 }
  }
}
