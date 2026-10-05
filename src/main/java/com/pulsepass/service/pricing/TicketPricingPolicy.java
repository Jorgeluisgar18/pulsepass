package com.pulsepass.service.pricing;

import com.pulsepass.domain.TicketType;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TicketPricingPolicy {

    private static final BigDecimal GENERAL_PRICE =
            new BigDecimal("120000.00");

    private static final BigDecimal STUDENT_PRICE =
            new BigDecimal("90000.00");

    private static final BigDecimal VIP_PRICE =
            new BigDecimal("250000.00");

    private static final BigDecimal BACKSTAGE_PRICE =
            new BigDecimal("400000.00");

    public BigDecimal calculate(TicketType type) {

        return switch (type) {
            case GENERAL -> GENERAL_PRICE;
            case STUDENT -> STUDENT_PRICE;
            case VIP -> VIP_PRICE;
            case BACKSTAGE -> BACKSTAGE_PRICE;
        };
    }
}