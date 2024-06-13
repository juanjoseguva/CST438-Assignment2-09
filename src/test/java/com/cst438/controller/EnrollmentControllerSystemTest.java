package com.cst438.controller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EnrollmentControllerSystemTest {

    public static final String CHROME_DRIVER_FILE_LOCATION =
            "C:/chromedriver-win64/chromedriver.exe";

    //public static final String CHROME_DRIVER_FILE_LOCATION =
    //        "~/chromedriver_macOS/chromedriver";

    public static final String URL = "http://localhost:3000/";
    public static final int SLEEP_DURATION = 1000; // 1 second.
    WebDriver driver;

    // these tests assumes that test data does NOT contain any
    // sections for course cst499 in 2024 Spring term.

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
    public void systemTestEnterFinalGrade() throws Exception {
        //Input the proper year and semester
        driver.findElement(By.id("year")).sendKeys("2024");
        driver.findElement(By.id("semester")).sendKeys("Spring");
        driver.findElement(By.linkText("Show Sections")).click();
        Thread.sleep(SLEEP_DURATION);

        //Enter section #8
        WebElement sec8 = driver.findElement(By.id("8"));
        sec8.findElement(By.linkText("View Enrollments")).click();
        Thread.sleep(SLEEP_DURATION);

        //change grade for all students
        List <WebElement> changeGradeButt = driver.findElements(By.name("changer"));
        for(WebElement changeGrade:changeGradeButt){
            changeGrade.click();
            WebElement field = driver.findElement(By.name("grade"));
            field.clear();
            field.sendKeys("C");
            driver.findElement(By.name("saver")).click();
        }
        Thread.sleep(SLEEP_DURATION);

        String message = driver.findElement(By.name("messager")).getText();
        assertEquals("Grade saved", message);

    }
}
