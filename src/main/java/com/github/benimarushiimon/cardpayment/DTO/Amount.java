package com.github.benimarushiimon.cardpayment.DTO;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Amount {

    @NotNull(message = "Сумма не может быть null")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    private BigDecimal value;

    @NotBlank(message = "Валюта обязательна")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Валюта должна быть ISO 4217 кодом (например: RUB, USD)")
    private String currency;
}