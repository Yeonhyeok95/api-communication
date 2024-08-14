package com.goldoogi.api_communication.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;

import com.goldoogi.api_communication.dto.request.board.DCPostRequestDto;
import com.goldoogi.api_communication.dto.response.ResponseDto;
import com.goldoogi.api_communication.dto.response.board.DCPostResponseDto;
import com.goldoogi.api_communication.entity.DCPostEntity;
import com.goldoogi.api_communication.repository.DCPostRepository;
import com.goldoogi.api_communication.service.DCPostService;

import io.github.bonigarcia.wdm.WebDriverManager;


@Service
public class DCPostServiceImpl implements DCPostService {

    @Autowired
    private DCPostRepository dcPostRepository;

    WebDriver driver;

    @Override
    public ResponseEntity<? super DCPostResponseDto> postBoard(DCPostRequestDto dto) {

        try {
            String chromePath = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
            String userDataDir = "C:\\chromeTemp";
            ProcessBuilder processBuilder = new ProcessBuilder(chromePath, "--remote-debugging-port=9222", "--user-data-dir=" + userDataDir);
            processBuilder.start();
    
            WebDriverManager.chromedriver().setup();
    
            ChromeDriverService chromeService = new ChromeDriverService.Builder().usingAnyFreePort().build();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-popup-blocking");
            options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
            
            this.driver = new ChromeDriver(chromeService, options);
            this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));

            String URL = "https://gall.dcinside.com/board/write/?id=" + dto.getGallery();
            driver.get(URL);

            // making entity to save post into DB
            String title = dto.getTitle();
            String password = dto.getPassword();
            String content = dto.getContent();
            DCPostEntity dcPostEntity = new DCPostEntity();
            dcPostEntity.setTitle(title);
            dcPostEntity.setPassword(password);
            dcPostEntity.setContent(content);

            // this is for async functioning in case the other user calling this API
            isDuplicatePost(dcPostEntity).thenAccept(isDuplicate -> {
                if (isDuplicate) {
                    delayTask(1000);
                    System.out.println("Duplicated post existed in DB!! It will be posted in 10 mins");
                } 
                dcPostRepository.save(dcPostEntity);
                System.out.println("requestBody is stored in DB");
                inputTitlePwIntoDcGallery(title, password);
                inputContentIntoNestedDOM(content);
                clickSubmitButton();
            });

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseDto.databaseError();
        }
        System.out.println("all done!");
        return DCPostResponseDto.success();
    }

    private CompletableFuture<Boolean> isDuplicatePost(DCPostEntity dcPostEntity) {
        // bring 1,000 recent posts from DB
        List<DCPostEntity> recentPosts = dcPostRepository.findTop1000ByOrderByCreatedAtDesc();
        // compare each post with newerly requested post
        for (DCPostEntity post : recentPosts) {
            // check if duplicated posts exist
            if (post.getTitle().equals(dcPostEntity.getTitle()) &&
                post.getContent().equals(dcPostEntity.getContent())) {
                return CompletableFuture.completedFuture(true);
            }
        }
        // if no duplicated posts, return false
        return CompletableFuture.completedFuture(false);
    }

    protected void clickSubmitButton() {
        long randomSec = (long) (3000 * Math.random());
        WebElement checkInput = driver.findElement(By.cssSelector("button.btn_blue.btn_svc.write"));
        try {
            delayTask(randomSec);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            delayTask(randomSec);
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            delayTask(randomSec);
            js.executeScript("document.querySelector('iframe[title=\"reCAPTCHA\"]').style.display='none';");
            delayTask(randomSec);

            new Actions(driver)
                .moveToElement(checkInput)
                .click(checkInput)
                .perform();

            checkInput.click();
        } catch (Exception e) {
            e.printStackTrace();
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkInput);
            new Actions(driver)
                .moveToElement(checkInput, 0, 18)
                .click();
            delayTask(3000);
            new Actions(driver)
                .keyDown(Keys.ENTER)
                .keyUp(Keys.ENTER)
                .perform();
            new Actions(driver)
                .moveToElement(checkInput, 0, 18)
                .click();
        }
        System.out.println("Click submit processed");
    }

    protected void inputTitlePwIntoDcGallery(String title, String password) {
        long randomSec = (long) (3000 * Math.random());
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement subjectInput = driver.findElement(By.id("subject"));

        // for avoiding "Google reCaptCha" technique,
        WebElement avoidCaptCha = driver.findElement(By.cssSelector("ul.tx-bar.tx-bar-right.tx-nav-opt"));

        try {
            new Actions(driver)
                .moveToElement(avoidCaptCha)
                .click(avoidCaptCha);
            delayTask(randomSec);

            new Actions(driver)
                .moveToElement(passwordInput)
                .click();
            passwordInput.clear();
            passwordInput.sendKeys(password);
            delayTask(randomSec);

            new Actions(driver)
                .moveToElement(subjectInput)
                .click();
                subjectInput.sendKeys(title);
            delayTask(randomSec);

            new Actions(driver)
                .moveToElement(avoidCaptCha)
                .click(avoidCaptCha);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void inputContentIntoNestedDOM(String content) {
        long randomSec = (long) (3000 * Math.random());
        try {
            driver.switchTo().frame("tx_canvas_wysiwyg");
            WebElement pTag = driver.findElement(By.xpath("//p"));
            delayTask(randomSec);

            new Actions(driver)
                .moveToElement(pTag)
                .click();
                pTag.sendKeys(content);
            delayTask(randomSec);

            driver.switchTo().defaultContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void randomMouseMove(WebElement clickable) {
        Random random = new Random();

        // 이동할 횟수 (랜덤하게 결정)
        int moveCount = random.nextInt(10) + 5;

        for (int i = 0; i < moveCount; i++) {
            // 랜덤 좌표 생성
            int xOffset = random.nextInt(100) - 50;  // -50부터 +50까지의 오프셋
            int yOffset = random.nextInt(100) - 50;

            // 현재 마우스 위치에서 오프셋만큼 이동
            new Actions(driver)
                .moveByOffset(xOffset, yOffset)
                .moveToElement(clickable)
                .pause(Duration.ofSeconds(1))
                .clickAndHold()
                .pause(Duration.ofSeconds(1))
                .sendKeys("abc")
                .perform();

            // 짧은 지연 추가 (사람의 움직임처럼 보이도록)
            try {
                Thread.sleep(random.nextInt(100) + 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Async
    public void delayTask(long longTypeTime) {
        try {
            Thread.sleep(longTypeTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
