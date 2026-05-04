package org.thoughtcrime.securesms.push

import android.content.Context
import android.net.ConnectivityManager
import android.net.ProxyInfo
import android.net.Uri
import androidx.core.content.ContextCompat
import okhttp3.Dns
import okhttp3.Interceptor
import org.signal.core.util.Base64
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.BuildConfig
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.net.DeprecatedClientPreventionInterceptor
import org.thoughtcrime.securesms.net.DeviceTransferBlockingInterceptor
import org.thoughtcrime.securesms.net.RemoteDeprecationDetectorInterceptor
import org.thoughtcrime.securesms.net.StandardUserAgentInterceptor
import org.thoughtcrime.securesms.net.StorageServiceSizeLoggingInterceptor
import org.whispersystems.signalservice.api.push.TrustStore
import org.whispersystems.signalservice.internal.configuration.HttpProxy
import org.whispersystems.signalservice.internal.configuration.SignalCdnUrl
import org.whispersystems.signalservice.internal.configuration.SignalCdsiUrl
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration
import org.whispersystems.signalservice.internal.configuration.SignalServiceUrl
import org.whispersystems.signalservice.internal.configuration.SignalStorageUrl
import org.whispersystems.signalservice.internal.configuration.SignalSvr2Url
import java.io.IOException
import java.util.Optional

/**
 * Provides a [SignalServiceConfiguration] for a private, self-hosted deployment. The base URL is read at
 * call time from [SignalStore.customServer] so changes made via the first-boot setup screen take effect
 * after [org.thoughtcrime.securesms.dependencies.AppDependencies.resetNetwork].
 *
 * All censorship-circumvention behavior has been removed: a private deployment behind WireGuard does not
 * need domain fronting. Sub-services (storage, CDN, CDSI, SVR2) all point at the same host — the private
 * server is expected to route paths server-side.
 */
class SignalServiceNetworkAccess(context: Context) {
  companion object {
    private val TAG = Log.tag(SignalServiceNetworkAccess::class.java)

    @JvmField
    val DNS: Dns = Dns.SYSTEM

    @Suppress("DEPRECATION")
    private fun getSystemHttpProxy(context: Context): HttpProxy? {
      val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java) ?: return null

      val proxyInfo = connectivityManager
        .activeNetwork
        ?.let { connectivityManager.getLinkProperties(it)?.httpProxy }

      return proxyInfo.toApplicableSystemHttpProxy()
    }

    fun ProxyInfo?.toApplicableSystemHttpProxy(): HttpProxy? {
      return this
        ?.takeIf { it.pacFileUrl == Uri.EMPTY }
        ?.let { proxy -> HttpProxy(proxy.host, proxy.port) }
    }
  }

  private val context: Context = context.applicationContext

  private val serviceTrustStore: TrustStore = SignalServiceTrustStore(context)

  private val interceptors: List<Interceptor> = listOf(
    StandardUserAgentInterceptor(),
    StorageServiceSizeLoggingInterceptor(),
    RemoteDeprecationDetectorInterceptor(this::getConfiguration),
    DeprecatedClientPreventionInterceptor(),
    DeviceTransferBlockingInterceptor.getInstance()
  )

  private val zkGroupServerPublicParams: ByteArray = try {
    Base64.decode(BuildConfig.ZKGROUP_SERVER_PUBLIC_PARAMS)
  } catch (e: IOException) {
    throw AssertionError(e)
  }

  private val genericServerPublicParams: ByteArray = try {
    Base64.decode(BuildConfig.GENERIC_SERVER_PUBLIC_PARAMS)
  } catch (e: IOException) {
    throw AssertionError(e)
  }

  private val backupServerPublicParams: ByteArray = try {
    Base64.decode(BuildConfig.BACKUP_SERVER_PUBLIC_PARAMS)
  } catch (e: IOException) {
    throw AssertionError(e)
  }

  /**
   * Resolves the base URL the user entered on first boot, falling back to the build-time URL if nothing has
   * been configured (this only matters during early initialization paths that touch the network before the
   * registration UI has run).
   */
  private fun resolveBaseUrl(): String {
    val configured = SignalStore.customServer.serverUrl
    return if (configured.isNotBlank()) configured else BuildConfig.SIGNAL_URL
  }

  val uncensoredConfiguration: SignalServiceConfiguration
    get() = buildConfiguration(resolveBaseUrl())

  fun getConfiguration(): SignalServiceConfiguration = uncensoredConfiguration

  fun getConfiguration(@Suppress("UNUSED_PARAMETER") e164: String?): SignalServiceConfiguration = uncensoredConfiguration

  fun isCensored(): Boolean = false

  fun isCensored(@Suppress("UNUSED_PARAMETER") number: String?): Boolean = false

  fun isCountryCodeCensoredByDefault(@Suppress("UNUSED_PARAMETER") countryCode: Int): Boolean = false

  private fun buildConfiguration(baseUrl: String): SignalServiceConfiguration {
    return SignalServiceConfiguration(
      signalServiceUrls = arrayOf(SignalServiceUrl(baseUrl, serviceTrustStore)),
      signalCdnUrlMap = mapOf(
        0 to arrayOf(SignalCdnUrl(baseUrl, serviceTrustStore)),
        2 to arrayOf(SignalCdnUrl(baseUrl, serviceTrustStore)),
        3 to arrayOf(SignalCdnUrl(baseUrl, serviceTrustStore))
      ),
      signalStorageUrls = arrayOf(SignalStorageUrl(baseUrl, serviceTrustStore)),
      signalCdsiUrls = arrayOf(SignalCdsiUrl(baseUrl, serviceTrustStore)),
      signalSvr2Urls = arrayOf(SignalSvr2Url(baseUrl, serviceTrustStore)),
      networkInterceptors = interceptors,
      dns = Optional.of(DNS),
      signalProxy = if (SignalStore.proxy.isProxyEnabled) Optional.ofNullable(SignalStore.proxy.proxy) else Optional.empty(),
      systemHttpProxy = Optional.ofNullable(getSystemHttpProxy(context)),
      zkGroupServerPublicParams = zkGroupServerPublicParams,
      genericServerPublicParams = genericServerPublicParams,
      backupServerPublicParams = backupServerPublicParams,
      censored = false
    )
  }
}
