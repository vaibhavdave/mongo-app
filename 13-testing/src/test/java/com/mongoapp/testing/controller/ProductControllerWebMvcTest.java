package com.mongoapp.testing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongoapp.testing.model.Product;
import com.mongoapp.testing.repository.ProductRepository;
import com.mongoapp.testing.service.ProductSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The fastest tier: only the web layer boots - no real or embedded
 * database, no Testcontainers, no Spring Data machinery at all. The
 * repository and service are plain Mockito mocks (@MockBean). Good for
 * verifying request/response shape, status codes, and validation wiring
 * without paying the cost of starting a database for every test.
 *
 * Compare startup cost: this class starts in a fraction of the time
 * ProductRepositoryIT/ProductSearchServiceIT take, precisely because it
 * never touches MongoDB.
 */
@WebMvcTest(ProductController.class)
class ProductControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private ProductSearchService productSearchService;

    @Test
    void findAll_returnsProductsFromRepository() throws Exception {
        when(productRepository.findAll()).thenReturn(List.of(new Product("1", "Widget", "tools", 9.99, 5)));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Widget"));
    }

    @Test
    void create_withBlankName_returns400() throws Exception {
        Product invalid = new Product(null, "", "tools", 9.99, 5);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
