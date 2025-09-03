package com.hsbc.transaction.config;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionType;
import com.hsbc.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final TransactionRepository transactionRepository;

    @Autowired
    public DataInitializer(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only initialize if no transactions exist
        if (transactionRepository.count() == 0) {
            initializeSampleData();
        }
    }

    private void initializeSampleData() {
        LocalDateTime now = LocalDateTime.now();
        
        // Sample transactions for account 12345678
        createTransaction("TXN202509010000000000001", "12345678", new BigDecimal("1000.00"), 
                        TransactionType.DEPOSIT, "Initial deposit", now.minusDays(30));
        
        createTransaction("TXN202509010000000000002", "12345678", new BigDecimal("500.00"), 
                        TransactionType.WITHDRAWAL, "ATM withdrawal", now.minusDays(25));
        
        createTransaction("TXN202509010000000000003", "12345678", new BigDecimal("250.00"), 
                        TransactionType.PAYMENT, "Utility bill payment", now.minusDays(20));
        
        createTransaction("TXN202509010000000000004", "12345678", new BigDecimal("750.00"), 
                        TransactionType.DEPOSIT, "Salary deposit", now.minusDays(15));
        
        // Sample transactions for account 87654321
        createTransaction("TXN202509010000000000005", "87654321", new BigDecimal("2000.00"), 
                        TransactionType.DEPOSIT, "Business account funding", now.minusDays(28));
        
        createTransaction("TXN202509010000000000006", "87654321", new BigDecimal("150.00"), 
                        TransactionType.FEE, "Monthly maintenance fee", now.minusDays(22));
        
        createTransaction("TXN202509010000000000007", "87654321", new BigDecimal("300.00"), 
                        TransactionType.TRANSFER, "Transfer to savings", now.minusDays(18));
        
        // Sample transactions for account 11111111
        createTransaction("TXN202509010000000000008", "11111111", new BigDecimal("100.00"), 
                        TransactionType.DEPOSIT, "Gift deposit", now.minusDays(10));
        
        createTransaction("TXN202509010000000000009", "11111111", new BigDecimal("50.00"), 
                        TransactionType.WITHDRAWAL, "Shopping", now.minusDays(5));
        
        createTransaction("TXN202509010000000000010", "11111111", new BigDecimal("25.00"), 
                        TransactionType.INTEREST, "Interest earned", now.minusDays(1));
    }

    private void createTransaction(String reference, String accountNumber, BigDecimal amount, 
                                 TransactionType type, String description, LocalDateTime transactionDate) {
        Transaction transaction = new Transaction(reference, accountNumber, amount, type, description, transactionDate);
        transactionRepository.save(transaction);
    }
}
