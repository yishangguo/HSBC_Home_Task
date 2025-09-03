package com.hsbc.transaction.performance;

import com.hsbc.transaction.dto.TransactionRequest;
import com.hsbc.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TransactionPerformanceTest {

    @Autowired
    private TransactionService transactionService;

    @Test
    void testConcurrentTransactionCreation() throws Exception {
        int numberOfThreads = 10;
        int transactionsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < transactionsPerThread; j++) {
                    try {
                        TransactionRequest request = createTransactionRequest(threadId, j);
                        transactionService.createTransaction(request);
                    } catch (Exception e) {
                        // Ignore duplicate reference errors for performance testing
                        if (!e.getMessage().contains("already exists")) {
                            fail("Unexpected error: " + e.getMessage());
                        }
                    }
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all threads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        int totalTransactions = numberOfThreads * transactionsPerThread;

        System.out.println("Performance Test Results:");
        System.out.println("Total transactions: " + totalTransactions);
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Average time per transaction: " + (double) totalTime / totalTransactions + " ms");
        System.out.println("Transactions per second: " + (totalTransactions * 1000.0 / totalTime));

        // Assert performance requirements
        assertTrue(totalTime < 30000, "Performance test should complete within 30 seconds");
        assertTrue((double) totalTime / totalTransactions < 100, "Average time per transaction should be less than 100ms");
    }

    @Test
    void testBulkTransactionRetrieval() throws Exception {
        // Create some test data first
        for (int i = 0; i < 100; i++) {
            try {
                TransactionRequest request = createTransactionRequest(999, i);
                transactionService.createTransaction(request);
            } catch (Exception e) {
                // Ignore duplicate reference errors
            }
        }

        // Test bulk retrieval performance
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            transactionService.getAllTransactions(
                org.springframework.data.domain.PageRequest.of(i, 10)
            );
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("Bulk Retrieval Performance:");
        System.out.println("Total time for 10 page retrievals: " + totalTime + " ms");
        System.out.println("Average time per page: " + totalTime / 10.0 + " ms");

        assertTrue(totalTime < 5000, "Bulk retrieval should complete within 5 seconds");
    }

    @Test
    void testSearchPerformance() throws Exception {
        // Test search performance with different keywords
        String[] keywords = {"deposit", "withdrawal", "payment", "transfer", "fee"};
        
        long startTime = System.currentTimeMillis();
        
        for (String keyword : keywords) {
            for (int i = 0; i < 5; i++) {
                transactionService.searchTransactions(
                    keyword, 
                    org.springframework.data.domain.PageRequest.of(i, 10)
                );
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        int totalSearches = keywords.length * 5;

        System.out.println("Search Performance:");
        System.out.println("Total searches: " + totalSearches);
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Average time per search: " + (double) totalTime / totalSearches + " ms");

        assertTrue(totalTime < 10000, "Search operations should complete within 10 seconds");
    }

    @Test
    void testCachePerformance() throws Exception {
        // Test cache performance by making repeated requests
        long startTime = System.currentTimeMillis();
        
        // First request (cache miss)
        transactionService.getTransactionTypes();
        
        // Subsequent requests (cache hits)
        for (int i = 0; i < 100; i++) {
            transactionService.getTransactionTypes();
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("Cache Performance:");
        System.out.println("Total requests: 101");
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Average time per request: " + (double) totalTime / 101 + " ms");

        assertTrue(totalTime < 1000, "Cached operations should be very fast");
    }

    private TransactionRequest createTransactionRequest(int threadId, int transactionId) {
        TransactionRequest request = new TransactionRequest();
        request.setReference(null);
        request.setAccountNumber("12345678");
        request.setAmount(new BigDecimal("100.00"));
        request.setType("DEPOSIT");
        request.setDescription("Performance test transaction " + threadId + "-" + transactionId);
        request.setTransactionDate(null);
        return request;
    }
}

