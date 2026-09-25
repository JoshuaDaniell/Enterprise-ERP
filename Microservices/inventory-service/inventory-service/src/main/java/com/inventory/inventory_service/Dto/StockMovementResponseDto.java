package com.inventory.inventory_service.Dto;

import com.inventory.inventory_service.Entity.StockMovement;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementResponseDto {

    private Long id;
    private Long productId;
    private String sku;
    private Integer quantityChange;
    private Integer quantityAfter;
    private StockMovement.MovementType movementType;
    private String reason;
    private Instant createdAt;
}
