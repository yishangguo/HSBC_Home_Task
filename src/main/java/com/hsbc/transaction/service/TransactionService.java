package com.hsbc.transaction.service;
import com.hsbc.transaction.dto.PageResponse;
import com.hsbc.transaction.dto.TransactionRequest;
import com.hsbc.transaction.dto.UpdateTransactionRequest;
import com.hsbc.transaction.dto.TransactionResponse;
import com.hsbc.transaction.model.TransactionType;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    TransactionResponse createTransaction(TransactionRequest request);
    
    TransactionResponse getTransactionById(Long id);
    
    TransactionResponse getTransactionByReference(String reference);
    
    TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request);
    
    void deleteTransaction(Long id);
    
    PageResponse<TransactionResponse> getAllTransactions(Pageable pageable);
    
    PageResponse<TransactionResponse> getTransactionsByAccount(String accountNumber, Pageable pageable);
    
    PageResponse<TransactionResponse> getTransactionsByType(TransactionType type, Pageable pageable);
    
    PageResponse<TransactionResponse> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    PageResponse<TransactionResponse> getTransactionsByAmountRange(BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);
    
    PageResponse<TransactionResponse> searchTransactions(String keyword, Pageable pageable);
    
    PageResponse<TransactionResponse> getTransactionsByCriteria(String accountNumber, TransactionType type, 
                                                             LocalDateTime startDate, LocalDateTime endDate,
                                                             BigDecimal minAmount, BigDecimal maxAmount, 
                                                             Pageable pageable);
    
    List<TransactionResponse> getRecentTransactions();
    
    long getTransactionCountByAccount(String accountNumber);
    
    BigDecimal getAccountBalance(String accountNumber);
    
    BigDecimal getAccountBalanceByType(String accountNumber, TransactionType type);

    /**
     * Get supported transaction types. Cached for fast repeated access.
     */
    List<String> getTransactionTypes();
}
