package com.example.deadlineapp;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Minimal assignment service. Data lives in memory (restart = empty).
 *
 * Business rules implemented:
 *  - Deadlines are stored as UTC instants.
 *  - The SERVER clock decides lateness; any client-sent time is ignored.
 *  - A submission exactly AT the deadline is on time (inclusive).
 *  - For file uploads, the time the student clicked Submit counts
 *    (a "start ticket" is issued then), as long as the upload finishes
 *    within 10 minutes.
 */
@Controller
public class AssignmentController {

    private static final Duration UPLOAD_WINDOW = Duration.ofMinutes(10);

    record Assignment(String id, String title, Instant deadline, boolean allowLateSubmissions) {}
    record CreateAssignment(String title, Instant deadline, boolean allowLateSubmissions) {}
    record Ticket(String assignmentId, Instant startedAt) {}
    enum Verdict { ON_TIME, LATE, REJECTED }

    private final Map<String, Assignment> assignments = new ConcurrentHashMap<>();
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final AppClock clock;
    private final String studentToken;
    private final String instructorToken;

    public AssignmentController(AppClock clock,
                                @Value("${demo.student-token}") String studentToken,
                                @Value("${demo.instructor-token}") String instructorToken) {
        this.clock = clock;
        this.studentToken = studentToken;
        this.instructorToken = instructorToken;
    }

    // ------------------------------------------------------------ API

    /** Instructor creates an assignment. */
    @PostMapping("/assignments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> create(@RequestHeader(value = "Authorization", required = false) String auth,
                                                      @RequestBody CreateAssignment body) {
        if (!isBearer(auth, instructorToken)) return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        if (body.deadline() == null) return error(HttpStatus.BAD_REQUEST, "DEADLINE_REQUIRED");

        String id = UUID.randomUUID().toString();
        assignments.put(id, new Assignment(id, body.title(), body.deadline(), body.allowLateSubmissions()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    /** Assignment details for the page (deadline is always sent as UTC). */
    @GetMapping("/assignments/{id}/details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> details(@PathVariable String id) {
        Assignment a = assignments.get(id);
        if (a == null) return error(HttpStatus.NOT_FOUND, "NOT_FOUND");
        return ResponseEntity.ok(Map.of(
                "id", a.id(),
                "title", a.title() == null ? "" : a.title(),
                "deadline", a.deadline().toString(),
                "allowLateSubmissions", a.allowLateSubmissions()));
    }

    /** Student submits text via the API. Any "submittedAt" in the body is ignored. */
    @PostMapping("/assignments/{id}/submissions")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submit(@RequestHeader(value = "Authorization", required = false) String auth,
                                                      @PathVariable String id,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        if (!isBearer(auth, studentToken)) return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        Assignment a = assignments.get(id);
        if (a == null) return error(HttpStatus.NOT_FOUND, "NOT_FOUND");

        Instant receivedAt = clock.now(); // server time, never the client's
        return respond(a, decide(a, receivedAt), receivedAt);
    }

    // ------------------------------------------------------------ Web UI support

    /** The student's assignment page. */
    @GetMapping("/assignments/{id}")
    public String page(@PathVariable String id) {
        return "forward:/assignment.html";
    }

    /** Called when the student clicks Submit: records the server time. */
    @PostMapping("/assignments/{id}/submissions/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> start(@PathVariable String id) {
        Assignment a = assignments.get(id);
        if (a == null) return error(HttpStatus.NOT_FOUND, "NOT_FOUND");
        Instant startedAt = clock.now();
        if (decide(a, startedAt) == Verdict.REJECTED) return error(HttpStatus.FORBIDDEN, "DEADLINE_PASSED");

        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, new Ticket(id, startedAt));
        return ResponseEntity.ok(Map.of("ticket", ticket, "startedAt", startedAt.toString()));
    }

    /** The (possibly slow) file upload. Lateness is judged by the ticket's start time. */
    @PostMapping("/assignments/{id}/submissions/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> upload(@PathVariable String id,
                                                      @RequestParam("ticket") String ticketId,
                                                      @RequestParam("file") MultipartFile file) {
        Assignment a = assignments.get(id);
        Ticket ticket = tickets.remove(ticketId);
        if (a == null || ticket == null || !ticket.assignmentId().equals(id)) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_TICKET");
        }
        Instant finishedAt = clock.now();
        if (Duration.between(ticket.startedAt(), finishedAt).compareTo(UPLOAD_WINDOW) > 0) {
            return error(HttpStatus.FORBIDDEN, "UPLOAD_WINDOW_EXPIRED");
        }
        Verdict verdict = decide(a, ticket.startedAt());
        ResponseEntity<Map<String, Object>> response = respond(a, verdict, ticket.startedAt());
        if (verdict != Verdict.REJECTED) {
            response.getBody().put("fileSizeBytes", file.getSize());
            response.getBody().put("uploadFinishedAt", finishedAt.toString());
        }
        return response;
    }

    // ------------------------------------------------------------ Rules

    private Verdict decide(Assignment a, Instant at) {
        if (!at.isAfter(a.deadline())) return Verdict.ON_TIME;       // inclusive deadline
        return a.allowLateSubmissions() ? Verdict.LATE : Verdict.REJECTED;
    }

    private ResponseEntity<Map<String, Object>> respond(Assignment a, Verdict verdict, Instant countedAt) {
        if (verdict == Verdict.REJECTED) return error(HttpStatus.FORBIDDEN, "DEADLINE_PASSED");
        Map<String, Object> body = new ConcurrentHashMap<>();
        body.put("id", UUID.randomUUID().toString());
        body.put("late", verdict == Verdict.LATE);
        body.put("status", verdict == Verdict.LATE ? "Submitted late" : "Submitted on time");
        body.put("countedAt", countedAt.toString());
        body.put("deadline", a.deadline().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    private static boolean isBearer(String header, String expectedToken) {
        return header != null && header.equals("Bearer " + expectedToken);
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(Map.of("error", code));
    }
}
