package com.finsecure.txn;

import java.math.BigDecimal;

public record Transaction(String id, String fromAccount, String toAccount, BigDecimal amount, String currency) {
}
