package com.contentnexus.iam.service.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
public abstract class BaseEntity {
    private String id;
    private LocalDateTime createdAt;

    // Default constructor for new entities
    public BaseEntity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // Constructor for persistence (e.g., when loading from DB or event)
    public BaseEntity(String id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }
}
