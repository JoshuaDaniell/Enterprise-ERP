package com.sales.sales_service.Dto;

import com.sales.sales_service.Entity.OrderStatus;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String remarks;
    private String changedBy;
    private Instant changedAt;
}
