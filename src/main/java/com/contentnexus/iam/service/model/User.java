package com.contentnexus.iam.service.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean enabled;
    private List<Map<String, Object>> credentials;

    public void setPassword(String password) {
        this.credentials = Collections.singletonList(Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        ));
    }

    public String getPassword() {
        if (credentials != null && !credentials.isEmpty()) {
            return credentials.get(0).get("value").toString();
        }
        return null;
    }
}
