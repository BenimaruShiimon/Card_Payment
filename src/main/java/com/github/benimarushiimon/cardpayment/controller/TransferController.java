package com.github.benimarushiimon.cardpayment.controller;

import com.github.benimarushiimon.cardpayment.DTO.ConfirmRequest;
import com.github.benimarushiimon.cardpayment.DTO.TransferPaymentRequest;
import com.github.benimarushiimon.cardpayment.exception.ErrorResponse;
import com.github.benimarushiimon.cardpayment.service.TransferService;
import io.micrometer.core.instrument.config.validate.ValidationException;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@CrossOrigin(
        origins = "https://serp-ya.github.io",
        allowCredentials = "true",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})

public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("transfer")
    public ResponseEntity<?> createTransfer(@Valid @RequestBody TransferPaymentRequest request) {
        try {
            String operationId = transferService.initiateTransfer(request);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ \"operationId\": \"" + operationId + "\" }");
        } catch (ValidationException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/confirmOperation")
    public ResponseEntity<?> confirmTransfer(@RequestBody ConfirmRequest request) {
        try {
            transferService.confirmTransfer(request.getOperationId(), request.getCode());
            return ResponseEntity.ok().build();
        } catch (ValidationException e) {
            ErrorResponse error = new ErrorResponse();
            error.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        ErrorResponse error = new ErrorResponse();
        // Собираем все сообщения об ошибках в одну строку
        String errorMessage = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        error.setMessage(errorMessage);
        return ResponseEntity.badRequest().body(error); // Возвращаем JSON с ошибкой и статусом 400
    }
}