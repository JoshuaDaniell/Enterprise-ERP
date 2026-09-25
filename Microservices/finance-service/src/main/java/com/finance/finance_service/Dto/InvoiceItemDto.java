package com.finance.finance_service.Dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record InvoiceItemDto(@NotBlank String productSku, String productName, @NotNull @Positive Integer quantity, @NotNull @PositiveOrZero BigDecimal unitPrice) {}
