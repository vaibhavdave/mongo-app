package com.mongoapp.aggregation.controller;

import com.mongoapp.aggregation.dto.CategorySales;
import com.mongoapp.aggregation.dto.OrderWithCustomer;
import com.mongoapp.aggregation.dto.ProductSales;
import com.mongoapp.aggregation.service.ReportService;
import org.bson.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales-by-category")
    public List<CategorySales> salesByCategory() {
        return reportService.salesByCategory();
    }

    @GetMapping("/top-products")
    public List<ProductSales> topProducts(@RequestParam(defaultValue = "5") int limit) {
        return reportService.topProducts(limit);
    }

    @GetMapping("/orders-with-customer")
    public List<OrderWithCustomer> ordersWithCustomer() {
        return reportService.ordersWithCustomer();
    }

    @GetMapping("/price-buckets")
    public List<Document> priceBuckets() {
        return reportService.priceBuckets();
    }

    @GetMapping("/dashboard")
    public Document dashboard() {
        return reportService.dashboard();
    }
}
