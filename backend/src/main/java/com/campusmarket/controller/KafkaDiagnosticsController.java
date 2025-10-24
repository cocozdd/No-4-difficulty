package com.campusmarket.controller;

import com.campusmarket.dto.KafkaTestRequest;
import com.campusmarket.dto.KafkaTestResponse;
import com.campusmarket.service.KafkaDiagnosticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/diagnostics/kafka")
public class KafkaDiagnosticsController {

    private final KafkaDiagnosticsService kafkaDiagnosticsService;

    public KafkaDiagnosticsController(KafkaDiagnosticsService kafkaDiagnosticsService) {
        this.kafkaDiagnosticsService = kafkaDiagnosticsService;
    }

    @PostMapping("/order-events")
    public ResponseEntity<KafkaTestResponse> publishOrderEvent(@Valid @RequestBody KafkaTestRequest request) {
        KafkaTestResponse response = kafkaDiagnosticsService.publishOrderEvent(request);
        return ResponseEntity.ok(response);
    }
}
