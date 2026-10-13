package com.example.qa.support;

import static io.restassured.RestAssured.given;

import java.time.Instant;
import java.util.Map;

/**
 * Controls the SERVER clock in staging so a deadline can expire on demand,
 * instead of waiting for real time to pass.
 *
 * ASSUMPTION: developers expose a staging-only endpoint (never deployed to
 * production) that sets the time the backend treats as "now".
 *   POST   /test-support/clock  {"now": "2026-10-23T15:59:59Z", "frozen": true}
 *   DELETE /test-support/clock  -> back to the real clock
 * The exact contract is something to agree with the dev team.
 */
public final class TestClock {

    private TestClock() {}

    /** Freeze server time at an exact instant (for precise boundary tests). */
    public static void freezeAt(Instant now) {
        set(now, true);
    }

    /** Jump server time to an instant and let it keep ticking from there. */
    public static void startAt(Instant now) {
        set(now, false);
    }

    public static void reset() {
        given().baseUri(Config.API_URL)
               .when().delete("/test-support/clock")
               .then().statusCode(204);
    }

    private static void set(Instant now, boolean frozen) {
        given().baseUri(Config.API_URL)
               .contentType("application/json")
               .body(Map.of("now", now.toString(), "frozen", frozen))
               .when().post("/test-support/clock")
               .then().statusCode(200);
    }
}
