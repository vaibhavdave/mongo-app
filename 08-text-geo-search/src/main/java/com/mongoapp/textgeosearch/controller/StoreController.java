package com.mongoapp.textgeosearch.controller;

import com.mongoapp.textgeosearch.model.*;
import com.mongoapp.textgeosearch.repository.StoreRepository;
import com.mongoapp.textgeosearch.service.StoreSearchService;
import jakarta.validation.Valid;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreRepository storeRepository;
    private final StoreSearchService searchService;

    public StoreController(StoreRepository storeRepository, StoreSearchService searchService) {
        this.storeRepository = storeRepository;
        this.searchService = searchService;
    }

    @PostMapping
    public ResponseEntity<Store> create(@Valid @RequestBody StoreCreateRequest request) {
        Store store = new Store();
        store.setName(request.getName());
        store.setDescription(request.getDescription());
        store.setCategory(request.getCategory());
        store.setLocation(new GeoJsonPoint(request.getLng(), request.getLat()));
        return ResponseEntity.status(HttpStatus.CREATED).body(storeRepository.save(store));
    }

    @GetMapping
    public List<Store> findAll() {
        return storeRepository.findAll();
    }

    @GetMapping("/near")
    public List<StoreWithDistance> near(@RequestParam double lng, @RequestParam double lat,
                                         @RequestParam(defaultValue = "10") double maxDistanceKm) {
        return searchService.findNear(lng, lat, maxDistanceKm);
    }

    @GetMapping("/within-radius")
    public List<Store> withinRadius(@RequestParam double lng, @RequestParam double lat,
                                     @RequestParam(defaultValue = "10") double radiusKm) {
        return searchService.findWithinRadius(lng, lat, radiusKm);
    }

    @PostMapping("/within-polygon")
    public List<Store> withinPolygon(@Valid @RequestBody PolygonRequest request) {
        List<Point> points = request.getPoints().stream()
                .map(p -> new Point(p.get(0), p.get(1)))
                .toList();
        return searchService.findWithinPolygon(points);
    }

    @GetMapping("/search/text")
    public List<Store> textSearch(@RequestParam String q) {
        return searchService.textSearch(q);
    }
}
