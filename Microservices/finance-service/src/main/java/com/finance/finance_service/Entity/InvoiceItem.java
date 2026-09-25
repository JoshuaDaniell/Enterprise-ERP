package com.finance.finance_service.Entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="invoice_items") @Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class InvoiceItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="invoice_id") private Invoice invoice;
 @Column(nullable=false) private String productSku; private String productName; private Integer quantity;
 @Column(nullable=false, precision=19, scale=4) private BigDecimal unitPrice;
 @Column(nullable=false, precision=19, scale=4) private BigDecimal subtotal;
}
