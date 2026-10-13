package com.example.qa.tests;

import static org.testng.Assert.assertEquals;

import com.example.qa.pages.AssignmentPage;
import com.example.qa.support.AssignmentApi;
import com.example.qa.support.BrowserFactory;
import com.example.qa.support.TestClock;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Follow-up question: student in Lagos clicks Submit at 16:59:58,
 * but a 50 MB upload only finishes at ~17:00:20. Is it late?
 *
 * The business rule must be confirmed first. This test encodes the rule
 * "the time the student STARTED the submission counts", so the expected
 * result is ON TIME. If the agreed rule is "upload completion counts",
 * flip EXPECTED_STATUS to "Late" (or "Rejected").
 */
public class SlowUploadAtDeadlineTest {

    private static final String EXPECTED_STATUS = "Submitted on time";

    private ChromeDriver driver;
    private Path bigFile;

    @Test(groups = "regression")
    public void uploadStartedBeforeDeadlineButFinishingAfterFollowsAgreedRule() throws IOException {
        Instant deadline = Instant.parse("2026-10-23T16:00:00Z"); // 17:00 in Lagos
        String id = AssignmentApi.createAssignment(deadline, false);

        bigFile = createFile(50); // 50 MB essay with images

        driver = BrowserFactory.chromeInTimezone("Africa/Lagos");
        AssignmentPage page = new AssignmentPage(driver).open(id);
        BrowserFactory.throttleNetwork(driver, 20_000); // ~20 Mbps up -> ~20 s upload

        // Server time starts 2 s before the deadline and keeps ticking,
        // so the deadline passes WHILE the file is still uploading.
        TestClock.startAt(deadline.minusSeconds(2));
        page.uploadAndSubmit(bigFile);

        assertEquals(page.waitForReceiptStatus(Duration.ofSeconds(90)), EXPECTED_STATUS);
    }

    private static Path createFile(int megabytes) throws IOException {
        Path file = Files.createTempFile("essay-", ".pdf");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.setLength((long) megabytes * 1024 * 1024);
        }
        return file;
    }

    @AfterMethod(alwaysRun = true)
    public void cleanUp() throws IOException {
        if (driver != null) driver.quit();
        TestClock.reset();
        if (bigFile != null) Files.deleteIfExists(bigFile);
    }
}
