package com.example.deadlineapp;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * The ONLY source of "now" for deadline decisions. Normally the real clock;
 * in staging, tests can freeze it or move it to any instant.
 */
@Component
public class AppClock {

    private volatile boolean overridden = false;
    private volatile boolean frozen = false;
    private volatile Instant base;
    private volatile long setAtNanos;

    public Instant now() {
        if (!overridden) return Instant.now();
        if (frozen) return base;
        return base.plus(Duration.ofNanos(System.nanoTime() - setAtNanos));
    }

    public synchronized void set(Instant now, boolean freeze) {
        this.base = now;
        this.frozen = freeze;
        this.setAtNanos = System.nanoTime();
        this.overridden = true;
    }

    public synchronized void reset() {
        this.overridden = false;
    }
}
