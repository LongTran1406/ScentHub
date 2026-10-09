package com.ecommerce.order.infrastructure.client;

import com.ecommerce.order.domain.exception.ExternalServiceUnavailableException;
import com.ecommerce.order.domain.port.CustomerDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/** REST adapter for the Customer Service (port: {@link CustomerDirectory}). */
@Component
public class CustomerServiceClient implements CustomerDirectory {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CustomerServiceClient(RestTemplate restTemplate,
                                 @Value("${customer-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean exists(UUID customerId) {
        try {
            restTemplate.getForObject(baseUrl + "/customers/{id}", String.class, customerId);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            throw new ExternalServiceUnavailableException("Customer Service is unavailable", e);
        }
    }
}
