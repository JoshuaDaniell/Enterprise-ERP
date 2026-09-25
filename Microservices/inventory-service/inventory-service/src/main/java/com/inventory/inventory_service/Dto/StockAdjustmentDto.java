package com.inventory.inventory_service.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentDto {

    @NotNull(message = "Adjustment amount is required")
    private Integer amount;

    private String reason;
}
