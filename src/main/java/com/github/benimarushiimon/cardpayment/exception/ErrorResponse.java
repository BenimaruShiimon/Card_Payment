package com.github.benimarushiimon.cardpayment.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {
    private String message;
    private Integer id;
}
