package com.hsbc.transaction.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionRequest {

    private String reference;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{8,12}$", message = "Account number must be 8-12 digits")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Amount cannot exceed 999,999,999.99")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private String type;

    @NotBlank(message = "Description is required")
    @Size(min = 5, max = 200, message = "Description must be between 5 and 200 characters")
    private String description;

    private LocalDateTime transactionDate;

    private String notes;

    // Constructors
    public TransactionRequest() {}

    public TransactionRequest(String reference, String accountNumber, BigDecimal amount, 
                           String type, String description, LocalDateTime transactionDate) {
        this.reference = reference;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.transactionDate = transactionDate;
    }

    // Getters and Setters
    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
