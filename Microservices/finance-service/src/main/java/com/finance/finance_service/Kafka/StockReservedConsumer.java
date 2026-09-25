package com.finance.finance_service.Kafka;
import com.fasterxml.jackson.databind.*; import com.finance.finance_service.Dto.*; import com.finance.finance_service.Entity.ProcessedEvent; import com.finance.finance_service.Repository.ProcessedEventRepository; import com.finance.finance_service.Service.InvoiceService; import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j; import org.springframework.kafka.annotation.KafkaListener; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional; import java.math.BigDecimal; import java.util.*;
@Component @RequiredArgsConstructor @Slf4j
public class StockReservedConsumer {
 private final ProcessedEventRepository events; private final InvoiceService invoices; private final ObjectMapper mapper;
 @KafkaListener(topics="${application.kafka.topics.stock-reserved}", groupId="${spring.kafka.consumer.group-id}")
 @Transactional public void consume(String message) {
  try {JsonNode root=mapper.readTree(message);String eventId=root.path("eventId").asText("");if(eventId.isBlank()) {log.warn("Ignoring stock event without eventId");return;} if(events.existsByEventId(eventId)) return;
   String order=root.path("orderId").asText(root.path("orderNumber").asText("")); if(order.isBlank()) return;
   if(!invoices.existsForOrder(order)) create(message);
   events.save(ProcessedEvent.builder().eventId(eventId).build());
  } catch(Exception e){throw new IllegalStateException("Unable to process stock-reserved event",e);}
 }
 private void create(String message) {
  try {JsonNode r=mapper.readTree(message);String order=r.path("orderId").asText(r.path("orderNumber").asText());BigDecimal total=r.path("totalAmount").decimalValue();var items=new ArrayList<InvoiceItemDto>();JsonNode arr=r.path("items");if(arr.isArray())for(JsonNode x:arr)items.add(new InvoiceItemDto(x.path("productSku").asText("ORDER"),x.path("productName").asText("Order item"),Math.max(1,x.path("quantity").asInt(1)),x.path("unitPrice").decimalValue()));if(items.isEmpty())items.add(new InvoiceItemDto("ORDER", "Order",1,total));invoices.create(new CreateInvoiceRequest(order,r.path("customerId").isNumber()?r.path("customerId").asLong():null,r.path("customerName").asText(null),r.path("customerEmail").asText(null),items,total));}catch(Exception e){throw new IllegalStateException("Failed to generate invoice from stock event",e);}
 }
}
