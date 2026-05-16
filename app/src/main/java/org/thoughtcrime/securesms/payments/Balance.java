package org.thoughtcrime.securesms.payments;

import androidx.annotation.NonNull;

import org.whispersystems.signalservice.api.payments.Money;

import java.math.BigDecimal;

/**
 * Stub in server-private fork. The MobileCoin wallet is gone, so the only
 * thing this represents is "zero MOB". Kept so [DeleteAccountViewModel]
 * can still receive a Balance from a stub LiveData without recompiling
 * its own protocol.
 */
public final class Balance {
  public static final Balance ZERO = new Balance();

  private Balance() {}

  public @NonNull Money getFullAmount() {
    return Money.mobileCoin(BigDecimal.ZERO);
  }
}
