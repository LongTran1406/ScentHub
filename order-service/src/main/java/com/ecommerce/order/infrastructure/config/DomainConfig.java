package com.ecommerce.order.infrastructure.config;

import com.ecommerce.order.domain.service.CancellationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers framework-free domain services as Spring beans (the domain itself has no Spring annotations). */
@Configuration
public class DomainConfig {

    @Bean
    public CancellationPolicy cancellationPolicy() {
        return new CancellationPolicy();
    }
}
