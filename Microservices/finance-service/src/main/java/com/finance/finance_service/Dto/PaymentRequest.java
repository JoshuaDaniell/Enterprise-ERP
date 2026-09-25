package com.finance.finance_service.Dto;
import jakarta.validation.constraints.NotBlank;
public record PaymentRequest(@NotBlank String paymentReference) {}
