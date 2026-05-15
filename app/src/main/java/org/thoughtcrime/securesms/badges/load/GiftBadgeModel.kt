/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.badges.load

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import okhttp3.OkHttpClient
import org.thoughtcrime.securesms.database.model.databaseprotos.GiftBadge
import org.thoughtcrime.securesms.dependencies.AppDependencies
import java.io.InputStream
import java.security.MessageDigest

data class GiftBadgeModel(val giftBadge: GiftBadge) : Key {
  class Loader(@Suppress("unused") val client: OkHttpClient) : ModelLoader<GiftBadgeModel, InputStream> {
    override fun buildLoadData(model: GiftBadgeModel, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
      return ModelLoader.LoadData(model, Fetcher())
    }

    override fun handles(model: GiftBadgeModel): Boolean = true
  }

  override fun updateDiskCacheKey(messageDigest: MessageDigest) {
    messageDigest.update(giftBadge.encode())
  }

  class Fetcher : DataFetcher<InputStream> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
      callback.onLoadFailed(UnsupportedOperationException("Gift badges are not supported on server-private."))
    }

    override fun cleanup() = Unit
    override fun cancel() = Unit
    override fun getDataClass(): Class<InputStream> = InputStream::class.java
    override fun getDataSource(): DataSource = DataSource.REMOTE
  }

  class Factory(private val client: OkHttpClient) : ModelLoaderFactory<GiftBadgeModel, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GiftBadgeModel, InputStream> {
      return Loader(client)
    }

    override fun teardown() {}
  }

  companion object {
    @JvmStatic
    fun createFactory(): Factory {
      return Factory(AppDependencies.signalOkHttpClient)
    }
  }
}
