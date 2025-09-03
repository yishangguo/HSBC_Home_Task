package com.hsbc.transaction.controller;

import com.hsbc.transaction.dto.PageResponse;
import com.hsbc.transaction.dto.TransactionRequest;
import com.hsbc.transaction.dto.TransactionResponse;
import com.hsbc.transaction.dto.UpdateTransactionRequest;
import com.hsbc.transaction.model.TransactionType;
import com.hsbc.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long id) {
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<TransactionResponse> getTransactionByReference(@PathVariable String reference) {
        TransactionResponse response = transactionService.getTransactionByReference(reference);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable Long id, 
                                                             @Valid @RequestBody UpdateTransactionRequest request) {
        TransactionResponse response = transactionService.updateTransaction(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<TransactionResponse> response = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<PageResponse<TransactionResponse>> getTransactionsByAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        PageResponse<TransactionResponse> response = transactionService.getTransactionsByAccount(accountNumber, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<PageResponse<TransactionResponse>> getTransactionsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        TransactionType transactionType = TransactionType.valueOf(type.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        PageResponse<TransactionResponse> response = transactionService.getTransactionsByType(transactionType, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date-range")
    public ResponseEntity<PageResponse<TransactionResponse>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        PageResponse<TransactionResponse> response = transactionService.getTransactionsByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/amount-range")
    public ResponseEntity<PageResponse<TransactionResponse>> getTransactionsByAmountRange(
            @RequestParam BigDecimal minAmount,
            @RequestParam BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        PageResponse<TransactionResponse> response = transactionService.getTransactionsByAmountRange(minAmount, maxAmount, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<TransactionResponse>> searchTransactions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        PageResponse<TransactionResponse> response = transactionService.searchTransactions(keyword, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/criteria")
    public ResponseEntity<PageResponse<TransactionResponse>> getTransactionsByCriteria(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        TransactionType transactionType = null;
        if (type != null) {
            transactionType = TransactionType.valueOf(type.toUpperCase());
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        PageResponse<TransactionResponse> response = transactionService.getTransactionsByCriteria(
                accountNumber, transactionType, startDate, endDate, minAmount, maxAmount, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<TransactionResponse>> getRecentTransactions() {
        List<TransactionResponse> response = transactionService.getRecentTransactions();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}/count")
    public ResponseEntity<Long> getTransactionCountByAccount(@PathVariable String accountNumber) {
        long count = transactionService.getTransactionCountByAccount(accountNumber);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/account/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable String accountNumber) {
        BigDecimal balance = transactionService.getAccountBalance(accountNumber);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/account/{accountNumber}/balance/{type}")
    public ResponseEntity<BigDecimal> getAccountBalanceByType(
            @PathVariable String accountNumber,
            @PathVariable String type) {
        
        TransactionType transactionType = TransactionType.valueOf(type.toUpperCase());
        BigDecimal balance = transactionService.getAccountBalanceByType(accountNumber, transactionType);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/types")
    public ResponseEntity<TransactionType[]> getTransactionTypes() {
        return ResponseEntity.ok(TransactionType.values());
    }
}
