package com.github.benimarushiimon.cardpayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benimarushiimon.cardpayment.DTO.Amount;
import com.github.benimarushiimon.cardpayment.DTO.ConfirmRequest;
import com.github.benimarushiimon.cardpayment.DTO.TransferPaymentRequest;
import com.github.benimarushiimon.cardpayment.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest // 1. Запускает ваш Spring Boot Application как контекст
@ExtendWith(SpringExtension.class)
public class TransferControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TransferService transferServiceMock;

    @InjectMocks
    private TransferController transferController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(transferController).build();
    }

    @Test
    void testCreateTransfer_Success() throws Exception {
        Amount validAmount = new Amount(new BigDecimal("1000.00"), "RUB");

        TransferPaymentRequest request = new TransferPaymentRequest();
        request.setCardFromNumber("1111222233334444");
        request.setCardToNumber("5555666677778888");
        request.setAmount(validAmount);
        request.setCardFromValidTill("12/29");
        request.setCardFromCVV("123");

        String mockOperationId = "generated-op-id-777";
        when(transferServiceMock.initiateTransfer(any(TransferPaymentRequest.class)))
                .thenReturn(mockOperationId);

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.operationId").value(mockOperationId));

        // Проверяем, что метод сервиса был вызван 1 раз с любым валидным запросом.
        // Это более надежно, чем проверка конкретных значений внутри объекта.
        verify(transferServiceMock, times(1)).initiateTransfer(any(TransferPaymentRequest.class));
    }

    @Test
    void testConfirmTransfer_Success() throws Exception {
        ConfirmRequest request = new ConfirmRequest();
        request.setOperationId("test-op-id-123");
        request.setCode("1234");

        doNothing().when(transferServiceMock).confirmTransfer(
                eq(request.getOperationId()), eq(request.getCode()));

        mockMvc.perform(post("/confirmOperation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(transferServiceMock, times(1)).confirmTransfer(
                request.getOperationId(), request.getCode());
    }
}