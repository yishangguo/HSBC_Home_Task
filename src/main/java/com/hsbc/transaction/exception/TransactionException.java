package com.hsbc.transaction.exception;

public class TransactionException extends RuntimeException {

    private final String errorCode;

    public TransactionException(String message) {
        super(message);
        this.errorCode = "TRANSACTION_ERROR";
    }

    public TransactionException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TRANSACTION_ERROR";
    }

    public TransactionException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
