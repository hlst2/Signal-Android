/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.backup.v2.ui.subscription

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Represents a "Feature" included for a specific tier of message backups.
 *
 * In the server-private fork there is no paid tier, but [RemoteRestoreActivity]
 * still renders a feature row for free-tier media retention, so the shape stays.
 */
data class MessageBackupsTypeFeature(
  val iconResourceId: Int,
  val label: String
)

@Composable
fun MessageBackupsTypeFeatureRow(
  messageBackupsTypeFeature: MessageBackupsTypeFeature,
  iconTint: Color = LocalContentColor.current,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth()
  ) {
    Icon(
      painter = painterResource(id = messageBackupsTypeFeature.iconResourceId),
      contentDescription = null,
      tint = iconTint,
      modifier = Modifier.padding(end = 8.dp).size(20.dp)
    )

    Text(
      text = messageBackupsTypeFeature.label,
      style = MaterialTheme.typography.bodyLarge
    )
  }
}
