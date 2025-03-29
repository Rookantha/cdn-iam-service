package com.contentnexus.iam.service.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class User extends BaseEntity {
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean enabled;
    private List<Map<String, Object>> credentials;

    // Default constructor
    public User() {}

    // Parameterized constructor
    public User(String id, String username, String firstName, String lastName, String email, String role,
                boolean enabled, List<Map<String, Object>> credentials) {
        super(id, null); // `createdAt` will be set in BaseEntity constructor
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.credentials = credentials;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Map<String, Object>> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<Map<String, Object>> credentials) {
        this.credentials = credentials;
    }

    // Password handling
    public void setPassword(String password) {
        this.credentials = Collections.singletonList(Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        ));
    }

    public String getPassword() {
        if (credentials != null && !credentials.isEmpty()) {
            Object passwordValue = credentials.get(0).get("value");
            return passwordValue != null ? passwordValue.toString() : null;
        }
        return null;
    }

    // toString method
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", enabled=" + enabled +
                ", credentials=" + credentials +
                "} " + super.toString();
    }
}
