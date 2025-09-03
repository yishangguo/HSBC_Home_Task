package com.hsbc.transaction.repository;

import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReference(String reference);
    
    boolean existsByReference(String reference);
    
    Page<Transaction> findByAccountNumber(String accountNumber, Pageable pageable);
    
    Page<Transaction> findByType(TransactionType type, Pageable pageable);
    
    Page<Transaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    Page<Transaction> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE " +
           "(:accountNumber IS NULL OR t.accountNumber = :accountNumber) AND " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:startDate IS NULL OR t.transactionDate >= :startDate) AND " +
           "(:endDate IS NULL OR t.transactionDate <= :endDate) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount)")
    Page<Transaction> findByCriteria(
            @Param("accountNumber") String accountNumber,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.accountNumber = :accountNumber")
    long countByAccountNumber(@Param("accountNumber") String accountNumber);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.accountNumber = :accountNumber AND t.type = :type")
    BigDecimal sumByAccountNumberAndType(@Param("accountNumber") String accountNumber, @Param("type") TransactionType type);
    
    List<Transaction> findTop10ByOrderByTransactionDateDesc();
    
    @Query("SELECT t FROM Transaction t WHERE t.reference LIKE %:keyword% OR t.description LIKE %:keyword%")
    Page<Transaction> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
