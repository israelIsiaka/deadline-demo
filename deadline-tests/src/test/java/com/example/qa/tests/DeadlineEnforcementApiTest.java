package com.example.qa.tests;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.oneOf;

import com.example.qa.support.AssignmentApi;
import com.example.qa.support.TestClock;
import java.time.Instant;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Deadline ENFORCEMENT is the server's job. These tests call the API directly
 * (bypassing the UI) and control the server clock, so nothing waits for real time.
 */
public class DeadlineEnforcementApiTest {

    private static final Instant DEADLINE = Instant.parse("2026-10-23T16:00:00Z");

    @AfterMethod(alwaysRun = true)
    public void restoreRealClock() {
        TestClock.reset();
    }

    // ---------- Boundary: 1 second before / exactly at / 1 second after ----------

    @Test(groups = "regression")
    public void submissionOneSecondBeforeDeadlineIsAccepted() {
        String id = AssignmentApi.createAssignment(DEADLINE, false);
        TestClock.freezeAt(DEADLINE.minusSeconds(1));

        AssignmentApi.submit(id).then()
                .statusCode(201)
                .body("late", equalTo(false));
    }

    /** Exactly-at-deadline rule (inclusive vs exclusive) must be confirmed with the PO. */
    @Test(groups = "regression")
    public void submissionExactlyAtDeadlineFollowsAgreedRule() {
        String id = AssignmentApi.createAssignment(DEADLINE, false);
        TestClock.freezeAt(DEADLINE);

        // Assumed rule: the deadline instant itself is still on time.
        AssignmentApi.submit(id).then()
                .statusCode(201)
                .body("late", equalTo(false));
    }

    @Test(groups = "regression")
    public void submissionOneSecondAfterDeadlineIsRejected() {
        String id = AssignmentApi.createAssignment(DEADLINE, false);
        TestClock.freezeAt(DEADLINE.plusSeconds(1));

        AssignmentApi.submit(id).then()
                .statusCode(oneOf(403, 422))
                .body("error", equalTo("DEADLINE_PASSED"));
    }

    // ---------- Late submissions allowed ----------

    @Test(groups = "regression")
    public void lateSubmissionIsAcceptedButFlaggedWhenAllowed() {
        String id = AssignmentApi.createAssignment(DEADLINE, true);
        TestClock.freezeAt(DEADLINE.plusSeconds(60));

        AssignmentApi.submit(id).then()
                .statusCode(201)
                .body("late", equalTo(true));
    }

    // ---------- Tampering: the client's clock must never be trusted ----------

    @Test(groups = "regression")
    public void clientSuppliedTimestampIsIgnored() {
        String id = AssignmentApi.createAssignment(DEADLINE, false);
        TestClock.freezeAt(DEADLINE.plusSeconds(3600)); // really an hour late

        // A student changed their laptop clock / edited the request
        // to claim they submitted before the deadline.
        AssignmentApi.submit(id, Map.of(
                        "text", "My essay",
                        "submittedAt", DEADLINE.minusSeconds(600).toString()))
                .then()
                .statusCode(oneOf(403, 422))
                .body("error", equalTo("DEADLINE_PASSED"));
    }
}
