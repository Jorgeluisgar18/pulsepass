package com.pulsepass.service.pricing;

import com.pulsepass.domain.TicketType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TicketPricingPolicyTest {

    private final TicketPricingPolicy pricingPolicy =
            new TicketPricingPolicy();


    @Test
    void shouldCalculateGeneralPrice() {

        assertThat(
                pricingPolicy.calculate(
                        TicketType.GENERAL
                )
        ).isEqualByComparingTo(
                new BigDecimal("120000.00")
        );
    }


    @Test
    void shouldCalculateStudentPrice() {

        assertThat(
                pricingPolicy.calculate(
                        TicketType.STUDENT
                )
        ).isEqualByComparingTo(
                new BigDecimal("90000.00")
        );
    }


    @Test
    void shouldCalculateVipPrice() {

        assertThat(
                pricingPolicy.calculate(
                        TicketType.VIP
                )
        ).isEqualByComparingTo(
                new BigDecimal("250000.00")
        );
    }


    @Test
    void shouldCalculateBackstagePrice() {

        assertThat(
                pricingPolicy.calculate(
                        TicketType.BACKSTAGE
                )
        ).isEqualByComparingTo(
                new BigDecimal("400000.00")
        );
    }
}