package com.ecommerce.order.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port to the Inventory context. Implementations throw ExternalServiceUnavailableException if it is down. */
public interface ProductCatalog {

    Optional<ProductSnapshot> findProduct(UUID productId);

    List<ProductSnapshot> searchProducts(String keyword);
}
