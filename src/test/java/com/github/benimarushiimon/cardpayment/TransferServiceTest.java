package com.github.benimarushiimon.cardpayment;

import com.github.benimarushiimon.cardpayment.DTO.Amount;
import com.github.benimarushiimon.cardpayment.DTO.TransferPaymentRequest;
import com.github.benimarushiimon.cardpayment.Entity.Card;
import com.github.benimarushiimon.cardpayment.repository.CardRepository;
import com.github.benimarushiimon.cardpayment.service.LoggingService;
import com.github.benimarushiimon.cardpayment.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private TransferService transferService;

    private Card fromCard;
    private Card toCard;
    private TransferPaymentRequest request;

    @BeforeEach
    void setUp() {
        fromCard = new Card();
        fromCard.setCardNumber("1111111111111111");
        fromCard.setValidTill("12/27");
        fromCard.setCvv("123");
        fromCard.setBalance(BigDecimal.valueOf(10000));

        toCard = new Card();
        toCard.setCardNumber("2222222222222222");
        toCard.setValidTill("11/28");
        toCard.setCvv("456");
        toCard.setBalance(BigDecimal.valueOf(5000));

        Amount amount = new Amount();
        amount.setValue(BigDecimal.valueOf(1000));
        amount.setCurrency("RUB");

        request = new TransferPaymentRequest();
        request.setCardFromNumber("1111111111111111");
        request.setCardFromValidTill("12/27");
        request.setCardFromCVV("123");
        request.setCardToNumber("2222222222222222");
        request.setAmount(amount);
    }

    private void setupMockCards() {
        when(cardRepository.findByCardNumber("1111111111111111")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumber("2222222222222222")).thenReturn(Optional.of(toCard));
    }

    @Test
    void testInitiateTransferSuccess() {
        // Arrange
        setupMockCards();

        // Act
        String operationId = transferService.initiateTransfer(request);

        // Assert
        assertNotNull(operationId);
        verify(cardRepository, times(2)).findByCardNumber(anyString());
    }

    @Test
    void testInitiateTransferFromCardNotFound() {
        // Arrange
        when(cardRepository.findByCardNumber("1111111111111111")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.initiateTransfer(request));
        assertEquals("Карта отправителя не найдена", exception.getMessage());
    }

    @Test
    void testInitiateTransferToCardNotFound() {
        // Arrange
        when(cardRepository.findByCardNumber("1111111111111111")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumber("2222222222222222")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.initiateTransfer(request));
        assertEquals("Карта получателя не найдена", exception.getMessage());
    }

    @Test
    void testInitiateTransferInvalidCardData() {
        // Arrange
        fromCard.setCvv("999");
        setupMockCards();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.initiateTransfer(request));
        assertEquals("Неверные данные карты", exception.getMessage());
    }

    @Test
    void testInitiateTransferInsufficientFunds() {
        // Arrange
        fromCard.setBalance(BigDecimal.valueOf(100));
        setupMockCards();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.initiateTransfer(request));
        assertEquals("Недостаточно средств", exception.getMessage());
    }

    @Test
    void testConfirmTransferSuccess() {
        // Arrange
        setupMockCards();
        String operationId = transferService.initiateTransfer(request);

        BigDecimal initialFromBalance = fromCard.getBalance();
        BigDecimal initialToBalance = toCard.getBalance();

        // Act - confirmTransfer НЕ требует моков, так как карты уже в операции
        transferService.confirmTransfer(operationId, "1234");

        // Assert
        verify(cardRepository, times(2)).save(any(Card.class));
        verify(loggingService, times(1)).logTransferSuccess(anyString(), anyString(), anyString(),
                any(BigDecimal.class), any(BigDecimal.class), anyString());

        // Проверяем, что балансы изменились
        assertTrue(fromCard.getBalance().compareTo(initialFromBalance) < 0);
        assertTrue(toCard.getBalance().compareTo(initialToBalance) > 0);
    }

    @Test
    void testConfirmTransferOperationNotFound() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.confirmTransfer("invalid-id", "1234"));
        assertEquals("Операция не найдена", exception.getMessage());

        verify(loggingService, times(1)).logTransferFailed(anyString(), anyString(), anyString(),
                any(BigDecimal.class), any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void testConfirmTransferNullOperationId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.confirmTransfer(null, "1234"));
        assertEquals("Operation ID обязателен", exception.getMessage());
    }

    @Test
    void testConfirmTransferEmptyOperationId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.confirmTransfer("   ", "1234"));
        assertEquals("Operation ID обязателен", exception.getMessage());
    }

    @Test
    void testConfirmTransferEmptyCode() {
        // Arrange
        setupMockCards();
        String operationId = transferService.initiateTransfer(request);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.confirmTransfer(operationId, ""));
        assertEquals("Неверный код подтверждения", exception.getMessage());
    }

    @Test
    void testConfirmTransferNullCode() {
        // Arrange
        setupMockCards();
        String operationId = transferService.initiateTransfer(request);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.confirmTransfer(operationId, null));
        assertEquals("Неверный код подтверждения", exception.getMessage());
    }


    @Test
    void testCommissionCalculation() {
        // Arrange
        setupMockCards();
        String operationId = transferService.initiateTransfer(request);

        BigDecimal initialBalance = BigDecimal.valueOf(10000);
        BigDecimal amount = BigDecimal.valueOf(1000);
        BigDecimal commission = amount.multiply(BigDecimal.valueOf(0.01)); // 10
        BigDecimal total = amount.add(commission); // 1010

        // Act
        transferService.confirmTransfer(operationId, "1234");

        // Assert
        BigDecimal expectedBalance = initialBalance.subtract(total);
        assertEquals(expectedBalance, fromCard.getBalance());
    }
}