package com.mongoapp.testing.repository;

import com.mongoapp.testing.AbstractIntegrationTest;
import com.mongoapp.testing.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataMongoTest boots only the Mongo-related slice of the app context
 * (repositories, MongoTemplate) - much faster to start than a full
 * @SpringBootTest - but still runs against a real, throwaway MongoDB
 * (see AbstractIntegrationTest) instead of a mock. This is the tier for
 * testing what a derived query method or MongoTemplate query actually
 * does against real BSON, which no amount of mocking can verify.
 */
@DataMongoTest
class ProductRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanUp() {
        productRepository.deleteAll();
    }

    @Test
    void findByCategory_returnsOnlyMatchingProducts() {
        productRepository.save(new Product(null, "Widget", "tools", 9.99, 5));
        productRepository.save(new Product(null, "Gadget", "electronics", 19.99, 3));

        List<Product> tools = productRepository.findByCategory("tools");

        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).getName()).isEqualTo("Widget");
    }

    @Test
    void findByPriceLessThan_excludesMoreExpensiveProducts() {
        productRepository.save(new Product(null, "Cheap", "misc", 5.00, 10));
        productRepository.save(new Product(null, "Expensive", "misc", 500.00, 1));

        List<Product> affordable = productRepository.findByPriceLessThan(100.0);

        assertThat(affordable).extracting(Product::getName).containsExactly("Cheap");
    }
}
