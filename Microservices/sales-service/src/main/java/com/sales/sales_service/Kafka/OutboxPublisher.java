package com.sales.sales_service.Kafka;

import com.sales.sales_service.Entity.OutboxEvent;
import com.sales.sales_service.Entity.OutboxStatus;
import com.sales.sales_service.Repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${application.kafka.topics.order-placed:order-placed-topic}")
    private String orderPlacedTopic;

    @Value("${application.kafka.topics.order-status-changed:order-status-changed-topic}")
    private String orderStatusChangedTopic;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEvent event : pending) {
            String topic = resolveTopic(event.getEventType());
            if (topic == null) {
                log.error("Unknown topic for event type: {}", event.getEventType());
                event.setStatus(OutboxStatus.FAILED);
                event.setLastError("Unknown event type: " + event.getEventType());
                outboxEventRepository.save(event);
                continue;
            }

            try {
                // Key = aggregateId (orderNumber) for partition ordering
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get(5, TimeUnit.SECONDS);
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
                log.info("Successfully published sales outbox event '{}' [{}] to topic '{}' with key '{}'",
                        event.getEventId(), event.getEventType(), topic, event.getAggregateId());
            } catch (Exception ex) {
                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);
                event.setLastError(ex.getMessage() != null ? ex.getMessage() : ex.toString());
                outboxEventRepository.save(event);
                log.warn("Failed to publish sales outbox event '{}' (retry={}): {}", event.getEventId(), retries, ex.getMessage());
            }
        }
    }

    private String resolveTopic(String eventType) {
        if ("ORDER_PLACED".equalsIgnoreCase(eventType)) return orderPlacedTopic;
        if ("ORDER_STATUS_CHANGED".equalsIgnoreCase(eventType)) return orderStatusChangedTopic;
        return null;
    }
}
