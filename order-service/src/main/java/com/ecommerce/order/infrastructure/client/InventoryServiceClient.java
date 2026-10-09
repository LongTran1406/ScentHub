package com.ecommerce.order.infrastructure.client;

import com.ecommerce.order.domain.exception.ExternalServiceUnavailableException;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.port.ProductCatalog;
import com.ecommerce.order.domain.port.ProductSnapshot;
import com.ecommerce.order.infrastructure.client.dto.InventoryProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** REST adapter for the Inventory Service (port: {@link ProductCatalog}). */
@Component
public class InventoryServiceClient implements ProductCatalog {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public InventoryServiceClient(RestTemplate restTemplate,
                                  @Value("${product-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public Optional<ProductSnapshot> findProduct(UUID productId) {
        try {
            InventoryProductResponse response = restTemplate.getForObject(
                    baseUrl + "/products/{id}", InventoryProductResponse.class, productId);
            return Optional.ofNullable(response).map(InventoryServiceClient::toSnapshot);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ExternalServiceUnavailableException("Inventory Service is unavailable", e);
        }
    }

    @Override
    public List<ProductSnapshot> searchProducts(String keyword) {
        try {
            InventoryProductResponse[] response = restTemplate.getForObject(
                    baseUrl + "/products?search={keyword}", InventoryProductResponse[].class, keyword);
            return response == null
                    ? List.of()
                    : Arrays.stream(response).map(InventoryServiceClient::toSnapshot).toList();
        } catch (RestClientException e) {
            throw new ExternalServiceUnavailableException("Inventory Service is unavailable", e);
        }
    }

    private static ProductSnapshot toSnapshot(InventoryProductResponse r) {
        return new ProductSnapshot(
                UUID.fromString(r.getProductId()),
                r.getName(),
                new Money(r.getPrice()),
                r.getAvailableQuantity() == null ? 0 : r.getAvailableQuantity());
    }
}
