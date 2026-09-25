package com.finance.finance_service.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="invoices", indexes=@Index(name="uk_invoice_order", columnList="orderNumber", unique=true))
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class Invoice {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false, unique=true, length=40) private String invoiceNumber;
 @Column(nullable=false, length=80) private String orderNumber;
 private Long customerId; private String customerName; private String customerEmail;
 @Column(nullable=false, precision=19, scale=4) private BigDecimal subtotal;
 @Column(nullable=false, precision=19, scale=4) private BigDecimal taxAmount;
 @Column(nullable=false, precision=19, scale=4) private BigDecimal totalAmount;
 @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private InvoiceStatus status;
 @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private PaymentStatus paymentStatus;
 @Column(nullable=false) private Instant issuedAt; private Instant paidAt; private Instant refundedAt;
 @OneToMany(mappedBy="invoice", cascade=CascadeType.ALL, orphanRemoval=true) @Builder.Default
 private List<InvoiceItem> items = new ArrayList<>();
 public void addItem(InvoiceItem item) { items.add(item); item.setInvoice(this); }
}
