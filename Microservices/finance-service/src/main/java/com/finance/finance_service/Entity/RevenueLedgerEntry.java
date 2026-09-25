package com.finance.finance_service.Entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="revenue_ledger", indexes=@Index(name="idx_ledger_invoice", columnList="invoiceId"))
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class RevenueLedgerEntry {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long invoiceId; @Column(nullable=false, length=20) private String entryType;
 @Column(nullable=false, precision=19, scale=4) private BigDecimal amount; @Column(nullable=false) private Instant postedAt;
 @Column(nullable=false, unique=true, length=100) private String reference;
}
