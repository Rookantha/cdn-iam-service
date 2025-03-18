package com.contentnexus.iam.service.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BaseEntity {
    private String id = UUID.randomUUID().toString();
    private LocalDateTime createdAt = LocalDateTime.now();
}
