package com.contentnexus.iam.service.model;

import java.time.LocalDateTime;
import java.util.UUID;

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

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // toString method
    @Override
    public String toString() {
        return "BaseEntity{" +
                "id='" + id + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
