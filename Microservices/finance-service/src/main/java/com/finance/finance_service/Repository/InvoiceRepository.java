package com.finance.finance_service.Repository;
import com.finance.finance_service.Entity.Invoice; import com.finance.finance_service.Entity.PaymentStatus; import java.math.BigDecimal; import java.util.*; import org.springframework.data.jpa.repository.EntityGraph; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Query;
public interface InvoiceRepository extends JpaRepository<Invoice,Long> {
 Optional<Invoice> findByOrderNumber(String orderNumber);
 Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
 @EntityGraph(attributePaths = "items")
 List<Invoice> findAllByOrderByIssuedAtDesc();
 @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i where i.paymentStatus = :status")
 BigDecimal sumTotalByPaymentStatus(PaymentStatus status);
}
