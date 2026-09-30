package com.finance.finance_service.Kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finance_service.Dto.CreateInvoiceRequest;
import com.finance.finance_service.Dto.InvoiceItemDto;
import com.finance.finance_service.Dto.StockReservedEvent;
import com.finance.finance_service.Entity.ProcessedEvent;
import com.finance.finance_service.Repository.ProcessedEventRepository;
import com.finance.finance_service.Service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockReservedConsumer {

    private final ProcessedEventRepository events;
    private final InvoiceService invoices;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "${application.kafka.topics.stock-reserved:stock-reserved-topic}", groupId = "${spring.kafka.consumer.group-id:finance-service}")
    @Transactional
    public void consume(String message) {
        try {
            StockReservedEvent event = mapper.readValue(message, StockReservedEvent.class);
            String eventId = event.getEventId();
            if (eventId == null || eventId.isBlank() || !"STOCK_RESERVED".equals(event.getEventType())
                    || event.getOccurredAt() == null) {
                throw new IllegalArgumentException("Malformed STOCK_RESERVED event contract");
            }
            if (events.existsByEventId(eventId)) {
                log.info("Stock reserved event '{}' already processed. Skipping.", eventId);
                return;
            }

            String orderNumber = event.getOrderNumber() != null ? event.getOrderNumber()
                    : event.getOrderId() != null ? event.getOrderId().toString() : "";
            if (orderNumber.isBlank()) {
                throw new IllegalArgumentException("STOCK_RESERVED event is missing order number");
            }

            if (!invoices.existsForOrder(orderNumber)) {
                createInvoiceFromEvent(event, orderNumber);
            }

            events.save(ProcessedEvent.builder().eventId(eventId).build());
        } catch (Exception e) {
            log.error("Unable to process stock-reserved event: {}", e.getMessage(), e);
            throw new IllegalStateException("Unable to process stock-reserved event", e);
        }
    }

    private void createInvoiceFromEvent(StockReservedEvent event, String orderNumber) {
        try {
            BigDecimal total = event.getTotalAmount();
            var items = new ArrayList<InvoiceItemDto>();
            if (event.getItems() != null) {
                for (StockReservedEvent.StockReservedItemDto item : event.getItems()) {
                    items.add(new InvoiceItemDto(
                            item.getProductSku() != null ? item.getProductSku() : "ORDER",
                            item.getProductName() != null ? item.getProductName() : "Order item",
                            item.getQuantity() != null ? Math.max(1, item.getQuantity()) : 1,
                            item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO
                    ));
                }
            }
            if (items.isEmpty()) {
                items.add(new InvoiceItemDto("ORDER", "Order " + orderNumber, 1, total != null ? total : BigDecimal.ZERO));
            }
            invoices.create(new CreateInvoiceRequest(
                    orderNumber,
                    event.getCustomerId(),
                    event.getCustomerName(),
                    event.getCustomerEmail(),
                    items,
                    total
            ));
        } catch (Exception e) {
            log.error("Failed to generate invoice from stock event for order {}: {}", orderNumber, e.getMessage(), e);
            throw new IllegalStateException("Failed to generate invoice from stock event", e);
        }
    }
}
