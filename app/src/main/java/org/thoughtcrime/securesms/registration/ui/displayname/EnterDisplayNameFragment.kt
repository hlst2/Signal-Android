/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.registration.ui.displayname

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.signal.core.ui.logging.LoggingFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.registration.data.network.RegisterAccountResult
import org.thoughtcrime.securesms.registration.ui.RegistrationViewModel
import org.signal.registration.R as RegistrationR

/**
 * Replaces the SMS-based phone-number entry on a private deployment. The user picks a public display
 * name, which the server uses to derive a stable account identifier and as the directory entry for
 * other users to find this account.
 */
class EnterDisplayNameFragment : LoggingFragment(R.layout.fragment_registration_enter_display_name) {

  private val viewModel by activityViewModels<RegistrationViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val nameField = view.findViewById<TextInputEditText>(R.id.display_name_input)
    val nameLayout = view.findViewById<TextInputLayout>(R.id.display_name_layout)
    val continueButton = view.findViewById<MaterialButton>(R.id.display_name_continue)
    val progress = view.findViewById<ProgressBar>(R.id.display_name_progress)

    continueButton.setOnClickListener {
      val displayName = nameField.text?.toString()?.trim().orEmpty()
      if (displayName.isBlank()) {
        nameLayout.error = getString(RegistrationR.string.EnterDisplayNameFragment_blank)
        return@setOnClickListener
      }
      nameLayout.error = null
      viewModel.registerPrivateAccount(requireContext(), displayName)
    }

    viewModel.uiState.observe(viewLifecycleOwner) { state ->
      val inProgress = state.inProgress
      continueButton.isEnabled = !inProgress
      nameField.isEnabled = !inProgress
      progress.visibility = if (inProgress) View.VISIBLE else View.GONE

      val error = state.registerAccountError
      if (error != null && error !is RegisterAccountResult.Success) {
        Toast.makeText(requireContext(), RegistrationR.string.EnterDisplayNameFragment_failed, Toast.LENGTH_LONG).show()
        viewModel.registerAccountErrorShown()
      }
    }
  }
}
