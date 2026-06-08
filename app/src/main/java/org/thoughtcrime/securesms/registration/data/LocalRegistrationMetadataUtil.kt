/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.registration.data

import okio.ByteString.Companion.toByteString
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.thoughtcrime.securesms.database.model.databaseprotos.LinkedDeviceInfo
import org.thoughtcrime.securesms.database.model.databaseprotos.LocalRegistrationMetadata
import org.whispersystems.signalservice.api.account.PreKeyCollection

/**
 * Takes the two sources of registration data ([RegistrationData], [RegistrationRepository.AccountRegistrationResult])
 * and combines them into a proto-backed class [LocalRegistrationMetadata] so they can be serialized & stored.
 */
object LocalRegistrationMetadataUtil {
  fun createLocalRegistrationMetadata(
    localAciIdentityKeyPair: IdentityKeyPair,
    localPniIdentityKeyPair: IdentityKeyPair,
    registrationData: RegistrationData,
    remoteResult: AccountRegistrationResult,
    reglockEnabled: Boolean,
    linkedDeviceInfo: LinkedDeviceInfo? = null
  ): LocalRegistrationMetadata {
    return LocalRegistrationMetadata.Builder().apply {
      aciIdentityKeyPair = localAciIdentityKeyPair.serialize().toByteString()
      aciSignedPreKey = remoteResult.aciPreKeyCollection.signedPreKey.serialize().toByteString()
      aciLastRestoreKyberPreKey = remoteResult.aciPreKeyCollection.lastResortKyberPreKey.serialize().toByteString()
      pniIdentityKeyPair = localPniIdentityKeyPair.serialize().toByteString()
      pniSignedPreKey = remoteResult.pniPreKeyCollection.signedPreKey.serialize().toByteString()
      pniLastRestoreKyberPreKey = remoteResult.pniPreKeyCollection.lastResortKyberPreKey.serialize().toByteString()
      aci = remoteResult.uuid
      pni = remoteResult.pni
      hasPin = remoteResult.storageCapable
      remoteResult.pin?.let {
        pin = it
      }
      remoteResult.masterKey?.serialize()?.toByteString()?.let {
        masterKey = it
      }
      // Prefer the identifier the server actually assigned to the account. For private/self-hosted
      // deployments this is a synthetic "+999..." number derived from the display name; storing it (rather
      // than the raw display name) keeps the local account identifier stable and consistent with the server.
      // Falls back to the registration-time value for the normal phone-number flow, where they are identical.
      e164 = remoteResult.number.takeIf { it.isNotBlank() } ?: registrationData.e164
      fcmEnabled = registrationData.isFcm
      profileKey = registrationData.profileKey.serialize().toByteString()
      servicePassword = registrationData.password
      this.reglockEnabled = reglockEnabled
      this.linkedDeviceInfo = linkedDeviceInfo
    }.build()
  }

  fun LocalRegistrationMetadata.getAciIdentityKeyPair(): IdentityKeyPair {
    return IdentityKeyPair(aciIdentityKeyPair.toByteArray())
  }

  fun LocalRegistrationMetadata.getPniIdentityKeyPair(): IdentityKeyPair {
    return IdentityKeyPair(pniIdentityKeyPair.toByteArray())
  }

  fun LocalRegistrationMetadata.getAciPreKeyCollection(): PreKeyCollection {
    return PreKeyCollection(
      getAciIdentityKeyPair().publicKey,
      SignedPreKeyRecord(aciSignedPreKey.toByteArray()),
      KyberPreKeyRecord(aciLastRestoreKyberPreKey.toByteArray())
    )
  }

  fun LocalRegistrationMetadata.getPniPreKeyCollection(): PreKeyCollection {
    return PreKeyCollection(
      getPniIdentityKeyPair().publicKey,
      SignedPreKeyRecord(pniSignedPreKey.toByteArray()),
      KyberPreKeyRecord(pniLastRestoreKyberPreKey.toByteArray())
    )
  }
}
