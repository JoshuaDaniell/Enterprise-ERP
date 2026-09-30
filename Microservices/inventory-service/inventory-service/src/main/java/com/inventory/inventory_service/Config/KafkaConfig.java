package com.inventory.inventory_service.Config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.apache.kafka.common.TopicPartition;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Value("${application.kafka.topics.stock-reserved:stock-reserved-topic}")
    private String stockReservedTopic;

    @Value("${application.kafka.topics.stock-rejected:stock-rejected-topic}")
    private String stockRejectedTopic;

    @Value("${application.kafka.topics.stock-low:stock-low-topic}")
    private String stockLowTopic;

    @Value("${application.kafka.topics.order-placed:order-placed-topic}")
    private String orderPlacedTopic;

    @Bean
    public NewTopic stockReservedTopic() {
        return TopicBuilder.name(stockReservedTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days retention
                .build();
    }

    @Bean
    public NewTopic stockRejectedTopic() {
        return TopicBuilder.name(stockRejectedTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic stockLowTopic() {
        return TopicBuilder.name(stockLowTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic orderPlacedDeadLetterTopic() {
        return TopicBuilder.name(orderPlacedTopic + ".DLT").partitions(3).replicas(1)
                .config("retention.ms", "1209600000").build();
    }

    @Bean
    public NewTopic stockReservedDeadLetterTopic() {
        return TopicBuilder.name(stockReservedTopic + ".DLT").partitions(3).replicas(1)
                .config("retention.ms", "1209600000").build();
    }

    @Bean
    public NewTopic stockRejectedDeadLetterTopic() {
        return TopicBuilder.name(stockRejectedTopic + ".DLT").partitions(3).replicas(1)
                .config("retention.ms", "1209600000").build();
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
