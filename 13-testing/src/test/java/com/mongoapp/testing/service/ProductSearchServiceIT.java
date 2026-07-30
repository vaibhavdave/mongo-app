package com.mongoapp.testing.service;

import com.mongoapp.testing.AbstractIntegrationTest;
import com.mongoapp.testing.model.Product;
import com.mongoapp.testing.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full @SpringBootTest - the whole application context, still against
 * the same Testcontainers-managed Mongo. Slower to start than the
 * @DataMongoTest slice in ProductRepositoryIT, but exercises the real
 * wiring end to end: service -> MongoTemplate -> driver -> database.
 */
@SpringBootTest
class ProductSearchServiceIT extends AbstractIntegrationTest {

    @Autowired
    private ProductSearchService productSearchService;
    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanUp() {
        productRepository.deleteAll();
    }

    @Test
    void findInStockByCategorySortedByPrice_excludesOutOfStockAndSortsAscending() {
        productRepository.save(new Product(null, "Mid", "tools", 20.0, 5));
        productRepository.save(new Product(null, "Cheap", "tools", 10.0, 2));
        productRepository.save(new Product(null, "OutOfStock", "tools", 5.0, 0));
        productRepository.save(new Product(null, "OtherCategory", "electronics", 1.0, 5));

        List<Product> results = productSearchService.findInStockByCategorySortedByPrice("tools");

        assertThat(results).extracting(Product::getName).containsExactly("Cheap", "Mid");
    }
}
