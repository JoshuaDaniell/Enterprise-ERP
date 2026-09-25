package com.inventory.inventory_service.Dto;

import com.inventory.inventory_service.Entity.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private String username;
    private String email;
    private Role role;

    // Alias for accessToken for backward compatibility
    public String getToken() {
        return accessToken != null ? accessToken : "";
    }
}
