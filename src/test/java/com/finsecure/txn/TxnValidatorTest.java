package com.finsecure.txn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class TxnValidatorTest {

    private final TxnValidator validator = new TxnValidator(new BigDecimal("5000"), List.of("SGD", "USD"));

    @Test
    void validTransaction() {
        assertTrue(validator.validate(new Transaction("T1", "111", "222", new BigDecimal("100"), "SGD")).isEmpty());
    }

    @Test
    void amountOverLimit() {
        assertEquals(1, validator.validate(new Transaction("T1", "111", "222", new BigDecimal("5000.01"), "SGD")).size());
    }

    @Test
    void negativeAmount() {
        assertEquals(List.of("amount must be positive"),
                validator.validate(new Transaction("T1", "111", "222", new BigDecimal("-1"), "SGD")));
    }

    @Test
    void wrongCurrency() {
        assertEquals(List.of("currency not supported"),
                validator.validate(new Transaction("T1", "111", "222", BigDecimal.TEN, "JPY")));
    }

    @Test
    void missingFields() {
        assertEquals(3, validator.validate(new Transaction(null, null, null, null, null)).size());
    }
}
