//note: requires user to be STUDENT

package com.cst438.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StudentControllerSystemTest {

    public static final String CHROME_DRIVER_FILE_LOCATION =
            "./chromedriver";

    //public static final String CHROME_DRIVER_FILE_LOCATION =
    //        "~/chromedriver_macOS/chromedriver";
    public static final String URL = "http://localhost:3000/";

    public static final int SLEEP_DURATION = 1000; // 1 second.

    // add selenium dependency to pom.xml

    // these tests assumes that test data does NOT contain any
    // sections for course cst499 in 2024 Spring term.

    WebDriver driver;

    @BeforeEach
    public void setUpDriver() throws Exception {

        // set properties required by Chrome Driver
        System.setProperty(
                "webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
        ChromeOptions ops = new ChromeOptions();
        ops.addArguments("--remote-allow-origins=*");

        // start the driver
        driver = new ChromeDriver(ops);

        driver.get(URL);
        // must have a short wait to allow time for the page to download
        Thread.sleep(SLEEP_DURATION);

    }

    @AfterEach
    public void terminateDriver() {
        if (driver != null) {
            // quit driver
            driver.close();
            driver.quit();
            driver = null;
        }
    }

    @Test
    public void systemTestEnrollSection() throws Exception {
        // add first available section
        //TODO:
        // verify enrollment shows on transcript
        // delete the enrollment
        // verify the enrollment is gone

        //click link to navigate to course enroll
        WebElement we = driver.findElement(By.id("addCourse"));
        we.click();
        Thread.sleep(SLEEP_DURATION);

        //connect driver to buttons
        List<WebElement> buttons = driver.findElements(By.tagName("button"));

        //consider clicking each available section
//        for (WebElement button : buttons) {
//            button.click();
//            Thread.sleep(SLEEP_DURATION);
//        }

        //click to enroll in first available section
        buttons.get(0).click();
        Thread.sleep(SLEEP_DURATION);

        String message = driver.findElement(By.id("addMessage")).getText();
        //assertTrue(message.equals("course added"));
        assertEquals("course added", message);

    }
}
