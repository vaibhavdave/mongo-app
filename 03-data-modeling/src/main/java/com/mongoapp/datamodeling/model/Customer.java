package com.mongoapp.datamodeling.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "customers")
@Data
@NoArgsConstructor
public class Customer {

    @Id
    private String id;

    @NotBlank
    private String name;

    @Email
    private String email;

    /** Embedded list - see Address for why. */
    private List<Address> addresses;
}
