package com.sales.sales_service.Kafka;

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
public class KafkaProducerConfig {

    @Value("${application.kafka.topics.order-placed:order-placed-topic}")
    private String orderPlacedTopic;

    @Value("${application.kafka.topics.order-status-changed:order-status-changed-topic}")
    private String orderStatusChangedTopic;

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name(orderPlacedTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days retention
                .build();
    }

    @Bean
    public NewTopic orderStatusChangedTopic() {
        return TopicBuilder.name(orderStatusChangedTopic)
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
    public NewTopic orderStatusChangedDeadLetterTopic() {
        return TopicBuilder.name(orderStatusChangedTopic + ".DLT").partitions(3).replicas(1)
                .config("retention.ms", "1209600000").build();
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
