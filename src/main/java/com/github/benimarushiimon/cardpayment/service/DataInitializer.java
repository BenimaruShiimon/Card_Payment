package com.github.benimarushiimon.cardpayment.service;

import com.github.benimarushiimon.cardpayment.Entity.Card;
import com.github.benimarushiimon.cardpayment.repository.CardRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {
    private final CardRepository cardRepository;

    public DataInitializer(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (cardRepository.count() == 0) {
            Card card1 = new Card();
            card1.setCardNumber("1111111111111111");
            card1.setValidTill("12/27");
            card1.setCvv("123");
            card1.setBalance(BigDecimal.valueOf(1000000));

            Card card2 = new Card();
            card2.setCardNumber("2222222222222222");
            card2.setValidTill("11/26");
            card2.setCvv("456");
            card2.setBalance(BigDecimal.valueOf(500000));

            cardRepository.save(card1);
            cardRepository.save(card2);

            System.out.println("Тестовые карты добавлены: 1111111111111111 (баланс 10000) и 2222222222222222 (баланс 5000)");
        }
    }
}