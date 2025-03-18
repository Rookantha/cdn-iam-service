package com.contentnexus.iam.service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakRoleConverter.class);

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Extract roles from "realm_access"
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?>) {
            List<String> roles = (List<String>) realmAccess.get("roles");

            // Log extracted roles
            logger.info("🔍 Extracted Keycloak Roles: {}", roles);

            authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())) // Prefix with "ROLE_"
                    .collect(Collectors.toSet()));
        }

        logger.info("✅ Granted Authorities: {}", authorities);
        return authorities;
    }
}
