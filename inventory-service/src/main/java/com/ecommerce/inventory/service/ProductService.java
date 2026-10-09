package com.ecommerce.inventory.service;

import com.ecommerce.inventory.domain.exception.InvalidProductException;
import com.ecommerce.inventory.domain.exception.ProductNotFoundException;
import com.ecommerce.inventory.domain.model.Money;
import com.ecommerce.inventory.domain.model.Product;
import com.ecommerce.inventory.domain.repository.ProductRepository;
import com.ecommerce.inventory.service.dto.CreateProductRequest;
import com.ecommerce.inventory.service.dto.ProductResponse;
import com.ecommerce.inventory.service.dto.UpdateProductRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Application service for the product catalogue (create / update / delete / browse). */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = Product.create(
                request.getName(), request.getDescription(), toMoney(request.getPrice()), request.getStockQuantity());
        return toResponse(productRepository.save(product));
    }

    /** Updates only the fields present in the request. */
    @Transactional
    public ProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = load(productId);
        product.update(request.getName(), request.getDescription(),
                request.getPrice() == null ? null : toMoney(request.getPrice()), request.getStockQuantity());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        productRepository.deleteById(productId);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(String searchKeyword) {
        List<Product> products = (searchKeyword == null || searchKeyword.isBlank())
                ? productRepository.findAll()
                : productRepository.searchByName(searchKeyword);
        return products.stream().map(ProductService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        return toResponse(load(productId));
    }

    private Product load(UUID productId) {
        return productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private static Money toMoney(BigDecimal price) {
        if (price == null) {
            return null;
        }
        try {
            return new Money(price);
        } catch (IllegalArgumentException e) {
            throw new InvalidProductException(e.getMessage());
        }
    }

    static ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice().amount(),
                p.getStockQuantity(), p.getReservedQuantity());
    }
}
