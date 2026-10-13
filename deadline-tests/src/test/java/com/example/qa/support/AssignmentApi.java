package com.example.qa.support;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import java.time.Instant;
import java.util.Map;

/** Thin API client so tests read as business steps, not HTTP plumbing. */
public final class AssignmentApi {

    private AssignmentApi() {}

    /** Instructor creates an assignment with a deadline (stored and sent as UTC). */
    public static String createAssignment(Instant deadlineUtc, boolean allowLate) {
        return given().baseUri(Config.API_URL)
                .header("Authorization", "Bearer " + Config.INSTRUCTOR_TOKEN)
                .contentType("application/json")
                .body(Map.of(
                        "title", "Timezone test " + System.currentTimeMillis(),
                        "deadline", deadlineUtc.toString(),
                        "allowLateSubmissions", allowLate))
                .when().post("/assignments")
                .then().statusCode(201)
                .extract().path("id");
    }

    /** Student submits directly via the API — bypassing any UI checks. */
    public static Response submit(String assignmentId, Map<String, Object> body) {
        return given().baseUri(Config.API_URL)
                .header("Authorization", "Bearer " + Config.STUDENT_TOKEN)
                .contentType("application/json")
                .body(body)
                .when().post("/assignments/" + assignmentId + "/submissions");
    }

    public static Response submit(String assignmentId) {
        return submit(assignmentId, Map.of("text", "My essay"));
    }
}
