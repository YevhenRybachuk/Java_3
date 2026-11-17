package org.example.lab1.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionResponseDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private LocalDate billingDate;
    private Long userId;
    private String username;
    private Long categoryId;
    private String categoryName;
}
