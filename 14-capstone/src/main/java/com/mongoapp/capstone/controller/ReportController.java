package com.mongoapp.capstone.controller;

import com.mongoapp.capstone.dto.ProductSales;
import com.mongoapp.capstone.dto.StatusSales;
import com.mongoapp.capstone.service.ReportService;
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

    @GetMapping("/top-products")
    public List<ProductSales> topProducts(@RequestParam(defaultValue = "5") int limit) {
        return reportService.topProducts(limit);
    }

    @GetMapping("/revenue-by-status")
    public List<StatusSales> revenueByStatus() {
        return reportService.revenueByStatus();
    }
}
