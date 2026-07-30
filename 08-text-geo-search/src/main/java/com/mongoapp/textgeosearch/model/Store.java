package com.mongoapp.textgeosearch.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "stores")
@Data
@NoArgsConstructor
public class Store {

    @Id
    private String id;

    @NotBlank
    @TextIndexed(weight = 2)
    private String name;

    @TextIndexed
    private String description;

    private String category;

    /**
     * GeoJSON Point: [longitude, latitude] - note the order, it trips
     * everyone up at least once (longitude first, like x before y - not
     * the "lat, lng" order most map UIs show you). The 2dsphere index
     * (below) understands this as a point on the Earth's surface and
     * enables spherical (great-circle) distance queries.
     */
    @NotNull
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;
}
