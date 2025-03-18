package com.contentnexus.iam.service.controller;

import com.contentnexus.iam.service.model.LoginRequest;
import com.contentnexus.iam.service.model.User;
import com.contentnexus.iam.service.service.KeycloakService;
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
public class AuthController {
    private final KeycloakService keycloakService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User request) {
        return keycloakService.registerUser(request);
    }

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

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("Authorization") String accessToken,
            @RequestHeader("Refresh-Token") String refreshToken,
            @RequestHeader("User-Id") String userId
    ) {
        logger.info("🔓 Logout request received for User ID: {}", userId);
        logger.info("🛂 Received Authorization Header (Access Token): {}", accessToken);
        logger.info("🔄 Received Refresh Token: {}", refreshToken);

        System.out.println("Logout method reached!");  // Debugging line
        return keycloakService.logout(refreshToken, accessToken, userId);
    }
}
