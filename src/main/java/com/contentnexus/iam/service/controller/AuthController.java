package com.contentnexus.iam.service.controller;

import com.contentnexus.iam.service.model.LoginRequest;
import com.contentnexus.iam.service.model.User;
import com.contentnexus.iam.service.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "APIs for user authentication and authorization")
public class AuthController {
    private final KeycloakService keycloakService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user in the Keycloak authentication system.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User registered successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid user data", content = @Content),
                    @ApiResponse(responseCode = "409", description = "User already exists", content = @Content)
            }
    )
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User request) {
        return keycloakService.registerUser(request);
    }

    @Operation(
            summary = "Login",
            description = "Authenticates a user and returns an access token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login successful"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
            }
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        MDC.put("username", loginRequest.getUsername());
        logger.info("🔐 User attempting to log in: {}", loginRequest.getUsername());

        ResponseEntity<?> response = keycloakService.login(loginRequest.getUsername(), loginRequest.getPassword());

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("✅ Login successful for user: {}", loginRequest.getUsername());
        } else {
            logger.warn("⚠️ Login failed for user: {}", loginRequest.getUsername());
        }

        MDC.clear();
        return response;
    }

    @Operation(
            summary = "Get user info",
            description = "Returns the authenticated user's information.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User info retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
            }
    )
    @GetMapping("/userinfo")
    public ResponseEntity<?> getUserInfo(Authentication authentication) {
        if (authentication == null) {
            logger.warn("🚫 Unauthorized access attempt to /auth/userinfo");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        logger.info("🔑 User Authorities: {}", authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(authentication.getPrincipal());
    }

    @Operation(
            summary = "Logout",
            description = "Logs out the user and revokes the access token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Logout successful"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
            }
    )
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @Parameter(description = "Access Token") @RequestHeader("Authorization") String accessToken,
            @Parameter(description = "Refresh Token") @RequestHeader("Refresh-Token") String refreshToken,
            @Parameter(description = "User ID") @RequestHeader("User-Id") String userId
    ) {
        logger.info("🔓 Logout request received for User ID: {}", userId);
        logger.info("🛂 Received Authorization Header (Access Token): {}", accessToken);
        logger.info("🔄 Received Refresh Token: {}", refreshToken);

        System.out.println("Logout method reached!");  // Debugging line
        return keycloakService.logout(refreshToken, accessToken, userId);
    }
}
