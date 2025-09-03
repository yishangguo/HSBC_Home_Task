package com.hsbc.transaction.model;

public enum TransactionType {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER("Transfer"),
    PAYMENT("Payment"),
    REFUND("Refund"),
    FEE("Fee"),
    INTEREST("Interest"),
    EXCHANGE("Exchange");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
