package com.sales.sales_service.Dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPlacedEvent implements Serializable {

    private String eventId;
    private String eventType; // "ORDER_PLACED"
    private Instant occurredAt;
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private List<OrderItemEventDto> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemEventDto implements Serializable {
        private String productSku;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
