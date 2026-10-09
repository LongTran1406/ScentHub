package com.ecommerce.inventory.service;

import com.ecommerce.inventory.domain.event.InventoryEvent;
import com.ecommerce.inventory.domain.event.StockLine;
import com.ecommerce.inventory.domain.event.StockRejected;
import com.ecommerce.inventory.domain.event.StockReserved;
import com.ecommerce.inventory.domain.exception.InvalidProductException;
import com.ecommerce.inventory.domain.exception.ProductNotFoundException;
import com.ecommerce.inventory.domain.model.Money;
import com.ecommerce.inventory.domain.model.Product;
import com.ecommerce.inventory.domain.port.InventoryEventPublisher;
import com.ecommerce.inventory.domain.repository.ProductRepository;
import com.ecommerce.inventory.domain.service.StockReservationService;
import com.ecommerce.inventory.service.dto.CreateProductRequest;
import com.ecommerce.inventory.service.dto.ProductResponse;
import com.ecommerce.inventory.service.dto.UpdateProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Application-service tests (stock use cases and catalogue) with in-memory fakes. */
class StockServiceTest {

    static class InMemoryProducts implements ProductRepository {
        final Map<UUID, Product> store = new LinkedHashMap<>();
        public Product save(Product p) { store.put(p.getId(), p); return p; }
        public Optional<Product> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        public List<Product> findAllById(Collection<UUID> ids) {
            return ids.stream().map(store::get).filter(p -> p != null).toList();
        }
        public List<Product> findAll() { return new ArrayList<>(store.values()); }
        public List<Product> searchByName(String k) {
            return store.values().stream().filter(p -> p.getName().toLowerCase().contains(k.toLowerCase())).toList();
        }
        public boolean existsById(UUID id) { return store.containsKey(id); }
        public void deleteById(UUID id) { store.remove(id); }
        public List<Product> findLowStock(int t) {
            return store.values().stream().filter(p -> p.availableQuantity() < t).toList();
        }
    }

    static class RecordingPublisher implements InventoryEventPublisher {
        final List<InventoryEvent> published = new ArrayList<>();
        public void publish(InventoryEvent event) { published.add(event); }
    }

    private InMemoryProducts products;
    private RecordingPublisher publisher;
    private StockService stockService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        products = new InMemoryProducts();
        publisher = new RecordingPublisher();
        stockService = new StockService(products, new StockReservationService(), publisher);
        productService = new ProductService(products);
    }

    private Product stocked(int stock) {
        return products.save(Product.create("Rose Eau", "d", Money.of("79.99"), stock));
    }

    // ---- stock use cases ---------------------------------------------------------------------------------

    @Test
    void reserveForOrderReservesAndAnnouncesStockReserved() {
        Product p = stocked(5);

        stockService.reserveForOrder("order-1", List.of(new StockLine(p.getId(), 2)));

        assertThat(p.getReservedQuantity()).isEqualTo(2);
        assertThat(publisher.published).singleElement().isInstanceOf(StockReserved.class);
        assertThat(publisher.published.get(0).orderId()).isEqualTo("order-1");
    }

    @Test
    void reserveForOrderAnnouncesStockRejectedAndChangesNothingWhenShort() {
        Product p = stocked(1);

        stockService.reserveForOrder("order-2", List.of(new StockLine(p.getId(), 5)));

        assertThat(p.getReservedQuantity()).isZero();
        assertThat(publisher.published).singleElement().isInstanceOf(StockRejected.class);
    }

    @Test
    void releaseForOrderGivesTheStockBackAndAnnouncesNothing() {
        Product p = stocked(5);
        stockService.reserveForOrder("order-3", List.of(new StockLine(p.getId(), 4)));
        publisher.published.clear();

        stockService.releaseForOrder(List.of(new StockLine(p.getId(), 4)));

        assertThat(p.getReservedQuantity()).isZero();
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void releaseForAnUnknownProductIsSkippedWithoutFailing() {
        stockService.releaseForOrder(List.of(new StockLine(UUID.randomUUID(), 1)));

        assertThat(publisher.published).isEmpty();
    }

    // ---- catalogue ---------------------------------------------------------------------------------------

    private CreateProductRequest createRequest(String name, String price, Integer stock) {
        CreateProductRequest r = new CreateProductRequest();
        r.setName(name);
        r.setDescription("d");
        r.setPrice(price == null ? null : new BigDecimal(price));
        r.setStockQuantity(stock);
        return r;
    }

    @Test
    void createProductReturnsTheComputedAvailableQuantity() {
        ProductResponse response = productService.createProduct(createRequest("Oud", "49.50", 7));

        assertThat(response.getAvailableQuantity()).isEqualTo(7);
        assertThat(response.getReservedQuantity()).isZero();
        assertThat(response.getPrice()).isEqualByComparingTo("49.50");
    }

    @Test
    void createProductRejectsInvalidInput() {
        assertThatThrownBy(() -> productService.createProduct(createRequest("Oud", "-1", 7)))
                .isInstanceOf(InvalidProductException.class);
        assertThatThrownBy(() -> productService.createProduct(createRequest("Oud", null, 7)))
                .isInstanceOf(InvalidProductException.class);
        assertThatThrownBy(() -> productService.createProduct(createRequest("", "5", 7)))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void updateProductAppliesOnlyProvidedFields() {
        Product p = stocked(5);
        UpdateProductRequest update = new UpdateProductRequest();
        update.setPrice(new BigDecimal("12.34"));

        ProductResponse response = productService.updateProduct(p.getId(), update);

        assertThat(response.getPrice()).isEqualByComparingTo("12.34");
        assertThat(response.getName()).isEqualTo("Rose Eau");
        assertThat(response.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void searchIsCaseInsensitiveAndBlankMeansAll() {
        products.save(Product.create("Rose Eau", "d", Money.of("1"), 1));
        products.save(Product.create("Oud Noir", "d", Money.of("1"), 1));

        assertThat(productService.listProducts("ROSE")).hasSize(1);
        assertThat(productService.listProducts(" ")).hasSize(2);
        assertThat(productService.listProducts(null)).hasSize(2);
    }

    @Test
    void unknownProductsAreReportedAsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> productService.getProduct(unknown)).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> productService.deleteProduct(unknown)).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> productService.updateProduct(unknown, new UpdateProductRequest()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteRemovesTheProduct() {
        Product p = stocked(1);

        productService.deleteProduct(p.getId());

        assertThat(products.store).isEmpty();
    }
}
