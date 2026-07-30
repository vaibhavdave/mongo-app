package com.mongoapp.textgeosearch.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PolygonRequest {

    /** Each entry is [lng, lat]. The ring must close (first point == last point). */
    @NotEmpty
    private List<List<Double>> points;
}
