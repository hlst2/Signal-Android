package org.thoughtcrime.securesms.payments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Stub in server-private fork. Wallet (MobileCoin) entropy is no longer
 * generated or stored, but [StorageSyncHelper] still threads the (always-null
 * here) bytes through the AccountRecord protobuf, so the data shape stays.
 */
public final class Entropy {
  private final byte[] bytes;

  Entropy(@NonNull byte[] bytes) {
    this.bytes = bytes;
  }

  public static @Nullable Entropy fromBytes(@Nullable byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    return new Entropy(bytes);
  }

  public byte[] getBytes() {
    return bytes;
  }
}
