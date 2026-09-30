package com.inventory.inventory_service.Dto;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRejectedEvent implements Serializable {

    private String eventId;
    private String eventType; // "STOCK_REJECTED"
    private Instant occurredAt;
    private Long orderId;
    private String orderNumber;
    private String reason;
}
