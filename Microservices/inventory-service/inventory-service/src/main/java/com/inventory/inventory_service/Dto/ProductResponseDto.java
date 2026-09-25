package com.inventory.inventory_service.Dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String sku;
    private String name;
    private Integer quantity;
    private BigDecimal price;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
