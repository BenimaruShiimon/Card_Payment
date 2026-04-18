package com.github.benimarushiimon.cardpayment.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String cardFromNumber;

    @Column(name = "card_to_number", nullable = false)
    private String cardToNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String errorMessage;
    }
