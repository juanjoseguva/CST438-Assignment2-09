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
        ops.addArguments("--no-sandbox");

        //Start driver
        driver = new ChromeDriver(ops);

        driver.get(reactURL);
        //Short wait to allow page to download
        Thread.sleep(SLEEP_DURATION);

    }


    @AfterEach
    public void destroyDriver() {
        if (driver != null) {
            //Quit driver
            driver.close();
            driver.quit();
            driver = null;
        }
    }

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
        Thread.sleep(SLEEP_DURATION);
        String message = driver.findElement(By.id("editMessage")).getText();
        assertEquals("Grades saved!", message);
        driver.findElement(By.id("closeGrades")).click();



    }

    @Test
    public void systemTestAddAssignment() throws Exception{
        //Adds an assignment for cst363, Spring 2024
        driver.findElement(By.id("year")).sendKeys("2024");
        driver.findElement(By.id("semester")).sendKeys("Spring");
        driver.findElement(By.linkText("Show Sections")).click();
        Thread.sleep(SLEEP_DURATION);

        //Check for cst363, view assignments
        try{
            while(true){
                WebElement row363 = driver.findElement(By.xpath("//tr[td='cst363']"));
                List<WebElement> links = row363.findElements(By.tagName("a"));
                assertEquals(2, links.size());
                //View Assignments is the second link
                links.get(1).click();
                Thread.sleep(SLEEP_DURATION);
            }
        } catch (NoSuchElementException e){
            //do nothing about it
        }

        //Add assginment
        driver.findElement(By.id("addAssignment")).click();
        Thread.sleep(SLEEP_DURATION);

        //populate data fields
        driver.findElement(By.id("addTitle")).sendKeys("db assignment 3 [TEST]");
        WebElement dueDateInput = driver.findElement(By.xpath("//input[@name='dueDate']"));
        Actions actions = new Actions(driver);

// Click on the input field to activate it
        actions.moveToElement(dueDateInput).click().perform();

// Send the year


// Move the cursor to the month section
        actions.sendKeys(Keys.LEFT).perform();
        actions.sendKeys(Keys.LEFT).perform();
        actions.sendKeys("02").perform();
        actions.sendKeys("22").perform();
        actions.sendKeys("2024").perform();
        actions.sendKeys(Keys.TAB).perform();
        driver.findElement(By.id("save")).click();
        Thread.sleep(SLEEP_DURATION);

        //Check to see if the assignment was added then delete
        WebElement row363 = driver.findElement(By.xpath("//tr[td='db assignment 3 [TEST]']"));
        List<WebElement> buttons = row363.findElements(By.tagName("button"));
        assertEquals(3, buttons.size());
        buttons.get(2).click();
        Thread.sleep(SLEEP_DURATION);
        List<WebElement> confirmButtons = driver
                .findElement(By.className("react-confirm-alert-button-group"))
                .findElements(By.tagName("button"));
        assertEquals(2, confirmButtons.size());
        confirmButtons.get(0).click();
        Thread.sleep(SLEEP_DURATION);

        //Check to make sure the assignment is deleted
        assertThrows(NoSuchElementException.class, () ->
                driver.findElement(By.xpath("//tr[td='db assignment 3 [TEST]']")));
    }

}
