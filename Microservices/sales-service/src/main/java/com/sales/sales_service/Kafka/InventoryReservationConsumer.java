package com.sales.sales_service.Kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sales.sales_service.Dto.InventoryReservationResultEvent;
import com.sales.sales_service.Entity.OrderStatus;
import com.sales.sales_service.Repository.ProcessedEventRepository;
import com.sales.sales_service.Service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationConsumer {

    private final OrderService orderService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper mapper;

    @KafkaListener(
            topics = "${application.kafka.topics.stock-reserved:stock-reserved-topic}",
            groupId = "${spring.kafka.consumer.group-id:sales-group}"
    )
    public void consumeReserved(String message) {
        update(message, "STOCK_RESERVED", OrderStatus.CONFIRMED, "Stock reserved by Inventory. Order automatically CONFIRMED.");
    }

    @KafkaListener(
            topics = "${application.kafka.topics.stock-rejected:stock-rejected-topic}",
            groupId = "${spring.kafka.consumer.group-id:sales-group}"
    )
    public void consumeRejected(String message) {
        update(message, "STOCK_REJECTED", OrderStatus.REJECTED, "Stock unavailable in Inventory. Order REJECTED.");
    }

    private void update(String message, String expectedEventType, OrderStatus status, String remarks) {
        try {
            InventoryReservationResultEvent event = mapper.readValue(message, InventoryReservationResultEvent.class);
            String eventId = event.getEventId();
            if (eventId == null || eventId.isBlank() || !expectedEventType.equals(event.getEventType())
                    || event.getOccurredAt() == null) {
                throw new IllegalArgumentException("Malformed " + expectedEventType + " event contract");
            }
            if (processedEventRepository.existsByEventId(eventId)) {
                log.info("Inventory event '{}' already processed. Skipping duplicate.", eventId);
                return;
            }

            String orderNumber = event.getOrderNumber() != null ? event.getOrderNumber()
                    : event.getOrderId() != null ? event.getOrderId().toString() : "";
            if (orderNumber.isBlank()) {
                throw new IllegalArgumentException("Inventory event is missing orderNumber");
            }

            String reason = event.getReason() != null ? event.getReason() : "";
            String detailedRemarks = !reason.isBlank() ? remarks + " Reason: " + reason : remarks;

            orderService.updateStatusFromInventory(orderNumber, status, detailedRemarks, eventId);
        } catch (Exception ex) {
            log.error("Unable to process inventory reservation event: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to process inventory reservation event", ex);
        }
    }
}
