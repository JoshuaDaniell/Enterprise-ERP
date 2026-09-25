package com.sales.sales_service.Dto;

import com.sales.sales_service.Entity.OrderStatus;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String billingAddress;
    private String notes;
    private List<OrderItemResponseDto> items;
    private List<OrderStatusHistoryResponseDto> statusHistory;
    private Instant orderDate;
    private Instant updatedAt;
}
