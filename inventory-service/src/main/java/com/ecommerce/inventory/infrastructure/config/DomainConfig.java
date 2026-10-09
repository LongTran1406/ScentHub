package com.ecommerce.inventory.infrastructure.config;

import com.ecommerce.inventory.domain.service.StockReservationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers framework-free domain services as Spring beans (the domain itself has no Spring annotations). */
@Configuration
public class DomainConfig {

    @Bean
    public StockReservationService stockReservationService() {
        return new StockReservationService();
    }
}
