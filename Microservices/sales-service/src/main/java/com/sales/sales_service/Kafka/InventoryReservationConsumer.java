package com.sales.sales_service.Kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sales.sales_service.Entity.OrderStatus;
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
    private final ObjectMapper mapper;

    @KafkaListener(
            topics = "${application.kafka.topics.stock-reserved:stock-reserved-topic}",
            groupId = "${spring.kafka.consumer.group-id:sales-group}"
    )
    public void consumeReserved(String message) {
        update(message, OrderStatus.STOCK_RESERVED, "Inventory reserved stock for this order");
    }

    @KafkaListener(
            topics = "${application.kafka.topics.stock-rejected:stock-rejected-topic}",
            groupId = "${spring.kafka.consumer.group-id:sales-group}"
    )
    public void consumeRejected(String message) {
        update(message, OrderStatus.REJECTED, "Inventory rejected the order because stock is unavailable");
    }

    private void update(String message, OrderStatus status, String remarks) {
        try {
            JsonNode event = mapper.readTree(message);
            String orderNumber = event.path("orderNumber").asText(event.path("orderId").asText(""));
            if (orderNumber.isBlank()) {
                log.warn("Ignoring inventory event without order number: {}", message);
                return;
            }
            orderService.updateStatusFromInventory(orderNumber, status, remarks);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to process inventory reservation event", ex);
        }
    }
}
