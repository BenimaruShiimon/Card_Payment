package com.github.benimarushiimon.cardpayment.exception;

import org.springframework.stereotype.Component;


public class ValidationException extends RuntimeException {
    private Integer errorCode;

    public ValidationException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}