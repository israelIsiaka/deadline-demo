package com.example.qa.support;

/**
 * All environment-specific values come from -D system properties or env vars,
 * so the same suite runs locally, in Jenkins, or against any staging stack.
 */
public final class Config {

    private Config() {}

    // Defaults point at the local demo app (../deadline-app). Override with -D for a real staging stack.
    public static final String WEB_URL = get("webUrl", "WEB_URL", "http://localhost:8081");
    public static final String API_URL = get("apiUrl", "API_URL", "http://localhost:8081");
    public static final String STUDENT_TOKEN = get("studentToken", "STUDENT_TOKEN", "student-demo-token");
    public static final String INSTRUCTOR_TOKEN = get("instructorToken", "INSTRUCTOR_TOKEN", "instructor-demo-token");
    public static final boolean HEADLESS = Boolean.parseBoolean(get("headless", "HEADLESS", "true"));
    // Pause after each page step so a human can watch. Only when Chrome is visible; -DpauseMs=0 turns it off.
    public static final long PAUSE_MS = HEADLESS ? 0 : Long.parseLong(get("pauseMs", "PAUSE_MS", "3000"));

    private static String get(String prop, String env, String fallback) {
        String value = System.getProperty(prop);
        if (value == null || value.isBlank()) value = System.getenv(env);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
