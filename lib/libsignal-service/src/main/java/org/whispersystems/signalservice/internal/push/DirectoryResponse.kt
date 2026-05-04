/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.signalservice.internal.push

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response object for `GET /v1/accounts/directory`, the private-deployment endpoint that lists every
 * registered account on the server.
 */
data class DirectoryResponse @JsonCreator constructor(
  @JsonProperty val entries: List<Entry> = emptyList()
) {
  data class Entry @JsonCreator constructor(
    @JsonProperty val aci: String,
    @JsonProperty val displayName: String? = null
  )
}
