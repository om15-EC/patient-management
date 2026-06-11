package com.gl.patient_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic patientTopic() {
        return TopicBuilder.name("patient")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic billingEventsTopic() {
        return TopicBuilder.name("billing.events")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // DLT topics
    @Bean
    public NewTopic patientDLTTopic() {
        return TopicBuilder.name("patient.DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic billingEventsDLTTopic() {
        return TopicBuilder.name("billing.events.DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }
}