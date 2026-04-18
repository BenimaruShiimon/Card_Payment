package com.github.benimarushiimon.cardpayment.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


public enum TransferStatus {
    PENDING,
    SUCCESS,
    FAILED
}