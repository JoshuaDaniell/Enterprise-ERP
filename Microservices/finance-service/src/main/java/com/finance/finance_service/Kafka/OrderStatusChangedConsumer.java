package com.finance.finance_service.Kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finance_service.Service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusChangedConsumer {

    private final InvoiceService invoiceService;
    private final ObjectMapper mapper;

    @KafkaListener(
            topics = "${application.kafka.topics.order-status-changed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        try {
            JsonNode event = mapper.readTree(message);
            String orderNumber = event.path("orderNumber").asText("");
            String status = event.path("status").asText("");
            if (orderNumber.isBlank() || status.isBlank()) {
                log.warn("Ignoring order status event without orderNumber or status: {}", message);
                return;
            }
            invoiceService.syncPaymentStatus(orderNumber, status);
            log.info("Synchronized invoice payment for order '{}' to sales status '{}'", orderNumber, status);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to process order status event", ex);
        }
    }
}
