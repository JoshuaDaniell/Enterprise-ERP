package com.finance.finance_service.Dto;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.util.List;
public record CreateInvoiceRequest(@NotBlank String orderNumber, Long customerId, String customerName, @Email String customerEmail,
 @NotEmpty List<@Valid InvoiceItemDto> items, @NotNull @PositiveOrZero BigDecimal subtotal) {}
