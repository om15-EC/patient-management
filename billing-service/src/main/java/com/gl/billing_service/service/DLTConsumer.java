package com.gl.billing_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DLTConsumer {

    @KafkaListener(topics = "patient.DLT",
                   groupId = "billing-service-dlt")
    public void handlePatientDLT(byte[] message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {

        log.error("DLT message received — topic: {} error: {}",
                topic, errorMessage);
    }
}