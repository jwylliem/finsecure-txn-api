package com.finsecure.txn;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TxnValidator {

    private final BigDecimal maxAmount;
    private final List<String> currencies;

    public TxnValidator(@Value("${txn.max-amount}") BigDecimal maxAmount,
                        @Value("${txn.currencies}") List<String> currencies) {
        this.maxAmount = maxAmount;
        this.currencies = currencies;
    }

    // returns list of error, empty list mean valid
    public List<String> validate(Transaction t) {
        List<String> errors = new ArrayList<>();
        if (isBlank(t.id()) || isBlank(t.fromAccount()) || isBlank(t.toAccount())) {
            errors.add("id, fromAccount and toAccount are required");
        }
        if (t.amount() == null || t.amount().signum() <= 0) {
            errors.add("amount must be positive");
        } else if (t.amount().compareTo(maxAmount) > 0) {
            errors.add("amount is over the limit " + maxAmount);
        }
        if (t.currency() == null || !currencies.contains(t.currency())) {
            errors.add("currency not supported");
        }
        return errors;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
