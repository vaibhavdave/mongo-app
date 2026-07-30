package com.mongoapp.capstone.service;

import com.mongoapp.capstone.AbstractIntegrationTest;
import com.mongoapp.capstone.exception.InsufficientStockException;
import com.mongoapp.capstone.model.*;
import com.mongoapp.capstone.repository.CustomerRepository;
import com.mongoapp.capstone.repository.OrderRepository;
import com.mongoapp.capstone.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The single most important behavior in this capstone, verified against
 * a real (Testcontainers-managed) database: when one item in a
 * multi-item order can't be fulfilled, EVERY stock decrement already
 * applied earlier in that same order is rolled back, and no Order
 * document is created. Without the @Transactional on
 * OrderService.placeOrder, the first item's stock would stay decremented
 * even though the order as a whole failed - exactly the kind of bug that
 * only shows up under a real transaction manager against a real replica
 * set, not against a mock.
 */
@SpringBootTest
class OrderServiceIT extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private OrderRepository orderRepository;

    private String customerId;
    private String plentifulProductId;
    private String scarceProductId;

    @BeforeEach
    void seedData() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();

        Customer customer = new Customer();
        customer.setName("Ada Lovelace");
        customer.setEmail("ada@example.com");
        customerId = customerRepository.save(customer).getId();

        Product plentiful = new Product();
        plentiful.setSku("sku-plentiful");
        plentiful.setName("Plentiful Widget");
        plentiful.setCategory("tools");
        plentiful.setPrice(10.0);
        plentiful.setStock(5);
        plentifulProductId = productRepository.save(plentiful).getId();

        Product scarce = new Product();
        scarce.setSku("sku-scarce");
        scarce.setName("Scarce Gadget");
        scarce.setCategory("electronics");
        scarce.setPrice(50.0);
        scarce.setStock(2);
        scarceProductId = productRepository.save(scarce).getId();
    }

    @Test
    void placeOrder_succeeds_whenAllItemsHaveEnoughStock() {
        OrderPlaceRequest request = new OrderPlaceRequest();
        request.setCustomerId(customerId);
        request.setItems(List.of(
                itemRequest(plentifulProductId, 2),
                itemRequest(scarceProductId, 1)
        ));

        Order order = orderService.placeOrder(request);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getTotalAmount()).isEqualTo(2 * 10.0 + 1 * 50.0);
        assertThat(productRepository.findById(plentifulProductId).orElseThrow().getStock()).isEqualTo(3);
        assertThat(productRepository.findById(scarceProductId).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void placeOrder_rollsBackEverything_whenAnyItemHasInsufficientStock() {
        // plentifulProductId has enough stock (5 >= 2) and would succeed on
        // its own; scarceProductId does not (2 < 10). The whole order must
        // fail, and plentifulProductId's stock must NOT stay decremented.
        OrderPlaceRequest request = new OrderPlaceRequest();
        request.setCustomerId(customerId);
        request.setItems(List.of(
                itemRequest(plentifulProductId, 2),
                itemRequest(scarceProductId, 10)
        ));

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(productRepository.findById(plentifulProductId).orElseThrow().getStock())
                .as("stock decremented for the first item must be rolled back")
                .isEqualTo(5);
        assertThat(productRepository.findById(scarceProductId).orElseThrow().getStock())
                .isEqualTo(2);
        assertThat(orderRepository.findAll())
                .as("no order should have been created")
                .isEmpty();
    }

    @Test
    void placeOrder_withUnknownCustomer_throwsBeforeTouchingStock() {
        OrderPlaceRequest request = new OrderPlaceRequest();
        request.setCustomerId("does-not-exist");
        request.setItems(List.of(itemRequest(plentifulProductId, 1)));

        assertThatThrownBy(() -> orderService.placeOrder(request)).isInstanceOf(RuntimeException.class);
        assertThat(productRepository.findById(plentifulProductId).orElseThrow().getStock()).isEqualTo(5);
    }

    private static OrderItemRequest itemRequest(String productId, int quantity) {
        OrderItemRequest request = new OrderItemRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }
}
