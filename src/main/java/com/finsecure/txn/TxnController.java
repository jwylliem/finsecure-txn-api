package com.finsecure.txn;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.MeterRegistry;

@RestController
public class TxnController {

    private static final Logger log = LoggerFactory.getLogger(TxnController.class);

    private final TxnValidator validator;
    private final MeterRegistry metrics;
    private final String apiKey;
    private final String version;

    public TxnController(TxnValidator validator, MeterRegistry metrics,
                         @Value("${txn.api-key}") String apiKey, @Value("${app.version}") String version) {
        this.validator = validator;
        this.metrics = metrics;
        this.apiKey = apiKey;
        this.version = version;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "version", version);
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestHeader(value = "X-API-Key", required = false) String key,
                                                        @RequestBody Transaction t) {
        if (apiKey.isEmpty() || !apiKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid api key"));
        }
        List<String> errors = validator.validate(t);
        String result = errors.isEmpty() ? "valid" : "invalid";
        metrics.counter("txn_validations", "result", result).increment();
        log.info("transaction {} is {} {}", t.id(), result, errors);

        if (errors.isEmpty()) {
            return ResponseEntity.ok(Map.of("id", t.id(), "valid", true));
        }
        return ResponseEntity.unprocessableContent().body(Map.of("id", String.valueOf(t.id()), "valid", false, "errors", errors));
    }
}
