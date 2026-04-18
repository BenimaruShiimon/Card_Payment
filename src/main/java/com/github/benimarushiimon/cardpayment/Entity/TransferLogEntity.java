package com.github.benimarushiimon.cardpayment.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String operationId;

    @Column(nullable = false)
    private String cardFrom;

    @Column(nullable = false)
    private String cardTo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, columnDefinition = "SMALLINT")
    private TransferStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String errorMessage;
}