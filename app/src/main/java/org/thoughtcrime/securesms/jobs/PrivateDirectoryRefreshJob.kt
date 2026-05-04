/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.jobs

import org.signal.core.models.ServiceId.ACI
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.jobmanager.Job
import org.thoughtcrime.securesms.jobmanager.impl.NetworkConstraint
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.profiles.ProfileName
import org.whispersystems.signalservice.api.NetworkResult

/**
 * Replaces the upstream CDS-based [DirectoryRefreshJob] for private deployments. Calls
 * `GET /v1/accounts/directory` and upserts every entry as a registered local [Recipient] with the
 * server-provided display name as the profile name. After the job runs, the existing contact picker
 * UI surfaces the directory because it queries `RecipientTable.querySignalContacts(...)` filtered by
 * `REGISTERED = REGISTERED.id` and a non-null sort name.
 */
class PrivateDirectoryRefreshJob private constructor(parameters: Parameters) : BaseJob(parameters) {

  constructor() : this(
    Parameters.Builder()
      .setQueue(QUEUE_KEY)
      .setMaxInstancesForQueue(2)
      .addConstraint(NetworkConstraint.KEY)
      .setMaxAttempts(5)
      .build()
  )

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun onShouldRetry(exception: Exception): Boolean = false

  override fun onFailure() = Unit

  override fun onRun() {
    if (!SignalStore.account.isRegistered) {
      Log.i(TAG, "Skipping directory refresh; account is not registered.")
      return
    }

    val result = AppDependencies.accountApi.getDirectory()
    if (result !is NetworkResult.Success) {
      Log.w(TAG, "Directory fetch failed: $result")
      return
    }

    val ownAci = SignalStore.account.aci
    val recipients = SignalDatabase.recipients

    var inserted = 0
    var updated = 0

    for (entry in result.result.entries) {
      val aci = try {
        ACI.parseOrThrow(entry.aci)
      } catch (t: Throwable) {
        Log.w(TAG, "Skipping invalid ACI in directory: ${entry.aci}", t)
        continue
      }
      if (aci == ownAci) continue

      val recipientId = recipients.getOrInsertFromServiceId(aci)
      val merged = recipients.markRegistered(recipientId, aci)
      val resolvedId = if (merged) recipients.getOrInsertFromServiceId(aci) else recipientId

      val displayName = entry.displayName?.takeIf { it.isNotBlank() }
      if (displayName != null) {
        recipients.setProfileName(resolvedId, ProfileName.asGiven(displayName))
      }
      recipients.setProfileSharing(resolvedId, true)

      if (merged) updated++ else inserted++
    }

    Log.i(TAG, "Directory refresh complete. inserted/updated=$inserted, merged=$updated, total=${result.result.entries.size}")
  }

  class Factory : Job.Factory<PrivateDirectoryRefreshJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): PrivateDirectoryRefreshJob {
      return PrivateDirectoryRefreshJob(parameters)
    }
  }

  companion object {
    const val KEY = "PrivateDirectoryRefreshJob"
    const val QUEUE_KEY = "PrivateDirectoryRefresh"
    private val TAG = Log.tag(PrivateDirectoryRefreshJob::class.java)

    @JvmStatic
    fun enqueue() {
      AppDependencies.jobManager.add(PrivateDirectoryRefreshJob())
    }
  }
}
