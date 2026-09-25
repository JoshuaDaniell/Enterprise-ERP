package com.finance.finance_service.Controller;
import com.finance.finance_service.Dto.*; import com.finance.finance_service.Service.*; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/finance/invoices") @RequiredArgsConstructor
public class InvoiceController {
 private final InvoiceService service; private final InvoicePdfService pdf;
 @GetMapping public List<InvoiceResponseDto> list(){return service.list();}
 @GetMapping("/dashboard") public Map<String,java.math.BigDecimal> dashboard(){return service.dashboard();}
 @PostMapping public ResponseEntity<InvoiceResponseDto> create(@Valid @RequestBody CreateInvoiceRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));}
 @GetMapping("/{invoiceNumber}") public InvoiceResponseDto get(@PathVariable String invoiceNumber){return service.get(invoiceNumber);}
 @GetMapping("/{invoiceNumber}/pdf") public ResponseEntity<byte[]> pdf(@PathVariable String invoiceNumber){var i=service.getEntity(invoiceNumber);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+i.getInvoiceNumber()+".pdf").body(pdf.generate(i));}
 @PostMapping("/{invoiceNumber}/payment") public InvoiceResponseDto pay(@PathVariable String invoiceNumber,@Valid @RequestBody PaymentRequest r){return service.markPaid(invoiceNumber,r.paymentReference());}
 @PostMapping("/{invoiceNumber}/refund") public InvoiceResponseDto refund(@PathVariable String invoiceNumber,@Valid @RequestBody PaymentRequest r){return service.refund(invoiceNumber,r.paymentReference());}
}
