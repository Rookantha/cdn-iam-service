package com.contentnexus.iam.service.service;

import com.contentnexus.iam.service.controller.AuthController;
import com.contentnexus.iam.service.exception.UserAlreadyExistsException;
import com.contentnexus.iam.service.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;



    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // 🔹 Register a new user in Keycloak
    public ResponseEntity<?> registerUser(User request) {
        System.out.println("🔹 Registering user: " + request.getUsername());

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Error: Password is required.");
        }

        String adminToken = getServiceAccountAccessToken();

        // 🔹 Check if user already exists
        String userId = getUserId(request.getUsername(), adminToken);
        if (userId != null) {
            throw new UserAlreadyExistsException("User with username '" + request.getUsername() + "' already exists.");
        }

        String url = keycloakServerUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String userJson = """
        {
            "username": "%s",
            "email": "%s",
            "firstName": "%s",
            "lastName": "%s",
            "enabled": true,
            "credentials": [
                {
                    "type": "password",
                    "value": "%s",
                    "temporary": false
                }
            ]
        }
        """.formatted(request.getUsername(), request.getEmail(), request.getFirstName(), request.getLastName(), request.getPassword());

        HttpEntity<String> entity = new HttpEntity<>(userJson, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            userId = getUserId(request.getUsername(), adminToken);
            if (userId != null) {
                assignRoleToUser(request.getUsername(), request.getRole(), adminToken);
                assignUserToGroup(request.getUsername(), "users", adminToken);  // ✅ Assign to a specific group
                return ResponseEntity.ok("✅ User registered successfully!");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Error: User ID retrieval failed.");
            }
        } else {
            return ResponseEntity.status(response.getStatusCode()).body("❌ User registration failed!");
        }
    }


    // 🔹 Assign a User to a Group
    private void assignUserToGroup(String username, String groupName, String adminToken) {
        String userId = getUserId(username, adminToken);
        if (userId == null) return;

        String groupId = getGroupId(groupName, adminToken);
        if (groupId == null) return;

        String url = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/groups/" + groupId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    // 🔹 Get Group ID by Name
    private String getGroupId(String groupName, String adminToken) {
        String url = keycloakServerUrl + "/admin/realms/" + realm + "/groups";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
        if (response.getBody() != null) {
            for (Object obj : response.getBody()) {
                Map<String, Object> group = (Map<String, Object>) obj;
                if (groupName.equals(group.get("name"))) {
                    return group.get("id").toString();
                }
            }
        }
        return null;
    }



    // 🔹 Assign a role to the user
    private void assignRoleToUser(String username, String role, String adminToken) {
        String userId = getUserId(username, adminToken);
        if (userId == null) {
            System.err.println("❌ User ID not found for username: " + username);
            return;
        }

        String roleId = getRoleId(role, adminToken);
        if (roleId == null) {
            System.err.println("❌ Role ID not found for role: " + role);
            return;
        }

        String url = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String roleJson = """
        [{
            "id": "%s",
            "name": "%s"
        }]
        """.formatted(roleId, role);

        HttpEntity<String> entity = new HttpEntity<>(roleJson, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            System.out.println("✅ Role '" + role + "' assigned to user: " + username);
        } catch (Exception e) {
            System.err.println("❌ Error assigning role: " + e.getMessage());
        }
    }

    // 🔹 Get Role ID by role name
    private String getRoleId(String roleName, String adminToken) {
        try {
            String url = keycloakServerUrl + "/admin/realms/" + realm + "/roles/" + roleName;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null) {
                return response.getBody().get("id").toString();
            }
        } catch (Exception e) {
            System.err.println("❌ Error retrieving role ID: " + e.getMessage());
        }
        return null;
    }

    // 🔹 Get User ID by username
    private String getUserId(String username, String adminToken) {
        try {
            String url = keycloakServerUrl + "/admin/realms/" + realm + "/users?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map[].class);
            if (response.getBody() != null && response.getBody().length > 0) {
                return response.getBody()[0].get("id").toString();
            }
        } catch (Exception e) {
            System.err.println("❌ Error retrieving user ID: " + e.getMessage());
        }
        return null;
    }

    // 🔹 User Login
    public ResponseEntity<?> login(String username, String password) {
        String url = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&grant_type=password" +
                "&username=" + username +
                "&password=" + password;

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return response.getStatusCode().is2xxSuccessful() ? ResponseEntity.ok(response.getBody()) : ResponseEntity.status(response.getStatusCode()).body("❌ Login failed!");
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body("❌ Error: " + e.getMessage());
        }
    }

    // 🔹 Get Keycloak Admin Token
    private String getServiceAccountAccessToken() {
        String url = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&grant_type=client_credentials"; // 🔹 Use client_credentials grant

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return response.getBody().get("access_token").toString();
            }
            throw new RuntimeException("Service account authentication failed.");
        } catch (Exception e) {
            logger.error("❌ Failed to fetch Keycloak service account token: {}", e.getMessage());
            throw new RuntimeException("Service account authentication failed.");
        }
    }


    // 🔹 User Logout (Now correctly takes refreshToken, accessToken, and userId)
    public ResponseEntity<?> logout(String refreshToken, String accessToken, String userId) {
        logger.info("🔒 Attempting to log out user with refresh token: {}", refreshToken);

        // ✅ Validate refresh token before proceeding
        if (!isRefreshTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Logout failed: Invalid refresh token. Please log in again.");
        }

        // 🔹 Send logout request to Keycloak
        String url = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&refresh_token=" + refreshToken;

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ User logged out successfully.");

                // 🔍 Verify if token is now invalid
                boolean tokenInvalid = !isRefreshTokenValid(refreshToken);
                if (tokenInvalid) {
                    logger.info("✅ Token successfully invalidated after logout.");
                } else {
                    logger.warn("⚠️ Token is still valid after logout. Investigate Keycloak configuration.");
                }

                // 🚀 Verify access token is invalid
                boolean accessTokenStillWorks = isAccessTokenValid(accessToken);
                if (!accessTokenStillWorks) {
                    logger.info("✅ Access token no longer works. Logout confirmed.");
                } else {
                    logger.warn("⚠️ Access token is still working after logout. Check Keycloak settings.");
                }

                // 🛠 Verify if user sessions still exist
                boolean noActiveSessions = checkUserSessions(userId);
                if (noActiveSessions) {
                    logger.info("✅ No active sessions found. Logout was successful.");
                } else {
                    logger.warn("⚠️ User still has active sessions. Logout may not have worked fully.");
                }

                return ResponseEntity.ok("✅ User logged out successfully.");
            } else {
                logger.error("❌ Keycloak Logout Error: {}", response.getBody());
                return ResponseEntity.status(response.getStatusCode()).body("❌ Logout failed: " + response.getBody());
            }
        } catch (HttpClientErrorException e) {
            logger.error("❌ HTTP Client Error during logout: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("❌ Logout failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Unexpected Error during logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Unexpected error: " + e.getMessage());
        }
    }

    // 🔹 Validate Refresh Token (Check if token is still valid)
    private boolean isRefreshTokenValid(String refreshToken) {
        String url = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&token=" + refreshToken;

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody().get("active"));
        } catch (Exception e) {
            return false;
        }
    }

    // 🔹 Check Active Sessions for a User
    private boolean checkUserSessions(String userId) {
        String adminAccessToken = getServiceAccountAccessToken();
        String url = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/sessions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            return response.getBody() == null || response.getBody().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    // 🔹 Validate Access Token (Check if it's still active)
    private boolean isAccessTokenValid(String accessToken) {
        String url = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&token=" + accessToken;

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody().get("active"));
        } catch (Exception e) {
            return false;
        }
    }

}
