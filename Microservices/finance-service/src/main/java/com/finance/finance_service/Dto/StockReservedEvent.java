package com.finance.finance_service.Dto;

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
public class StockReservedEvent implements Serializable {

    private String eventId;
    private String eventType; // "STOCK_RESERVED"
    private Instant occurredAt;
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private List<StockReservedItemDto> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockReservedItemDto implements Serializable {
        private String productSku;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
