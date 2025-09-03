package com.hsbc.transaction.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsbc.transaction.dto.TransactionRequest;
import org.junit.jupiter.api.Test;
import com.hsbc.transaction.dto.TransactionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(webApplicationContext);
    }

    @Test
    void createTransaction_Success() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        TransactionRequest request = createValidTransactionRequest();
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").isString())
                .andExpect(jsonPath("$.accountNumber").value("12345678"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test
    void createTransaction_InvalidRequest_ReturnsBadRequest() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        TransactionRequest request = createInvalidTransactionRequest();
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTransactions_ReturnsOk() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/v1/transactions")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void getTransactionById_NotFound_ReturnsNotFound() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/v1/transactions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactionTypes_ReturnsOk() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/v1/transactions/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void searchTransactions_ReturnsOk() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/v1/transactions/search")
                .param("keyword", "deposit")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void getTransactionsByAccount_ReturnsOk() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/v1/transactions/account/12345678")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void getTransactionsByType_ReturnsOk() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/v1/transactions/type/deposit")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void getRecentTransactions_ReturnsOk() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/v1/transactions/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAccountBalance_ReturnsOk() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/v1/transactions/account/12345678/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void updateTransaction_OnlyDescriptionAndNotesAreUpdatable() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // create
        TransactionRequest createRequest = createValidTransactionRequest();
        String createJson = objectMapper.writeValueAsString(createRequest);
        String responseBody = mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TransactionResponse created = objectMapper.readValue(responseBody, TransactionResponse.class);

        // attempt to update all fields but backend should only honor description and notes
        TransactionRequest updateRequest = new TransactionRequest();
        updateRequest.setReference("SHOULD_BE_IGNORED");
        updateRequest.setAccountNumber("99999999");
        updateRequest.setAmount(new java.math.BigDecimal("999.99"));
        updateRequest.setType("WITHDRAWAL");
        updateRequest.setDescription("Updated description");
        updateRequest.setTransactionDate(java.time.LocalDateTime.now().minusDays(5));
        updateRequest.setNotes("Updated notes");
        String updateJson = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(put("/api/v1/transactions/" + created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.notes").value("Updated notes"))
                .andExpect(jsonPath("$.reference").value(created.getReference()))
                .andExpect(jsonPath("$.accountNumber").value("12345678"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    private TransactionRequest createValidTransactionRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setReference(null); // reference auto-generated by backend
        request.setAccountNumber("12345678");
        request.setAmount(new BigDecimal("100.00"));
        request.setType("DEPOSIT");
        request.setDescription("Test transaction");
        request.setTransactionDate(null); // transaction date default to now by backend
        return request;
    }

    private TransactionRequest createInvalidTransactionRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("12345678");
        request.setAmount(new BigDecimal("-100.00")); // Invalid: negative amount
        request.setType("INVALID_TYPE"); // Invalid: invalid type
        request.setDescription("Test transaction");
        request.setTransactionDate(LocalDateTime.now().plusDays(1)); // Invalid: future date
        return request;
    }
}
