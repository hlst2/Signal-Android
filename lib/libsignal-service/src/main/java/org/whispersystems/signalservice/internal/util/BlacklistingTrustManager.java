/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.internal.util;

import org.whispersystems.signalservice.api.push.TrustStore;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import kotlin.Pair;

/**
 * Trust manager that defers to a system X509 trust manager, and
 * additionally rejects certificates if they have a blacklisted
 * serial.
 *
 * @author Moxie Marlinspike
 */
public class BlacklistingTrustManager implements X509TrustManager {

  private static final List<Pair<String, BigInteger>> BLACKLIST = new LinkedList<Pair<String, BigInteger>>() {{
    add(new Pair<>("Open Whisper Systems", new BigInteger("4098")));
  }};

  public static TrustManager[] createFor(TrustManager[] trustManagers) {
    for (TrustManager trustManager : trustManagers) {
      if (trustManager instanceof X509TrustManager) {
        TrustManager[] results = new BlacklistingTrustManager[1];
        results[0] = new BlacklistingTrustManager((X509TrustManager)trustManager);

        return results;
      }
    }

    throw new AssertionError("No X509 Trust Managers!");
  }

  public static TrustManager[] createFor(TrustStore trustStore) {
    try {
      InputStream keyStoreInputStream = trustStore.getKeyStoreInputStream();
      KeyStore    keyStore            = KeyStore.getInstance("BKS");

      keyStore.load(keyStoreInputStream, trustStore.getKeyStorePassword().toCharArray());

      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
      trustManagerFactory.init(keyStore);

      X509TrustManager pinned = findX509(trustManagerFactory.getTrustManagers());

      // Private/self-hosted deployments present a certificate that does not chain to the bundled (pinned) CA in
      // whisper.store. Allow the platform's default CA store as a fallback so a publicly-trusted (e.g. Let's
      // Encrypt) or otherwise system-trusted server certificate validates. The bundled store is still tried
      // first, and the serial blacklist is always enforced.
      X509TrustManager system = systemTrustManager();

      return new TrustManager[] { new BlacklistingTrustManager(pinned, system) };
    } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  private static X509TrustManager findX509(TrustManager[] trustManagers) {
    for (TrustManager trustManager : trustManagers) {
      if (trustManager instanceof X509TrustManager) {
        return (X509TrustManager) trustManager;
      }
    }
    throw new AssertionError("No X509 Trust Managers!");
  }

  private static X509TrustManager systemTrustManager() {
    try {
      TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      factory.init((KeyStore) null);
      return findX509(factory.getTrustManagers());
    } catch (NoSuchAlgorithmException | KeyStoreException e) {
      return null;
    }
  }

  private final X509TrustManager trustManager;
  private final X509TrustManager systemFallback;

  public BlacklistingTrustManager(X509TrustManager trustManager) {
    this(trustManager, null);
  }

  public BlacklistingTrustManager(X509TrustManager trustManager, X509TrustManager systemFallback) {
    this.trustManager   = trustManager;
    this.systemFallback = systemFallback;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException
  {
    trustManager.checkClientTrusted(chain, authType);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException
  {
    try {
      trustManager.checkServerTrusted(chain, authType);
    } catch (CertificateException e) {
      if (systemFallback != null) {
        systemFallback.checkServerTrusted(chain, authType);
      } else {
        throw e;
      }
    }

    for (X509Certificate certificate : chain) {
      for (Pair<String, BigInteger> blacklistedSerial : BLACKLIST) {
        if (certificate.getIssuerDN().getName().equals(blacklistedSerial.getFirst()) &&
            certificate.getSerialNumber().equals(blacklistedSerial.getSecond()))
        {
          throw new CertificateException("Blacklisted Serial: " + certificate.getSerialNumber());
        }
      }
    }

  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    if (systemFallback == null) {
      return trustManager.getAcceptedIssuers();
    }

    X509Certificate[] pinnedIssuers = trustManager.getAcceptedIssuers();
    X509Certificate[] systemIssuers = systemFallback.getAcceptedIssuers();
    X509Certificate[] combined      = new X509Certificate[pinnedIssuers.length + systemIssuers.length];

    System.arraycopy(pinnedIssuers, 0, combined, 0, pinnedIssuers.length);
    System.arraycopy(systemIssuers, 0, combined, pinnedIssuers.length, systemIssuers.length);

    return combined;
  }
}
