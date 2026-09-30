package com.sales.sales_service.Dto;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusChangedEvent implements Serializable {

    private String eventId;
    private String eventType; // "ORDER_STATUS_CHANGED"
    private Instant occurredAt;
    private String orderNumber;
    private String status;
}
