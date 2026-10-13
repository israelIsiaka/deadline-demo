package com.example.qa.pages;

import com.example.qa.support.Config;
import java.nio.file.Path;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object for the student's assignment page.
 * Locators use data-testid attributes (stable, not tied to styling).
 */
public class AssignmentPage {

    private static final By DEADLINE_TEXT = By.cssSelector("[data-testid='deadline']");
    private static final By FILE_INPUT = By.cssSelector("[data-testid='file-input']");
    private static final By SUBMIT_BUTTON = By.cssSelector("[data-testid='submit']");
    private static final By RECEIPT_STATUS = By.cssSelector("[data-testid='receipt-status']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public AssignmentPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public AssignmentPage open(String assignmentId) {
        driver.get(Config.WEB_URL + "/assignments/" + assignmentId);
        wait.until(ExpectedConditions.visibilityOfElementLocated(DEADLINE_TEXT));
        return this;
    }

    public String deadlineText() {
        return driver.findElement(DEADLINE_TEXT).getText();
    }

    public void uploadAndSubmit(Path file) {
        driver.findElement(FILE_INPUT).sendKeys(file.toAbsolutePath().toString());
        driver.findElement(SUBMIT_BUTTON).click();
    }

    /** Waits for the receipt after a (possibly slow) upload finishes. */
    public String waitForReceiptStatus(Duration timeout) {
        return new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.visibilityOfElementLocated(RECEIPT_STATUS))
                .getText();
    }
}
