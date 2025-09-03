package com.hsbc.transaction.exception;

public class DuplicateTransactionException extends TransactionException {

    public DuplicateTransactionException(String reference) {
        super("Transaction with reference " + reference + " already exists", "DUPLICATE_TRANSACTION");
    }
}

