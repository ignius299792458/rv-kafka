package com.ignius.kafka_producer.service;

import com.ignius.kafka_producer.kafka_topics.MessagingTopicEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ReactiveKafkaProducerTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = MessagingTopicEnum.first_topic.name();

    // send the message on topic
    public Mono<SenderResult<Void>> sendMessage(String message) {
        return kafkaTemplate.send(TOPIC, message)
                .doOnSuccess(result ->
                        log.info("Message sent successfully, topic: {}, partition: {}, offset: {}",
                                TOPIC, result.recordMetadata().partition(), result.recordMetadata().offset()))
                .doOnError(e -> log.error("Error sending message to topic: {}", TOPIC, e));
    }
}
