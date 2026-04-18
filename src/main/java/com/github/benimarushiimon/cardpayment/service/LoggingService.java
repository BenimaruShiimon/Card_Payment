package com.github.benimarushiimon.cardpayment.service;

import com.github.benimarushiimon.cardpayment.Entity.TransferLogEntity;
import com.github.benimarushiimon.cardpayment.Entity.TransferStatus;
import com.github.benimarushiimon.cardpayment.repository.TransferLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class LoggingService {
    @Value("${transfer.log.operations-file:deploy/transfer.log}")
    private String operationsLogFile;

    private final TransferLogRepository transferLogRepository;

    public LoggingService(TransferLogRepository transferLogRepository, TransferLogRepository transferLogRepository1) {
        this.transferLogRepository = transferLogRepository1;
    }

    public void logTransferSuccess(String operationId, String cardFrom, String cardTo,
                                   BigDecimal amount, BigDecimal fee, String currency) {
        logTransfer(operationId, cardFrom, cardTo, amount, fee, currency, String.valueOf(TransferStatus.SUCCESS), null);
    }

    public void logTransferFailed(String operationId, String cardFrom, String cardTo,
                                  BigDecimal amount, BigDecimal fee, String currency, String errorMessage) {
        logTransfer(operationId, cardFrom, cardTo, amount, fee, currency, String.valueOf(TransferStatus.FAILED), errorMessage);
    }

    private void logTransfer(String operationId, String cardFrom, String cardTo,
                             BigDecimal amount, BigDecimal fee, String currency,
                             String status, String errorMessage) {

        LocalDateTime now = LocalDateTime.now();
        logToFile(now, cardFrom, cardTo, amount, fee, currency, TransferStatus.valueOf(status), operationId);
        saveToDatabase(operationId, cardFrom, cardTo, amount, fee, currency, TransferStatus.valueOf(status), now, errorMessage);
    }
    private void logToFile (LocalDateTime now, String cardFrom, String cardTo,
                            BigDecimal amount, BigDecimal fee, String currency,
                            TransferStatus status, String operationI) {
        ensureDirectoryExists(operationsLogFile);

        String line = String.format(
                "[%s] FROM=%s TO=%s AMOUNT=%.2f/%s FEE=%.2f STATUS=%s OP_ID=%s%n",
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                cardFrom,
                cardTo,
                amount,
                currency,
                fee,
                status,
                operationI
        );
        try (FileWriter writer = new FileWriter(operationsLogFile, true)){
            writer.write(line);
        } catch (IOException e){
            System.err.println("Ошибка записи логирования в файл: " + e.getMessage());
        }
    }
    private void saveToDatabase (String operationId, String cardFrom, String cardTo,
                                 BigDecimal amount, BigDecimal fee, String currency,
                                 TransferStatus status, LocalDateTime createdAt, String errorMessage) {
        TransferLogEntity log = TransferLogEntity.builder()
                .operationId(operationId)
                .cardFrom(cardFrom)
                .cardTo(cardTo)
                .amount(amount)
                .fee(fee)
                .currency(currency)
                .status(status)
                .createdAt(createdAt)
                .errorMessage(errorMessage)
                .build();
        try {
            transferLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Ошибка записи логирования в БД: " + e.getMessage());
        }
    }

    private void ensureDirectoryExists(String filePath){
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()){
            if (parentDir.mkdir()){
                System.out.println("Папка для логов создана успешно: " + parentDir.getAbsolutePath());
            }
        }
    }
}
