package com.inventory.inventory_service.Dto;

import com.inventory.inventory_service.Entity.Role;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDto {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean enabled;
    private Instant createdAt;
}
