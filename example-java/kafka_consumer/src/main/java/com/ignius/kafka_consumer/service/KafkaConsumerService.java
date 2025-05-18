package com.ignius.kafka_consumer.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.kafka.receiver.KafkaReceiver;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final KafkaReceiver<String, String> kafkaReceiver;

    @PostConstruct
    public void consumeMessages() {
        kafkaReceiver.receive()
                .subscribe(
                        record -> {
                            log.info("\nReceived record: \n Topic - {} \n Partition - {} \n Offset - {}, \n Key - {} \n Value - {},",
                                    record.topic(), record.partition(), record.offset(), record.key(), record.value());

                            // Process the message here
                            log.info("Working record...");
                            // Acknowledge the message here
                            record.receiverOffset().acknowledge();
                        });
    }

}
