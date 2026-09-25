package com.finance.finance_service.Service;

import com.finance.finance_service.Dto.*; import com.finance.finance_service.Entity.*; import com.finance.finance_service.Exception.ResourceNotFoundException;
import com.finance.finance_service.Repository.*; import lombok.RequiredArgsConstructor; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.math.*; import java.time.Instant; import java.util.*; import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.finance.finance_service.Config.RedisConfig;

@Service @RequiredArgsConstructor
public class InvoiceService {
 private final InvoiceRepository invoices; private final RevenueLedgerRepository ledger;
 private final Map<String, String> pendingOrderStatuses = new ConcurrentHashMap<>();
 @Value("${finance.tax-rate:0.18}") private BigDecimal taxRate;
 @Transactional @CacheEvict(cacheNames = {RedisConfig.INVOICES, RedisConfig.DASHBOARD}, allEntries = true) public InvoiceResponseDto create(CreateInvoiceRequest request) {
  var existing=invoices.findByOrderNumber(request.orderNumber()); if(existing.isPresent()) return toDto(existing.get());
  BigDecimal subtotal=request.items().stream().map(i->i.unitPrice().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(4,RoundingMode.HALF_UP);
  if(request.subtotal()!=null && request.subtotal().compareTo(subtotal)!=0) throw new IllegalArgumentException("Subtotal does not match invoice items");
  BigDecimal tax=subtotal.multiply(taxRate).setScale(4,RoundingMode.HALF_UP);
  Invoice invoice=Invoice.builder().invoiceNumber("INV-"+UUID.randomUUID().toString().substring(0,8).toUpperCase()).orderNumber(request.orderNumber()).customerId(request.customerId()).customerName(request.customerName()).customerEmail(request.customerEmail()).subtotal(subtotal).taxAmount(tax).totalAmount(subtotal.add(tax)).status(InvoiceStatus.ISSUED).paymentStatus(PaymentStatus.PENDING).issuedAt(Instant.now()).build();
  final Invoice draft=invoice;
  request.items().forEach(i->draft.addItem(InvoiceItem.builder().productSku(i.productSku()).productName(i.productName()).quantity(i.quantity()).unitPrice(i.unitPrice()).subtotal(i.unitPrice().multiply(BigDecimal.valueOf(i.quantity()))).build()));
  String orderStatus = pendingOrderStatuses.remove(request.orderNumber());
  if ("CONFIRMED".equalsIgnoreCase(orderStatus)) {
   invoice.setPaymentStatus(PaymentStatus.PAID);
   invoice.setStatus(InvoiceStatus.PAID);
   invoice.setPaidAt(Instant.now());
  }
  invoice=invoices.save(invoice); postLedger(invoice,"INVOICE_ISSUED",invoice.getTotalAmount(), "invoice:"+invoice.getId()); return toDto(invoice);
 }
 @Transactional(readOnly=true) public InvoiceResponseDto get(String number){return toDto(find(number));}
 @Transactional @CacheEvict(cacheNames = {RedisConfig.INVOICES, RedisConfig.DASHBOARD}, allEntries = true) public void syncPaymentStatus(String orderNumber, String orderStatus) {
  var invoice = invoices.findByOrderNumber(orderNumber);
  if (invoice.isEmpty()) {
   pendingOrderStatuses.put(orderNumber, orderStatus);
   return;
  }
  invoice.ifPresent(invoiceEntity -> {
   boolean confirmed = "CONFIRMED".equalsIgnoreCase(orderStatus);
   PaymentStatus target = confirmed ? PaymentStatus.PAID : PaymentStatus.PENDING;
   if (invoiceEntity.getPaymentStatus() != target) {
    invoiceEntity.setPaymentStatus(target);
    invoiceEntity.setStatus(confirmed ? InvoiceStatus.PAID : InvoiceStatus.ISSUED);
    if (confirmed) invoiceEntity.setPaidAt(Instant.now());
    else invoiceEntity.setPaidAt(null);
    invoices.save(invoiceEntity);
   }
  });
 }
 @Transactional(readOnly=true) @Cacheable(cacheNames = RedisConfig.INVOICES) public List<InvoiceResponseDto> list(){return invoices.findAllByOrderByIssuedAtDesc().stream().map(this::toDto).toList();}
 @Transactional(readOnly=true) @Cacheable(cacheNames = RedisConfig.DASHBOARD) public Map<String,BigDecimal> dashboard(){
  BigDecimal revenue=invoices.sumTotalByPaymentStatus(PaymentStatus.PAID);
  BigDecimal outstanding=invoices.sumTotalByPaymentStatus(PaymentStatus.PENDING);
  return Map.of("totalRevenue",revenue,"outstandingAmount",outstanding);
 }
 @Transactional @CacheEvict(cacheNames = {RedisConfig.INVOICES, RedisConfig.DASHBOARD}, allEntries = true) public InvoiceResponseDto markPaid(String number,String reference){Invoice i=find(number); if(i.getPaymentStatus()==PaymentStatus.REFUNDED) throw new IllegalStateException("Refunded invoice cannot be paid"); if(i.getPaymentStatus()!=PaymentStatus.PAID){i.setPaymentStatus(PaymentStatus.PAID);i.setStatus(InvoiceStatus.PAID);i.setPaidAt(Instant.now());i=invoices.save(i);postLedger(i,"PAYMENT",i.getTotalAmount(),"payment:"+reference);} return toDto(i);}
 @Transactional @CacheEvict(cacheNames = {RedisConfig.INVOICES, RedisConfig.DASHBOARD}, allEntries = true) public InvoiceResponseDto refund(String number,String reference){Invoice i=find(number); if(i.getPaymentStatus()!=PaymentStatus.PAID) throw new IllegalStateException("Only paid invoices can be refunded"); i.setPaymentStatus(PaymentStatus.REFUNDED);i.setStatus(InvoiceStatus.REFUNDED);i.setRefundedAt(Instant.now());i=invoices.save(i);postLedger(i,"REFUND",i.getTotalAmount().negate(),"refund:"+reference);return toDto(i);}
 private Invoice find(String n){return invoices.findByInvoiceNumber(n).orElseThrow(()->new ResourceNotFoundException("Invoice not found: "+n));}
 private void postLedger(Invoice i,String type,BigDecimal amount,String ref){if(!ledger.existsByReference(ref))ledger.save(RevenueLedgerEntry.builder().invoiceId(i.getId()).entryType(type).amount(amount).postedAt(Instant.now()).reference(ref).build());}
 public InvoiceResponseDto toDto(Invoice i){return new InvoiceResponseDto(i.getId(),i.getInvoiceNumber(),i.getOrderNumber(),i.getCustomerId(),i.getCustomerName(),i.getCustomerEmail(),i.getSubtotal(),i.getTaxAmount(),i.getTotalAmount(),i.getStatus(),i.getPaymentStatus(),i.getIssuedAt(),i.getPaidAt(),i.getRefundedAt(),i.getItems().stream().map(x->new InvoiceItemDto(x.getProductSku(),x.getProductName(),x.getQuantity(),x.getUnitPrice())).collect(Collectors.toList()));}
 public Invoice getEntity(String n){return find(n);}
 public boolean existsForOrder(String n){return invoices.findByOrderNumber(n).isPresent();}
}
