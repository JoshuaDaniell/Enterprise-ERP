package com.inventory.inventory_service.Kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventory.inventory_service.Dto.StockAdjustmentDto;
import com.inventory.inventory_service.Dto.StockReservedEvent;
import com.inventory.inventory_service.Dto.StockRejectedEvent;
import com.inventory.inventory_service.Entity.OutboxEvent;
import com.inventory.inventory_service.Entity.OutboxStatus;
import com.inventory.inventory_service.Entity.ProcessedEvent;
import com.inventory.inventory_service.Repository.OutboxEventRepository;
import com.inventory.inventory_service.Repository.ProcessedEventRepository;
import com.inventory.inventory_service.Repository.ProductRepository;
import com.inventory.inventory_service.Service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedKafkaConsumer {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @KafkaListener(topics = "${application.kafka.topics.order-placed:order-placed-topic}", groupId = "${spring.kafka.consumer.group-id:inventory-group}")
    @Transactional
    public void consumeOrderPlacedEvent(String message) {
        log.info("Received Kafka event on 'order-placed-topic': {}", message);
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventId = root.path("eventId").asText("");
            String orderNumber = root.path("orderNumber").asText("UNKNOWN");

            if (!"ORDER_PLACED".equals(root.path("eventType").asText())
                    || root.path("occurredAt").isMissingNode()
                    || eventId.isBlank() || "UNKNOWN".equals(orderNumber)) {
                throw new IllegalArgumentException("ORDER_PLACED must include eventId, eventType, occurredAt, and orderNumber");
            }

            if (processedEventRepository.existsByEventId(eventId)) {
                log.info("Event ID {} already processed. Skipping to maintain idempotency.", eventId);
                return;
            }

            JsonNode items = root.path("items");
            boolean allSuccessful = items.isArray() && items.size() > 0;
            String rejectionReason = null;
            Map<String, Integer> requestedBySku = new LinkedHashMap<>();

            if (allSuccessful) {
                for (JsonNode item : items) {
                    String sku = item.path("productSku").asText();
                    int quantity = item.path("quantity").asInt(1);
                    if (sku.isBlank() || quantity <= 0) {
                        allSuccessful = false;
                        rejectionReason = "Invalid product or quantity in order";
                        break;
                    }
                    requestedBySku.merge(sku, quantity, Integer::sum);
                }
            }

            if (allSuccessful) {
                for (Map.Entry<String, Integer> request : requestedBySku.entrySet()) {
                    String sku = request.getKey();
                    int quantity = request.getValue();
                    var product = productRepository.findBySku(sku).orElse(null);
                    if (product == null) {
                        allSuccessful = false;
                        rejectionReason = "Product not found: " + sku;
                        break;
                    }
                    if (product.getQuantity() < quantity) {
                        allSuccessful = false;
                        rejectionReason = "Insufficient stock for " + sku
                                + ". Available: " + product.getQuantity()
                                + ", requested: " + quantity;
                        break;
                    }
                }
            }

            if (allSuccessful) {
                for (Map.Entry<String, Integer> request : requestedBySku.entrySet()) {
                    String sku = request.getKey();
                    int quantity = request.getValue();
                    var product = productRepository.findBySku(sku).orElseThrow();
                    productService.adjustStock(product.getId(), StockAdjustmentDto.builder()
                            .amount(-quantity)
                            .reason("Kafka Order Reservation: " + orderNumber)
                            .build());
                }
            }

            if (allSuccessful) {
                String reservationEventId = UUID.randomUUID().toString();
                StockReservedEvent event = objectMapper.treeToValue(root, StockReservedEvent.class);
                event.setEventId(reservationEventId);
                event.setEventType("STOCK_RESERVED");
                event.setOccurredAt(Instant.now());

                outboxEventRepository.save(OutboxEvent.builder()
                        .eventId(reservationEventId)
                        .eventType("STOCK_RESERVED")
                        .aggregateType("ORDER")
                        .aggregateId(orderNumber)
                        .payload(objectMapper.writeValueAsString(event))
                        .status(OutboxStatus.PENDING)
                        .build());
                log.info("Persisted STOCK_RESERVED outbox event for order '{}'", orderNumber);
            } else {
                String rejectionEventId = UUID.randomUUID().toString();
                StockRejectedEvent rejectionEvent = StockRejectedEvent.builder()
                        .eventId(rejectionEventId)
                        .eventType("STOCK_REJECTED")
                        .occurredAt(Instant.now())
                        .orderId(root.path("orderId").asLong(0))
                        .orderNumber(orderNumber)
                        .reason(rejectionReason != null ? rejectionReason : "Insufficient stock or invalid items")
                        .build();

                outboxEventRepository.save(OutboxEvent.builder()
                        .eventId(rejectionEventId)
                        .eventType("STOCK_REJECTED")
                        .aggregateType("ORDER")
                        .aggregateId(orderNumber)
                        .payload(objectMapper.writeValueAsString(rejectionEvent))
                        .status(OutboxStatus.PENDING)
                        .build());
                log.info("Persisted STOCK_REJECTED outbox event for order '{}': {}", orderNumber, rejectionReason);
            }

            // Save processed event in the same transaction
            processedEventRepository.save(ProcessedEvent.builder().eventId(eventId).build());

        } catch (Exception e) {
            log.error("Error processing ORDER_PLACED Kafka message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process message", e); // Throwing lets Kafka know it failed (if we want retry)
        }
    }
}
