package com.mongoapp.transactions.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "accounts")
@Data
@NoArgsConstructor
public class BankAccount {

    @Id
    private String id;

    @NotBlank
    private String owner;

    private double balance;

    /**
     * @Version enables optimistic locking: Spring Data includes the
     * current version in the update filter ({"_id": ..., "version": N})
     * and increments it on write. If another write already bumped the
     * version, this filter matches nothing, the update reports zero
     * documents modified, and Spring Data raises
     * OptimisticLockingFailureException instead of silently overwriting
     * the other change.
     */
    @Version
    private Long version;
}
