package com.multimodal.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    @Builder.Default
    private String type = "Bearer";
    private long expiresIn; // access token expiration in seconds
    private UserDTO user;
    private Set<String> roles;

    /** Convenience builder for refresh-only response (no user info needed) */
    public static AuthResponse refreshOnly(String token, String refreshToken, long expiresIn) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .type("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
