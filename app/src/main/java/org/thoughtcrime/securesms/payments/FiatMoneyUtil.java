package org.thoughtcrime.securesms.payments;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import org.signal.core.util.money.FiatMoney;

/**
 * Stub in server-private fork. In the original implementation this also did
 * currency exchange + much richer formatting via the deleted PaymentsValues +
 * AppDependencies.getPayments() services. The only surviving callers are the
 * paid-tier backup UI fragments, and those code paths are unreachable in this
 * fork (the server has no paid tier). Kept so the call sites still link.
 */
public final class FiatMoneyUtil {

  private FiatMoneyUtil() {}

  public static @NonNull String format(@NonNull Resources resources, @NonNull FiatMoney amount) {
    return amount.getAmount().toPlainString() + " " + amount.getCurrency().getCurrencyCode();
  }

  public static @NonNull String format(@NonNull Resources resources, @NonNull FiatMoney amount, @NonNull FormatOptions options) {
    return format(resources, amount);
  }

  public static @NonNull FormatOptions formatOptions() {
    return new FormatOptions();
  }

  public static final class FormatOptions {
    public @NonNull FormatOptions trimZerosAfterDecimal() { return this; }
    public @NonNull FormatOptions withSymbol(boolean withSymbol) { return this; }
    public @NonNull FormatOptions numberOfDecimals(int decimals) { return this; }
  }
}
