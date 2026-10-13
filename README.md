# Deadline demo: a small app and its test suite

```
deadline-demo/
├── deadline-app/     Spring Boot app: assignments, deadlines, submissions (port 8081)
└── deadline-tests/   Selenium + TestNG + REST Assured tests that run against it
```

## Run it (Mac)

You need Java 21+ (the JRE 25 you already have is fine), Maven (`brew install maven`) and Google Chrome.

**Terminal 1: start the app**
```
cd deadline-demo/deadline-app
```
```
mvn spring-boot:run
```
Leave it running. It's ready when you see `Started DeadlineApplication`.

**Terminal 2: run the tests**
```
cd deadline-demo/deadline-tests
```
```
mvn test
```
To watch Chrome open and switch through the timezones:
```
mvn test -Dheadless=false
```
Expected result: `Tests run: 15, Failures: 0`.

In IntelliJ you can also click the green ▶ next to any test class or method, as long as the app in Terminal 1 is running.

## Try it by hand

Create an assignment due Friday 23 Oct 2026, 17:00 London time:
```
curl -s -X POST localhost:8081/assignments -H "Authorization: Bearer instructor-demo-token" -H "Content-Type: application/json" -d '{"title":"Essay 1","deadline":"2026-10-23T16:00:00Z","allowLateSubmissions":false}'
```
Copy the `id` from the reply and open `http://localhost:8081/assignments/<id>` in Chrome. The deadline shows in your Mac's timezone.

To see it from another timezone: open DevTools (Cmd+Opt+I), press Cmd+Shift+P, type **Show Sensors**, and set a location such as Tokyo. Then reload the page.

Pretend it's 1 second after the deadline (server clock):
```
curl -s -X POST localhost:8081/test-support/clock -H "Content-Type: application/json" -d '{"now":"2026-10-23T16:00:01Z","frozen":true}'
```
Back to the real time:
```
curl -s -X DELETE localhost:8081/test-support/clock
```

## Endpoints

| Method | Path | Who | What |
|---|---|---|---|
| POST | `/assignments` | instructor token | Create an assignment `{title, deadline (UTC ISO), allowLateSubmissions}` → `201 {id}` |
| GET | `/assignments/{id}` | anyone | Student page (deadline shown in the browser's local time) |
| GET | `/assignments/{id}/details` | anyone | Assignment JSON (deadline in UTC) |
| POST | `/assignments/{id}/submissions` | student token | Submit text → `201 {late}` or `403 {error: DEADLINE_PASSED}` |
| POST | `/assignments/{id}/submissions/start` | page | Student clicks Submit: records server time, returns a ticket |
| POST | `/assignments/{id}/submissions/upload` | page | File upload. Judged by the ticket's start time (10-minute window) |
| POST / DELETE | `/test-support/clock` | tests | Set, freeze or reset the server clock. **Staging only** (`testsupport.enabled`) |

Demo tokens: `instructor-demo-token`, `student-demo-token`.

## Business rules the app implements (and the tests check)
- Deadlines are stored in UTC and displayed in each student's local time, with a timezone label.
- The **server** clock decides lateness. Any time sent by the client is ignored.
- A submission exactly at the deadline is on time (inclusive).
- With late submissions off, anything after the deadline is rejected (403). With them on, it's accepted and flagged `late: true`.
- For uploads, the moment the student clicked Submit counts, provided the upload finishes within 10 minutes.
