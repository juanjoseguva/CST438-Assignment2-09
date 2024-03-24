package com.cst438.controller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class AssignmentControllerSystemTest {

    //Path to selenium driver
    public static final String CHROME_DRIVER_FILE_LOCATION =
            "/Users/kyleabsten/Library/Mobile Documents/com~apple~CloudDocs/CSUMB/CST438_SoftwareEngineering/downloads/chromedriver-mac-arm64/chromedriver";

    //Url of react/nodejs server
    public static final String reactURL = "http://localhost:3000";

    //Sleep variable = 1 second
    public static final int SLEEP_DURATION = 1000;


    WebDriver driver;

    @BeforeEach
    public void setUpDriver() throws Exception {
        //Set properties required by Chrome driver
        System.setProperty(
                "webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
        ChromeOptions ops = new ChromeOptions();
        ops.addArguments("--remote-allow-origins=*");

        //Start driver
        driver = new ChromeDriver(ops);

        driver.get(reactURL);
        //Short wait to allow page to download
        Thread.sleep(SLEEP_DURATION);

    }

    /*
    @AfterEach
    public void destroyDriver() {
        if (driver != null) {
            //Quit driver
            driver.close();
            driver.quit();
            driver = null;
        }
    }

     */

    @Test
    public void systemTestGradeAssignment() throws Exception {
        //Input the proper year and semester
        driver.findElement(By.id("year")).sendKeys("2024");
        driver.findElement(By.id("semester")).sendKeys("Spring");
        driver.findElement(By.linkText("Show Sections")).click();

    }

}
