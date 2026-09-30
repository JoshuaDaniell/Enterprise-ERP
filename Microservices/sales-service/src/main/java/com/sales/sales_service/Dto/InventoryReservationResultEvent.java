package com.sales.sales_service.Dto;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservationResultEvent implements Serializable {
    private String eventId;
    private String eventType;
    private Instant occurredAt;
    private Long orderId;
    private String orderNumber;
    private String reason;
}
