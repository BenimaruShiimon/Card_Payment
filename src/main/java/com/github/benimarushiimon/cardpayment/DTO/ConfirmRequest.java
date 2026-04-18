package com.github.benimarushiimon.cardpayment.DTO;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ConfirmRequest {
    private String operationId;
    private String code;
}
