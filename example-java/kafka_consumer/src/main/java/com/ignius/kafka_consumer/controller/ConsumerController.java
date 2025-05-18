package com.ignius.kafka_consumer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("kafka/consumer")
public class ConsumerController {

    @GetMapping("/status")
    public Mono<String> status() {
        return Mono.just("Consumer is running and listening to Kafka topics...");
    }
}
