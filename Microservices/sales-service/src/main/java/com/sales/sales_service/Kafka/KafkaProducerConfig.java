package com.sales.sales_service.Kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    @Value("${application.kafka.topics.order-placed:order-placed-topic}")
    private String orderPlacedTopic;

    @Value("${application.kafka.topics.order-status-changed:order-status-changed-topic}")
    private String orderStatusChangedTopic;

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name(orderPlacedTopic)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderStatusChangedTopic() {
        return TopicBuilder.name(orderStatusChangedTopic)
                .partitions(2)
                .replicas(1)
                .build();
    }
}
