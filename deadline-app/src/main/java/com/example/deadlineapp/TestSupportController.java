package com.example.deadlineapp;

import java.time.Instant;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Staging-only hook so tests can make a deadline expire on demand.
 * Only exists when testsupport.enabled=true (never in production).
 */
@RestController
@RequestMapping("/test-support/clock")
@ConditionalOnProperty(name = "testsupport.enabled", havingValue = "true")
public class TestSupportController {

    private final AppClock clock;

    public TestSupportController(AppClock clock) {
        this.clock = clock;
    }

    record ClockRequest(Instant now, boolean frozen) {}

    @PostMapping
    public Map<String, Object> set(@RequestBody ClockRequest request) {
        clock.set(request.now(), request.frozen());
        return Map.of("now", clock.now().toString(), "frozen", request.frozen());
    }

    @DeleteMapping
    public ResponseEntity<Void> reset() {
        clock.reset();
        return ResponseEntity.noContent().build();
    }
}
