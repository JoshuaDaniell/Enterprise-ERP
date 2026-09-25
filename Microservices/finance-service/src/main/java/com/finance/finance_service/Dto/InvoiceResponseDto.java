package com.finance.finance_service.Dto;
import com.finance.finance_service.Entity.*; import java.math.BigDecimal; import java.time.Instant; import java.util.List;
public record InvoiceResponseDto(Long id,String invoiceNumber,String orderNumber,Long customerId,String customerName,String customerEmail,
 BigDecimal subtotal,BigDecimal taxAmount,BigDecimal totalAmount,InvoiceStatus status,PaymentStatus paymentStatus,Instant issuedAt,Instant paidAt,Instant refundedAt,List<InvoiceItemDto> items) {}
