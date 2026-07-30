package com.mongoapp.textgeosearch.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StoreCreateRequest {

    @NotBlank
    private String name;

    private String description;

    private String category;

    @NotNull
    private Double lng;

    @NotNull
    private Double lat;
}
