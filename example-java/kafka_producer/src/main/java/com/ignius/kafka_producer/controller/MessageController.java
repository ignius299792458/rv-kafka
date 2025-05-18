package com.ignius.kafka_producer.controller;


import com.ignius.kafka_producer.service.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/kafka/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessagingService messagingService;

    @PostMapping
    public Mono<ResponseEntity<Void>> sendMessage(@RequestBody String message) {
        return messagingService.sendMessage(message)
                .map(result -> ResponseEntity.ok().<Void>build())
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}
