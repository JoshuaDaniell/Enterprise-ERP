package com.sales.sales_service.Kafka;

import com.sales.sales_service.Dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${application.kafka.topics.order-placed:order-placed-topic}")
    private String orderPlacedTopic;

    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Publishing ORDER_PLACED event for orderNumber '{}' to topic '{}'", event.getOrderNumber(), orderPlacedTopic);
        try {
            kafkaTemplate.send(orderPlacedTopic, event.getOrderNumber(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully published ORDER_PLACED event for orderNumber '{}' at offset {}",
                                    event.getOrderNumber(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish ORDER_PLACED event for orderNumber '{}': {}",
                                    event.getOrderNumber(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Exception while sending ORDER_PLACED event: {}", e.getMessage(), e);
        }
    }

    @Value("${application.kafka.topics.order-status-changed:order-status-changed-topic}")
    private String orderStatusChangedTopic;

    public void publishOrderStatusChanged(String orderNumber, String status) {
        Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "ORDER_STATUS_CHANGED",
                "orderNumber", orderNumber,
                "status", status
        );
        kafkaTemplate.send(orderStatusChangedTopic, orderNumber, event);
        log.info("Published ORDER_STATUS_CHANGED for orderNumber '{}' with status '{}'", orderNumber, status);
    }

}
