package com.sales.sales_service.Dto;

import com.sales.sales_service.Entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusDto {

    @NotNull(message = "New order status is required")
    private OrderStatus status;

    private String remarks;
}
