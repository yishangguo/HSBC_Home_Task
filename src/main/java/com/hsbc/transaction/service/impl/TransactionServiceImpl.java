package com.hsbc.transaction.service.impl;

import com.hsbc.transaction.dto.PageResponse;
import com.hsbc.transaction.dto.TransactionRequest;
import com.hsbc.transaction.dto.TransactionResponse;
import com.hsbc.transaction.dto.UpdateTransactionRequest;
import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.exception.ValidationException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionType;
import com.hsbc.transaction.repository.TransactionRepository;
import com.hsbc.transaction.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @CacheEvict(value = {"transactions", "recentTransactions", "accountBalances"}, allEntries = true)
    public TransactionResponse createTransaction(TransactionRequest request) {
        validateTransactionRequest(request);

        TransactionType type = parseTransactionType(request.getType());

        // Auto-generate transaction reference (number)
        String generatedReference = generateTransactionNumber();
        while (transactionRepository.existsByReference(generatedReference)) {
            generatedReference = generateTransactionNumber();
        }

        // Default transaction date to now if not provided
        LocalDateTime transactionDate = request.getTransactionDate() != null
                ? request.getTransactionDate()
                : LocalDateTime.now();

        Transaction transaction = new Transaction(
                generatedReference,
                request.getAccountNumber(),
                request.getAmount(),
                type,
                request.getDescription(),
                transactionDate
        );
        
        transaction.setNotes(request.getNotes());
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        return new TransactionResponse(savedTransaction);
    }

    @Override
    @Cacheable(value = "transactions", key = "#id")
    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        return new TransactionResponse(transaction);
    }

    @Override
    @Cacheable(value = "transactions", key = "#reference")
    public TransactionResponse getTransactionByReference(String reference) {
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new TransactionNotFoundException(reference));
        return new TransactionResponse(transaction);
    }

    @Override
    @CacheEvict(value = {"transactions", "recentTransactions", "accountBalances"}, allEntries = true)
    public TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request) {
        if (request == null) {
            throw new ValidationException("Transaction request cannot be null");
        }
        if (!StringUtils.hasText(request.getDescription())) {
            throw new ValidationException("Description is required");
        }
        
        Transaction existingTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        
        // Immutable fields: reference, accountNumber, amount, type, transactionDate
        existingTransaction.setDescription(request.getDescription());
        existingTransaction.setNotes(request.getNotes());
        
        Transaction updatedTransaction = transactionRepository.save(existingTransaction);
        return new TransactionResponse(updatedTransaction);
    }

    @Override
    @CacheEvict(value = {"transactions", "recentTransactions", "accountBalances"}, allEntries = true)
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new TransactionNotFoundException(id);
        }
        transactionRepository.deleteById(id);
    }

    @Override
    @Cacheable(value = "transactions", key = "'all_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<TransactionResponse> getAllTransactions(Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findAll(pageable);
        return createPageResponse(transactionPage);
    }

    @Override
    @Cacheable(value = "transactions", key = "'account_' + #accountNumber + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<TransactionResponse> getTransactionsByAccount(String accountNumber, Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findByAccountNumber(accountNumber, pageable);
        return createPageResponse(transactionPage);
    }

    @Override
    @Cacheable(value = "transactions", key = "'type_' + #type + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<TransactionResponse> getTransactionsByType(TransactionType type, Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findByType(type, pageable);
        return createPageResponse(transactionPage);
    }

    @Override
    @Cacheable(value = "transactions", key = "'dateRange_' + #startDate + '_' + #endDate + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<TransactionResponse> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findByTransactionDateBetween(startDate, endDate, pageable);
        return createPageResponse(transactionPage);
    }

    @Override
    @Cacheable(value = "transactions", key = "'amountRange_' + #minAmount + '_' + #maxAmount + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<TransactionResponse> getTransactionsByAmountRange(BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findByAmountBetween(minAmount, maxAmount, pageable);
        return createPageResponse(transactionPage);
    }

    @Override
    @Cacheable(value = "transactions", key = "'search_' + #keyword + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<TransactionResponse> searchTransactions(String keyword, Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.searchByKeyword(keyword, pageable);
        return createPageResponse(transactionPage);
    }

    @Override
    @Cacheable(value = "transactions", key = "'criteria_' + #accountNumber + '_' + #type + '_' + #startDate + '_' + #endDate + '_' + #minAmount + '_' + #maxAmount + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<TransactionResponse> getTransactionsByCriteria(String accountNumber, TransactionType type,
                                                                   LocalDateTime startDate, LocalDateTime endDate,
                                                                   BigDecimal minAmount, BigDecimal maxAmount,
                                                                   Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findByCriteria(
                accountNumber, type, startDate, endDate, minAmount, maxAmount, pageable);
        return createPageResponse(transactionPage);
    }

    @Override
    @Cacheable(value = "recentTransactions")
    public List<TransactionResponse> getRecentTransactions() {
        List<Transaction> recentTransactions = transactionRepository.findTop10ByOrderByTransactionDateDesc();
        return recentTransactions.stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "accountBalances", key = "#accountNumber + '_count'")
    public long getTransactionCountByAccount(String accountNumber) {
        return transactionRepository.countByAccountNumber(accountNumber);
    }
    public BigDecimal getAccountBalance(String accountNumber) {
        BigDecimal deposits = transactionRepository.sumByAccountNumberAndType(accountNumber, TransactionType.DEPOSIT);
        BigDecimal withdrawals = transactionRepository.sumByAccountNumberAndType(accountNumber, TransactionType.WITHDRAWAL);
        
        deposits = deposits != null ? deposits : BigDecimal.ZERO;
        withdrawals = withdrawals != null ? withdrawals : BigDecimal.ZERO;
        
        return deposits.subtract(withdrawals);
    }

    @Override
    @Cacheable(value = "accountBalances", key = "#accountNumber + '_' + #type + '_balance'")
    public BigDecimal getAccountBalanceByType(String accountNumber, TransactionType type) {
        BigDecimal balance = transactionRepository.sumByAccountNumberAndType(accountNumber, type);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Override
    @Cacheable(value = "metadata", key = "'transactionTypes'")
    public List<String> getTransactionTypes() {
        return Arrays.stream(TransactionType.values())
                .map(TransactionType::name)
                .collect(Collectors.toList());
    }

    private void validateTransactionRequest(TransactionRequest request) {
        if (request == null) {
            throw new ValidationException("Transaction request cannot be null");
        }
        
        if (!StringUtils.hasText(request.getAccountNumber())) {
            throw new ValidationException("Account number is required");
        }
        
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero");
        }
        
        if (!StringUtils.hasText(request.getType())) {
            throw new ValidationException("Transaction type is required");
        }
        
        if (!StringUtils.hasText(request.getDescription())) {
            throw new ValidationException("Description is required");
        }
        
        if (request.getTransactionDate() != null && request.getTransactionDate().isAfter(LocalDateTime.now())) {
            throw new ValidationException("Transaction date cannot be in the future");
        }
    }

    private TransactionType parseTransactionType(String typeString) {
        try {
            return TransactionType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid transaction type: " + typeString);
        }
    }

    private String generateTransactionNumber() {
        // Format: TXNyyyyMMddHHmmssSSS + 4-digit random
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS%1$tL", now);
        int random = (int)(Math.random() * 10000);
        return "TXN" + timestamp + String.format("%04d", random);
    }

    private PageResponse<TransactionResponse> createPageResponse(Page<Transaction> transactionPage) {
        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                content,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.isFirst(),
                transactionPage.isLast()
        );
    }
}
