package com.example.qa.support;

import java.util.Map;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Starts Chrome "as if" the student were in a given timezone, using the
 * Chrome DevTools Protocol. No need to change the laptop or CI machine clock.
 */
public final class BrowserFactory {

    private BrowserFactory() {}

    public static ChromeDriver chromeInTimezone(String ianaTimezone) {
        ChromeOptions options = new ChromeOptions();
        if (Config.HEADLESS) options.addArguments("--headless=new");
        options.addArguments("--window-size=1366,900", "--lang=en-GB");
        // Optional: use a specific Chrome/Chromium build (normally not needed on a Mac)
        String chromeBinary = System.getProperty("chromeBinary");
        if (chromeBinary != null && !chromeBinary.isBlank()) options.setBinary(chromeBinary);
        // Optional extra flags, e.g. -DchromeArgs=--no-sandbox for Linux CI containers
        String extraArgs = System.getProperty("chromeArgs");
        if (extraArgs != null && !extraArgs.isBlank()) options.addArguments(extraArgs.trim().split("\\s+"));

        ChromeDriver driver = new ChromeDriver(options);
        // Same as DevTools > Sensors > Location/Timezone override.
        driver.executeCdpCommand("Emulation.setTimezoneOverride",
                Map.of("timezoneId", ianaTimezone));
        return driver;
    }

    /** Simulates a slow connection (e.g. a student on weak mobile data). */
    public static void throttleNetwork(ChromeDriver driver, int uploadKbps) {
        driver.executeCdpCommand("Network.enable", Map.of());
        driver.executeCdpCommand("Network.emulateNetworkConditions", Map.of(
                "offline", false,
                "latency", 300,                                // ms
                "downloadThroughput", 1_000 * 1024 / 8,         // ~1 Mbps
                "uploadThroughput", uploadKbps * 1024 / 8));    // bytes/sec
    }
}
