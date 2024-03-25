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
        Thread.sleep(SLEEP_DURATION);

        //Section 8 should have two assignments
        WebElement sec8 = driver.findElement(By.id("8"));
        sec8.findElement(By.linkText("View Assignments")).click();
        Thread.sleep(SLEEP_DURATION);

        //We will select assignment one to grade.
        WebElement ass1 = driver.findElement(By.id("1"));
        ass1.findElements(By.tagName("button")).get(0).click();
        Thread.sleep(SLEEP_DURATION);

        //We will enter a score of 88 for all students
        List<WebElement> scoreFields = driver.findElements(By.name("score"));

        for(WebElement field:scoreFields){
            field.clear();
            field.sendKeys("88");
        }
        driver.findElement(By.id("saveGrades")).click();
        String message = driver.findElement(By.id("editMessage")).getText();
        assertEquals("Grades saved!", message);
        driver.findElement(By.id("closeGrades")).click();



    }

}
