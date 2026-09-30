package com.inventory.inventory_service.Kafka;

import com.inventory.inventory_service.Entity.OutboxEvent;
import com.inventory.inventory_service.Entity.OutboxStatus;
import com.inventory.inventory_service.Repository.OutboxEventRepository;
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
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${application.kafka.topics.stock-reserved:stock-reserved-topic}")
    private String stockReservedTopic;

    @Value("${application.kafka.topics.stock-rejected:stock-rejected-topic}")
    private String stockRejectedTopic;

    @Value("${application.kafka.topics.stock-low:stock-low-topic}")
    private String stockLowTopic;

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
                // Key = aggregateId (e.g. orderNumber) for partition ordering
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get(5, TimeUnit.SECONDS);
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
                log.info("Successfully published outbox event '{}' [{}] to topic '{}'",
                        event.getEventId(), event.getEventType(), topic);
            } catch (Exception ex) {
                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);
                event.setLastError(ex.getMessage() != null ? ex.getMessage() : ex.toString());
                outboxEventRepository.save(event);
                log.warn("Failed to publish outbox event '{}' (retry={}): {}", event.getEventId(), retries, ex.getMessage());
            }
        }
    }

    private String resolveTopic(String eventType) {
        if ("STOCK_RESERVED".equalsIgnoreCase(eventType)) return stockReservedTopic;
        if ("STOCK_REJECTED".equalsIgnoreCase(eventType)) return stockRejectedTopic;
        if ("STOCK_LOW".equalsIgnoreCase(eventType)) return stockLowTopic;
        return null;
    }
}
