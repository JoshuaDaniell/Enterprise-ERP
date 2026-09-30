package com.finance.finance_service.Kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finance_service.Entity.ProcessedEvent;
import com.finance.finance_service.Dto.OrderStatusChangedEvent;
import com.finance.finance_service.Repository.ProcessedEventRepository;
import com.finance.finance_service.Service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusChangedConsumer {

    private final InvoiceService invoiceService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper mapper;

    @KafkaListener(
            topics = "${application.kafka.topics.order-status-changed:order-status-changed-topic}",
            groupId = "${spring.kafka.consumer.group-id:finance-service}"
    )
    @Transactional
    public void consume(String message) {
        try {
            OrderStatusChangedEvent event = mapper.readValue(message, OrderStatusChangedEvent.class);
            String eventId = event.getEventId();
            if (eventId == null || eventId.isBlank() || !"ORDER_STATUS_CHANGED".equals(event.getEventType())
                    || event.getOccurredAt() == null) {
                throw new IllegalArgumentException("Malformed ORDER_STATUS_CHANGED event contract");
            }
            if (processedEventRepository.existsByEventId(eventId)) {
                log.info("Order status changed event '{}' already processed. Skipping.", eventId);
                return;
            }

            String orderNumber = event.getOrderNumber() != null ? event.getOrderNumber() : "";
            String status = event.getStatus() != null ? event.getStatus() : "";
            if (orderNumber.isBlank() || status.isBlank()) {
                throw new IllegalArgumentException("Order status event is missing orderNumber or status");
            }
            invoiceService.syncPaymentStatus(orderNumber, status);

            processedEventRepository.save(ProcessedEvent.builder().eventId(eventId).build());
            log.info("Synchronized invoice payment for order '{}' to sales status '{}'", orderNumber, status);
        } catch (Exception ex) {
            log.error("Unable to process order status event: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to process order status event", ex);
        }
    }
}
