package com.hsbc.transaction.exception;

public class TransactionNotFoundException extends TransactionException {

    public TransactionNotFoundException(Long id) {
        super("Transaction with id " + id + " not found", "TRANSACTION_NOT_FOUND");
    }

    public TransactionNotFoundException(String reference) {
        super("Transaction with reference " + reference + " not found", "TRANSACTION_NOT_FOUND");
    }
}
