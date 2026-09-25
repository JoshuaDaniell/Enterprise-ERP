package com.inventory.inventory_service.Kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${application.kafka.topics.stock-reserved:stock-reserved-topic}")
    private String stockReservedTopic;

    @Value("${application.kafka.topics.stock-rejected:stock-rejected-topic}")
    private String stockRejectedTopic;

    @Value("${application.kafka.topics.stock-low:stock-low-topic}")
    private String stockLowTopic;

    public void publishStockReserved(String orderId) {
        publishEvent(stockReservedTopic, orderId, "STOCK_RESERVED", Map.of("orderId", orderId));
    }

    public void publishStockReserved(JsonNode orderEvent) {
        Map<String, Object> event = objectMapper.convertValue(orderEvent, Map.class);
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "STOCK_RESERVED");
        event.put("occurredAt", Instant.now().toString());
        String key = String.valueOf(event.getOrDefault("orderNumber", event.getOrDefault("orderId", "unknown")));
        publishEvent(stockReservedTopic, key, event);
    }

    public void publishStockRejected(String orderId, String reason) {
        publishEvent(stockRejectedTopic, orderId, "STOCK_REJECTED",
                Map.of("orderId", orderId, "orderNumber", orderId, "reason", reason));
    }

    public void publishStockLow(String sku, int currentQuantity) {
        publishEvent(stockLowTopic, sku, "STOCK_LOW", Map.of("sku", sku, "quantity", currentQuantity));
    }

    private void publishEvent(String topic, String key, String eventType, Map<String, Object> additionalData) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("eventType", eventType);
            event.put("occurredAt", Instant.now().toString());
            event.putAll(additionalData);

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, message);
            log.info("Published {} event to topic '{}': {}", eventType, topic, message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish " + eventType + " event to topic '" + topic + "'", e);
        }
    }

    private void publishEvent(String topic, String key, Map<String, Object> event) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event));
            log.info("Published {} event to topic '{}'", event.get("eventType"), topic);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish event to topic '" + topic + "'", e);
        }
    }
}
