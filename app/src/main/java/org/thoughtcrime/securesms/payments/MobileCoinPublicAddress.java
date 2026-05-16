package org.thoughtcrime.securesms.payments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * server-private fork: MobileCoin wallet removed. This is reduced to an opaque
 * address wrapper that carries the base58 string verbatim — there is no MobileCoin
 * SDK to validate against, and no UI path that meaningfully consumes the value
 * any more.
 */
public final class MobileCoinPublicAddress {

  private final String base58;

  private MobileCoinPublicAddress(@NonNull String base58) {
    this.base58 = base58;
  }

  public static @Nullable MobileCoinPublicAddress fromBase58NullableOrThrow(@Nullable String base58String) {
    return base58String != null ? new MobileCoinPublicAddress(base58String) : null;
  }

  public @NonNull String getPaymentAddressBase58() {
    return base58;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MobileCoinPublicAddress)) return false;

    return base58.equals(((MobileCoinPublicAddress) o).base58);
  }

  @Override
  public int hashCode() {
    return base58.hashCode();
  }

  @Override
  public @NonNull String toString() {
    return base58;
  }
}
