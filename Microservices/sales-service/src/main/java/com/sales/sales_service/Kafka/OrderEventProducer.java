package com.sales.sales_service.Kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sales.sales_service.Dto.OrderPlacedEvent;
import com.sales.sales_service.Dto.OrderStatusChangedEvent;
import com.sales.sales_service.Entity.OutboxEvent;
import com.sales.sales_service.Entity.OutboxStatus;
import com.sales.sales_service.Repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Transactional
    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Persisting ORDER_PLACED outbox event for orderNumber '{}'", event.getOrderNumber());
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(event.getEventId())
                    .eventType("ORDER_PLACED")
                    .aggregateType("ORDER")
                    .aggregateId(event.getOrderNumber())
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Persisted ORDER_PLACED outbox event '{}' for orderNumber '{}'", event.getEventId(), event.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to persist ORDER_PLACED outbox event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to persist outbox event", e);
        }
    }

    @Transactional
    public void publishOrderStatusChanged(String orderNumber, String status) {
        String eventId = UUID.randomUUID().toString();
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .eventId(eventId)
                .eventType("ORDER_STATUS_CHANGED")
                .occurredAt(Instant.now())
                .orderNumber(orderNumber)
                .status(status)
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .eventType("ORDER_STATUS_CHANGED")
                    .aggregateType("ORDER")
                    .aggregateId(orderNumber)
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Persisted ORDER_STATUS_CHANGED outbox event '{}' for orderNumber '{}' with status '{}'",
                    eventId, orderNumber, status);
        } catch (Exception e) {
            log.error("Failed to persist ORDER_STATUS_CHANGED outbox event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to persist outbox event", e);
        }
    }
}
