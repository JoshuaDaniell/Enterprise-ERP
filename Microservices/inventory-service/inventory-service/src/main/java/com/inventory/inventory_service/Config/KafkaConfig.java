package com.inventory.inventory_service.Config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic stockAlertTopic() {
        return TopicBuilder.name("stock-alerts")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
