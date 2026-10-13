# Deadline & timezone tests (interview example)

Selenium 4 + TestNG + REST Assured + Maven. Selenium is downloaded by Maven
automatically; there is nothing to install by hand except Java and Chrome.

## What is covered
| Test class | What it proves |
|---|---|
| `DeadlineTimezoneDisplayTest` | One data-driven test across 9 timezone cases: Lagos, London, New York, Sydney (date flip), India (+5:30), Nepal (+5:45), and the UK daylight-saving night. Uses Chrome DevTools timezone emulation. |
| `DeadlineEnforcementApiTest` | Server-side enforcement via the API: 1 s before / at / 1 s after the deadline, late-allowed mode, and a fake client timestamp being ignored. |
| `SlowUploadAtDeadlineTest` | Upload starts 2 s before the deadline and finishes after it, on a throttled network. |

## What the tests need from the app
These hooks are provided by the demo app in `../deadline-app`. On a real product they'd be agreed with the dev team:
- A staging-only clock endpoint: `POST/DELETE /test-support/clock` (see `TestClock`).
- `data-testid` attributes on the assignment page.
- Endpoints `POST /assignments` and `POST /assignments/{id}/submissions`.

## Run
Start the demo app first (see the README in the parent folder), then:
```
mvn test
```
Against a different environment:
```
mvn test -DwebUrl=https://staging.example.com -DapiUrl=https://api.staging.example.com -DstudentToken=... -DinstructorToken=...
```
Only the regression group:
```
mvn test -Dgroups=regression
```

## CI: prove the suite does not depend on the machine's timezone
Run the same suite with the build agent set to different timezones; results must be identical.
```
TZ=Asia/Kolkata mvn test -Duser.timezone=Asia/Kolkata
```
```
TZ=America/New_York mvn test -Duser.timezone=America/New_York
```
