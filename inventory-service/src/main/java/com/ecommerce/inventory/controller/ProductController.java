package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.CreateProductRequest;
import com.ecommerce.inventory.dto.ProductResponse;
import com.ecommerce.inventory.dto.UpdateProductRequest;
import com.ecommerce.inventory.exception.ProductNotFoundException;
import com.ecommerce.inventory.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** POST /products — create a product, returns 201 */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody CreateProductRequest request) {
        return ResponseEntity.status(201).body(productService.createProduct(request));
    }

    /** PUT /products/{productId} — update non-null fields */
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID productId,
            @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(productId, request));
    }

    /** DELETE /products/{productId} — returns 204 */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /products — returns all products.
     * GET /products?search=phone — filters by name (case-insensitive).
     * NOTE: param name "search" is used by Member 1's ProductLookupTool — do NOT rename.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> listProducts(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.listProducts(search));
    }

    /** GET /products/{productId} — returns a single product, or 404 */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<String> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }
}
