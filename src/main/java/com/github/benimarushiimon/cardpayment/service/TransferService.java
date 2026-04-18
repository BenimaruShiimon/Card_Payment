package com.github.benimarushiimon.cardpayment.service;

import com.github.benimarushiimon.cardpayment.DTO.TransferPaymentRequest;
import com.github.benimarushiimon.cardpayment.Entity.Card;
import com.github.benimarushiimon.cardpayment.repository.CardRepository;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransferService {
    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);
    private final CardRepository cardRepository;
    private final LoggingService loggingService;
    private final ConcurrentHashMap<String, TransferData> pendingTransfers = new ConcurrentHashMap<>();

    public TransferService(CardRepository cardRepository, LoggingService loggingService) {
        this.cardRepository = cardRepository;
        this.loggingService = loggingService;
    }

    @Transactional
    public String initiateTransfer(TransferPaymentRequest request) {
        String operationId = UUID.randomUUID().toString();

        try {
            // 1. Ищем карту отправителя
            Card fromCard = getFromCard(request);

            // 2. Ищем карту получателя
            Card toCard = getToCard(request);

            // 3. Валидируем данные карты
            validate(request, fromCard);

            // 4. Извлекаем сумму
            BigDecimal amount = request.getAmount().getValue();
            BigDecimal commission = calculateCommission(amount);
            BigDecimal totalSum = amount.add(commission);

            // 5. Проверяем баланс
            checkBalance(request, fromCard, totalSum, amount, commission);

            // 6. Сохраняем данные операции (для подтверждения)
            TransferData transferData = new TransferData(operationId, fromCard, toCard, amount, commission, request);
            pendingTransfers.put(operationId, transferData);

            logger.info("Операция инициирована: {}", operationId);
            return operationId;

        } catch (IllegalArgumentException e) {
            logger.error("Ошибка инициации операции: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void confirmTransfer(String operationId, String code) {

        if (operationId == null || operationId.trim().isEmpty()) {
            logger.error("Operation ID is null or empty");
            throw new IllegalArgumentException("Operation ID обязателен");
        }

        try {
            if (code == null || code.isEmpty()) {
                logger.error("Пустой код подтверждения для операции: {}", operationId);
                loggingService.logTransferFailed(operationId, "", "", BigDecimal.ZERO, BigDecimal.ZERO, "", "Пустой код");
                throw new IllegalArgumentException("Неверный код подтверждения");
            }

            TransferData transferData = pendingTransfers.get(operationId);
            if (transferData == null) {
                logger.error("Операция не найдена: {}", operationId);
                loggingService.logTransferFailed(operationId, "", "", BigDecimal.ZERO, BigDecimal.ZERO, "", "Операция не найдена");
                throw new IllegalArgumentException("Операция не найдена");
            }

            transaction(transferData);

            loggingService.logTransferSuccess(
                    operationId,
                    transferData.fromCard.getCardNumber(),
                    transferData.toCard.getCardNumber(),
                    transferData.amount,
                    transferData.commission,
                    transferData.request.getAmount().getCurrency()
            );

            logger.info("Операция подтверждена и завершена: {}", operationId);

            pendingTransfers.remove(operationId);

        } catch (IllegalArgumentException e) {
            logger.error("Ошибка подтверждения операции {}: {}", operationId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при подтверждении {}: {}", operationId, e.getMessage(), e);
            loggingService.logTransferFailed(operationId, "", "", BigDecimal.ZERO, BigDecimal.ZERO, "", e.getMessage());
            throw new IllegalArgumentException("Ошибка при подтверждении: " + e.getMessage());
        }
    }

    private void transaction(TransferData transferData) {
        Card fromCard = transferData.fromCard;
        Card toCard = transferData.toCard;
        BigDecimal totalSum = transferData.amount.add(transferData.commission);

        if (fromCard.getBalance().compareTo(totalSum) < 0) {
            String error = "Недостаточно средств при подтверждении";
            loggingService.logTransferFailed(
                    transferData.operationId,
                    fromCard.getCardNumber(),
                    toCard.getCardNumber(),
                    transferData.amount,
                    transferData.commission,
                    transferData.request.getAmount().getCurrency(),
                    error
            );
            throw new IllegalArgumentException(error);
        }

        fromCard.setBalance(fromCard.getBalance().subtract(totalSum));
        toCard.setBalance(toCard.getBalance().add(transferData.amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        logger.info("Транзакция проведена: {} {} → {}",
                transferData.amount,
                fromCard.getCardNumber(),
                toCard.getCardNumber());
    }

    private static void checkBalance(TransferPaymentRequest request, Card fromCard, BigDecimal totalSum, BigDecimal amount, BigDecimal commission) {
        if (fromCard.getBalance().compareTo(totalSum) < 0) {
            logger.warn("Недостаточно средств: карта {}, баланс {}, требуется {}",
                    request.getCardFromNumber(),
                    fromCard.getBalance(),
                    totalSum);
            throw new IllegalArgumentException("Недостаточно средств");
        }
    }

    private static void validate(TransferPaymentRequest request, Card fromCard) {
        if (!fromCard.isValid(request.getCardFromValidTill(), request.getCardFromCVV())) {
            logger.warn("Неверные данные карты: {}", request.getCardFromNumber());
            throw new IllegalArgumentException("Неверные данные карты");
        }
    }

    private @NonNull Card getToCard(TransferPaymentRequest request) {
        return cardRepository.findByCardNumber(request.getCardToNumber())
                .orElseThrow(() -> {
                    logger.error("Карта получателя не найденав базе данных: {}", request.getCardToNumber());
                    return new IllegalArgumentException("Карта получателя не найдена");
                });
    }

    private @NonNull Card getFromCard(TransferPaymentRequest request) {
        return cardRepository.findByCardNumber(request.getCardFromNumber())
                .orElseThrow(() -> {
                    logger.error("Карта отправителя не найдена в базе данных: {}", request.getCardFromNumber());
                    return new IllegalArgumentException("Карта отправителя не найдена");
                });
    }

    private BigDecimal calculateCommission(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(0.01)); // 1%
    }

    // Внутренний класс для хранения операции
    public static class TransferData {
        public String operationId;
        public Card fromCard;
        public Card toCard;
        public BigDecimal amount;
        public BigDecimal commission;
        public TransferPaymentRequest request;
        public LocalDateTime createdAt;

        public TransferData(String operationId, Card fromCard, Card toCard, BigDecimal amount, BigDecimal commission, TransferPaymentRequest request) {
            this.operationId = operationId;
            this.fromCard = fromCard;
            this.toCard = toCard;
            this.amount = amount;
            this.commission = commission;
            this.request = request;
            this.createdAt = LocalDateTime.now();
        }
    }

    // Для тестирования
    public TransferData getPendingTransfer(String operationId) {
        return pendingTransfers.get(operationId);
    }
}