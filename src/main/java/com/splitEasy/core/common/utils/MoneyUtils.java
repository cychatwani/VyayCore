package com.splitEasy.core.common.utils;

import com.splitEasy.core.entity.reference.Currency;
import com.splitEasy.core.exception.business.InvalidExpenseException;

import java.math.BigDecimal;

public final class MoneyUtils {

    private MoneyUtils() {
    }

    // Major -> minor, exact. Rejects amounts more precise than the currency allows.
    public static long toMinor(BigDecimal major, Currency currency) {
        try {
            return major.movePointRight(currency.getDecimalPlaces()).longValueExact();
        } catch (ArithmeticException e) {
            throw new InvalidExpenseException(
                    "Amount " + major.toPlainString() + " has more precision than "
                            + currency.getCode() + " allows (" + currency.getDecimalPlaces()
                            + " decimal places)");
        }
    }

    // Minor -> major, scaled to the currency (e.g. 12342 + INR -> 123.42).
    public static BigDecimal toMajor(long minor, Currency currency) {
        return BigDecimal.valueOf(minor).movePointLeft(currency.getDecimalPlaces());
    }
}