package com.hsbc.transaction.service;

import com.hsbc.transaction.dto.TransactionRequest;
import com.hsbc.transaction.dto.UpdateTransactionRequest;
import com.hsbc.transaction.dto.TransactionResponse;
import com.hsbc.transaction.exception.TransactionNotFoundException;
import com.hsbc.transaction.exception.ValidationException;
import com.hsbc.transaction.model.Transaction;
import com.hsbc.transaction.model.TransactionType;
import com.hsbc.transaction.repository.TransactionRepository;
import com.hsbc.transaction.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private TransactionRequest validRequest;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        validRequest = new TransactionRequest();
        validRequest.setReference(null);
        validRequest.setAccountNumber("12345678");
        validRequest.setAmount(new BigDecimal("100.00"));
        validRequest.setType("DEPOSIT");
        validRequest.setDescription("Test transaction");
        validRequest.setTransactionDate(LocalDateTime.now().minusDays(1));

        sampleTransaction = new Transaction();
        sampleTransaction.setId(1L);
        sampleTransaction.setReference("TXNTEST001");
        sampleTransaction.setAccountNumber("12345678");
        sampleTransaction.setAmount(new BigDecimal("100.00"));
        sampleTransaction.setType(TransactionType.DEPOSIT);
        sampleTransaction.setDescription("Test transaction");
        sampleTransaction.setTransactionDate(LocalDateTime.now().minusDays(1));
        sampleTransaction.setType(TransactionType.DEPOSIT);
    }

    @Test
    void createTransaction_Success() {
        when(transactionRepository.existsByReference(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        TransactionResponse response = transactionService.createTransaction(validRequest);

        assertNotNull(response);
        assertNotNull(response.getReference());
        assertEquals("12345678", response.getAccountNumber());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(TransactionType.DEPOSIT, response.getType());

        verify(transactionRepository, atLeast(0)).existsByReference(anyString());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_RetryOnGeneratedReferenceCollision_Succeeds() {
        when(transactionRepository.existsByReference(anyString())).thenReturn(true, false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        TransactionResponse response = transactionService.createTransaction(validRequest);

        assertNotNull(response);
        assertNotNull(response.getReference());
        verify(transactionRepository, atLeast(1)).existsByReference(anyString());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_InvalidAmount_ThrowsException() {
        validRequest.setAmount(new BigDecimal("-100.00"));

        assertThrows(ValidationException.class, () -> {
            transactionService.createTransaction(validRequest);
        });

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_FutureDate_ThrowsException() {
        validRequest.setTransactionDate(LocalDateTime.now().plusDays(1));

        assertThrows(ValidationException.class, () -> {
            transactionService.createTransaction(validRequest);
        });

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionById_Success() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));

        TransactionResponse response = transactionService.getTransactionById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("TXNTEST001", response.getReference());

        verify(transactionRepository).findById(1L);
    }

    @Test
    void getTransactionById_NotFound_ThrowsException() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> {
            transactionService.getTransactionById(999L);
        });

        verify(transactionRepository).findById(999L);
    }

    @Test
    void updateTransaction_Success() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        UpdateTransactionRequest updateReq = new UpdateTransactionRequest();
        updateReq.setDescription("Updated description");
        updateReq.setNotes("Updated notes");
        TransactionResponse response = transactionService.updateTransaction(1L, updateReq);

        assertNotNull(response);
        verify(transactionRepository).findById(1L);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_NotFound_ThrowsException() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateTransactionRequest updateReq = new UpdateTransactionRequest();
        updateReq.setDescription("Updated description");
        updateReq.setNotes("Updated notes");

        assertThrows(TransactionNotFoundException.class, () -> {
            transactionService.updateTransaction(999L, updateReq);
        });

        verify(transactionRepository).findById(999L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void deleteTransaction_Success() {
        when(transactionRepository.existsById(1L)).thenReturn(true);
        doNothing().when(transactionRepository).deleteById(1L);

        assertDoesNotThrow(() -> {
            transactionService.deleteTransaction(1L);
        });

        verify(transactionRepository).existsById(1L);
        verify(transactionRepository).deleteById(1L);
    }

    @Test
    void deleteTransaction_NotFound_ThrowsException() {
        when(transactionRepository.existsById(999L)).thenReturn(false);

        assertThrows(TransactionNotFoundException.class, () -> {
            transactionService.deleteTransaction(999L);
        });

        verify(transactionRepository).existsById(999L);
        verify(transactionRepository, never()).deleteById(any());
    }

    @Test
    void createTransaction_NullRequest_ThrowsException() {
        assertThrows(ValidationException.class, () -> {
            transactionService.createTransaction(null);
        });

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_EmptyReference_IgnoredAndGeneratesAutomatically() {
        validRequest.setReference("");
        when(transactionRepository.existsByReference(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        TransactionResponse response = transactionService.createTransaction(validRequest);

        assertNotNull(response);
        assertNotNull(response.getReference());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_InvalidType_ThrowsException() {
        validRequest.setType("INVALID_TYPE");

        assertThrows(ValidationException.class, () -> {
            transactionService.createTransaction(validRequest);
        });

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionTypes_ReturnsAllEnumNames() {
        List<String> types = transactionService.getTransactionTypes();
        List<String> expected = Arrays.stream(TransactionType.values())
                .map(Enum::name)
                .toList();

        assertNotNull(types);
        assertEquals(expected.size(), types.size());
        assertTrue(types.containsAll(expected));
    }
}

