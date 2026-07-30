package com.mongoapp.textgeosearch.service;

import com.mongoapp.textgeosearch.model.Store;
import com.mongoapp.textgeosearch.model.StoreWithDistance;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class StoreSearchService {

    private final MongoTemplate mongoTemplate;

    public StoreSearchService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * $geoNear via NearQuery: not just "which stores are within range" but
     * "here's every matching store, sorted nearest-first, with its actual
     * distance attached." That per-result distance is the thing plain
     * $geoWithin queries don't give you for free.
     */
    public List<StoreWithDistance> findNear(double lng, double lat, double maxDistanceKm) {
        NearQuery nearQuery = NearQuery.near(new Point(lng, lat), Metrics.KILOMETERS)
                .maxDistance(new Distance(maxDistanceKm, Metrics.KILOMETERS))
                .spherical(true);

        GeoResults<Store> results = mongoTemplate.geoNear(nearQuery, Store.class);
        return results.getContent().stream()
                .map(r -> new StoreWithDistance(r.getContent(), r.getDistance().getValue()))
                .collect(Collectors.toList());
    }

    /**
     * $geoWithin with a spherical circle ($centerSphere under the hood) -
     * "is this point within N km of here", without needing/returning a
     * distance for each result. Cheaper than $geoNear when you don't
     * actually need the distance or the nearest-first ordering.
     */
    public List<Store> findWithinRadius(double lng, double lat, double radiusKm) {
        Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS));
        Query geoQuery = query(where("location").withinSphere(circle));
        return mongoTemplate.find(geoQuery, Store.class);
    }

    /** $geoWithin with an arbitrary GeoJSON polygon - "is this point inside this shape". */
    public List<Store> findWithinPolygon(List<Point> polygonPoints) {
        GeoJsonPolygon polygon = new GeoJsonPolygon(polygonPoints);
        Query geoQuery = query(where("location").within(polygon));
        return mongoTemplate.find(geoQuery, Store.class);
    }

    /** Recap of module 04's text search, applied to store name/description. */
    public List<Store> textSearch(String search) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(search.split("\\s+"));
        TextQuery textQuery = TextQuery.queryText(criteria).sortByScore();
        return mongoTemplate.find(textQuery, Store.class);
    }
}
