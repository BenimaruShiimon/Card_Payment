package com.github.benimarushiimon.cardpayment.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@Table(name = "cards")
@AllArgsConstructor
@NoArgsConstructor
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "card_number", unique = true, nullable = false)
    private String cardNumber;

    @Column(nullable = false)
    private String validTill;

    @Column(nullable = false)
    private String cvv;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    public boolean isValid(String validTill, String cvv) {
        return this.validTill.equals(validTill) && this.cvv.equals(cvv);
    }
}
