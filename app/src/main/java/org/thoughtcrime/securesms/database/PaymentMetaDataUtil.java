package org.thoughtcrime.securesms.database;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.payments.proto.PaymentMetaData;

import java.io.IOException;

/**
 * server-private fork: MobileCoin wallet removed. Only the proto decode helper remains,
 * since {@link PaymentTable#readPayment} still needs to read stored META_DATA blobs.
 */
public final class PaymentMetaDataUtil {

  public static PaymentMetaData parseOrThrow(byte[] requireBlob) {
    try {
      return PaymentMetaData.ADAPTER.decode(requireBlob);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
