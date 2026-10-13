package com.example.qa.tests;

import static org.testng.Assert.assertTrue;

import com.example.qa.pages.AssignmentPage;
import com.example.qa.support.AssignmentApi;
import com.example.qa.support.BrowserFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * ONE test, many timezones (data-driven).
 * The instructor sets a single deadline; every student must see it converted
 * correctly to their own local time, including date flips and DST changes.
 */
public class DeadlineTimezoneDisplayTest {

    /** The UI format we expect, e.g. "Fri 23 Oct 2026, 17:00". Agree this with the team. */
    private static final DateTimeFormatter UI_FORMAT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);

    // Friday 23 Oct 2026, 17:00 London time (BST)
    private static final Instant FRIDAY_DEADLINE = Instant.parse("2026-10-23T16:00:00Z");
    // UK clocks go back at 01:00 UTC on Sun 25 Oct 2026: 01:30 London happens twice
    private static final Instant DST_FIRST_0130 = Instant.parse("2026-10-25T00:30:00Z");  // 01:30 BST
    private static final Instant DST_SECOND_0130 = Instant.parse("2026-10-25T01:30:00Z"); // 01:30 GMT

    private ChromeDriver driver;

    @DataProvider(name = "studentTimezones")
    public Object[][] studentTimezones() {
        return new Object[][] {
                // deadline (UTC),  student timezone,      why this case matters
                {FRIDAY_DEADLINE,  "Europe/London",      "instructor's own zone"},
                {FRIDAY_DEADLINE,  "Africa/Lagos",       "UTC+1, no daylight saving"},
                {FRIDAY_DEADLINE,  "America/New_York",   "behind UTC"},
                {FRIDAY_DEADLINE,  "Australia/Sydney",   "date flips to Saturday"},
                {FRIDAY_DEADLINE,  "Asia/Kolkata",       "half-hour offset (+5:30)"},
                {FRIDAY_DEADLINE,  "Asia/Kathmandu",     "45-minute offset (+5:45)"},
                {DST_FIRST_0130,   "Europe/London",      "DST: first 01:30 (BST)"},
                {DST_SECOND_0130,  "Europe/London",      "DST: repeated 01:30 (GMT)"},
                {DST_SECOND_0130,  "Africa/Lagos",       "Lagos-London gap changes after DST"},
        };
    }

    @Test(groups = {"timezone", "regression"}, dataProvider = "studentTimezones")
    public void deadlineIsShownInStudentsLocalTime(Instant deadlineUtc, String timezone, String reason) {
        // Arrange: instructor creates the assignment via API (fast, no UI needed)
        String assignmentId = AssignmentApi.createAssignment(deadlineUtc, false);

        // Independent oracle: what the student SHOULD see, computed with java.time
        String expected = UI_FORMAT.format(deadlineUtc.atZone(ZoneId.of(timezone)));

        // Act: open the page in a browser emulating the student's timezone
        driver = BrowserFactory.chromeInTimezone(timezone);
        String shown = new AssignmentPage(driver).open(assignmentId).deadlineText();

        // Assert: correct local date/time AND a timezone label, so the
        // two different "01:30" deadlines on DST night are not ambiguous
        assertTrue(shown.contains(expected),
                "[" + timezone + " | " + reason + "] expected '" + expected + "' but UI showed '" + shown + "'");
        assertTrue(shown.matches(".*\\b(GMT|UTC|[A-Z]{2,5})([+-]\\d{1,2}(:\\d{2})?)?\\s*$"),
                "[" + timezone + "] deadline must include a timezone label, UI showed '" + shown + "'");
    }

    @AfterMethod(alwaysRun = true)
    public void quitBrowser() {
        if (driver != null) driver.quit();
    }
}
