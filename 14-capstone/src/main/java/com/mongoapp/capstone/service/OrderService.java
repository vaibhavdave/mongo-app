package com.mongoapp.capstone.service;

import com.mongoapp.capstone.exception.InsufficientStockException;
import com.mongoapp.capstone.exception.NotFoundException;
import com.mongoapp.capstone.model.*;
import com.mongoapp.capstone.repository.CustomerRepository;
import com.mongoapp.capstone.repository.OrderRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Placing an order combines two techniques from earlier modules:
 *
 *  - Each item's stock decrement is an atomic, conditional findAndModify
 *    (module 02) - {stock: {$gte: quantity}} + {$inc: {stock: -quantity}}
 *    in one server-side operation, so concurrent orders for the same
 *    product can't both succeed against insufficient stock.
 *
 *  - The whole method is @Transactional (module 06) - if item 3 of a
 *    4-item order fails because of insufficient stock, the successful
 *    decrements already applied for items 1 and 2 are rolled back too,
 *    and no Order document is created. Without the transaction, a failed
 *    multi-item order would leave some products' stock decremented with
 *    no corresponding order to show for it.
 */
@Service
public class OrderService {

    private final MongoTemplate mongoTemplate;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public OrderService(MongoTemplate mongoTemplate, OrderRepository orderRepository,
                         CustomerRepository customerRepository) {
        this.mongoTemplate = mongoTemplate;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Order placeOrder(OrderPlaceRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("No customer with id " + request.getCustomerId()));

        List<OrderItem> items = new ArrayList<>();
        double total = 0.0;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product decremented = decrementStockAtomically(itemRequest.getProductId(), itemRequest.getQuantity());
            items.add(new OrderItem(decremented.getId(), decremented.getName(), decremented.getPrice(), itemRequest.getQuantity()));
            total += decremented.getPrice() * itemRequest.getQuantity();
        }

        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setItems(items);
        order.setTotalAmount(total);
        order.setStatus("PLACED");
        order.setCreatedAt(Instant.now());

        return orderRepository.save(order);
    }

    private Product decrementStockAtomically(String productId, int quantity) {
        Update update = new Update().inc("stock", -quantity);
        Product updated = mongoTemplate.findAndModify(
                query(where("id").is(productId).and("stock").gte(quantity)),
                update,
                FindAndModifyOptions.options().returnNew(true),
                Product.class);

        if (updated == null) {
            throw new InsufficientStockException("Not enough stock for product " + productId
                    + " (or product does not exist) - requested " + quantity);
        }
        return updated;
    }
}
