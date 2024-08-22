package com.goldoogi.api_communication.service.impl;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.goldoogi.api_communication.dto.request.board.DCPostRequestDto;
import com.goldoogi.api_communication.dto.response.ResponseDto;
import com.goldoogi.api_communication.dto.response.board.DCPostResponseDto;
import com.goldoogi.api_communication.entity.DCPostEntity;
import com.goldoogi.api_communication.repository.DCPostRepository;
import com.goldoogi.api_communication.service.TestService;

import lombok.RequiredArgsConstructor;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private DCPostRepository dcPostRepository;

    private HttpClient httpClient;
    private CookieManager cookieManager;
    public static String SITE_KEY = "6Lc-Fr0UAAAAAOdqLYqPy53MxlRMIXpNXFvBliwI";
    @Value("${API_KEY}")
    private static String API_KEY;

    // constructor for initial setup of httpClient
    public TestServiceImpl() {
        this.cookieManager = new CookieManager();
        this.cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        this.httpClient = HttpClient.newBuilder()
                                    .cookieHandler(cookieManager)
                                    .build();
    }
    
    @Override
    public ResponseEntity<? super DCPostResponseDto> postBoard(DCPostRequestDto dto) {

        String SITE_URL = "https://gall.dcinside.com/board/write/?id=";

        try {
            // access to writing page
            HttpRequest getPageRequest = HttpRequest.newBuilder()
                                                    .uri(URI.create(SITE_URL + dto.getGallery()))
                                                    .GET()
                                                    .build();

            HttpResponse getPageResponse = httpClient.send(getPageRequest, HttpResponse.BodyHandlers.ofString());

            if (getPageResponse.statusCode() != 200) {
                return DCPostResponseDto.notServerWorking();
            }

            // use CapSolver to solve reCAPTCHA
            String token = removeCAPTCHA(SITE_URL + dto.getGallery());

            // extract dynamic values from the page
            String responseBody = getPageResponse.body().toString();
            Map<String, String> formData = extractFormData(responseBody);
            formData.put("subject", dto.getTitle());
            formData.put("password", dto.getPassword());
            formData.put("memo", URLEncoder.encode(dto.getContent(), StandardCharsets.UTF_8));
            formData.put("g-recaptcha-token", token);

            StringBuilder formBody = new StringBuilder();
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                if (formBody.length() > 0) {
                    formBody.append("&");
                }
                formBody.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                formBody.append("=");
                formBody.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            printPrettierString(formBody);

            // create the POST request for block packet submission
            HttpRequest postBlockRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://gall.dcinside.com/block/block/"))
                    // .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Content-Type", "text/html")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
                    .header("Referer", SITE_URL + dto.getGallery())
                    .POST(HttpRequest.BodyPublishers.ofString(formBody.toString()))
                    .build();
            // create the POST request for article submission
            HttpRequest postSubmitRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://gall.dcinside.com/board/forms/article_submit"))
                    // .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Content-Type", "text/html")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
                    .header("Referer", SITE_URL + dto.getGallery())
                    .POST(HttpRequest.BodyPublishers.ofString(formBody.toString()))
                    .build();

            // send the POST request and handle the response
            HttpResponse postBlolckResponse = httpClient.send(postBlockRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println(postBlolckResponse.headers());
            System.out.println(postBlolckResponse.body());
            System.out.println(postBlolckResponse.statusCode());
            HttpResponse postSubmitResponse = httpClient.send(postSubmitRequest, HttpResponse.BodyHandlers.ofString());
            // System.out.println(postSubmitResponse.headers());
            // System.out.println(postSubmitResponse.body());
            // System.out.println(postSubmitResponse.statusCode());

            if (postSubmitResponse.statusCode() != 200) {
                System.out.println(postSubmitResponse.body());
                System.out.println("form data for post request may be wrong!");
                return DCPostResponseDto.notServerWorking();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return DCPostResponseDto.databaseError();
        }
        
        return DCPostResponseDto.success();
    }

    private void printPrettierString(StringBuilder formBody) {
        String stringFormData = formBody.toString();
        String[] params = stringFormData.split("&");
        
        StringBuilder prettierString = new StringBuilder();
        for (String param : params) {
            prettierString.append(param).append("\n&\n");
        }

        if (prettierString.length() > 0) {
            prettierString.setLength(prettierString.length() - 2);
        }

        System.out.println(prettierString.toString());
    }

    private Map<String, String> extractFormData(String html) {
        Map<String, String> extractDynamicValues = new HashMap<>();

        // extractDynamicValues.put("block_key", extractValueFromHtml(html, "id=\"block_key\" value=\""));
        extractDynamicValues.put("board_id", extractValueFromHtml(html, "id=\"id\" value=\""));
        // extractDynamicValues.put("_GALLTYPE_", extractValueFromHtml(html, "id=\"_GALLTYPE_\" value=\""));
        // extractDynamicValues.put("gallery_no", extractValueFromHtml(html, "id=\"gallery_no\" value=\""));
        // extractDynamicValues.put("r_key", extractValueFromHtml(html, "id=\"r_key\" value=\""));
        // extractDynamicValues.put("upload_status", extractValueFromHtml(html, "id=\"upload_status\" value=\""));
        // extractDynamicValues.put("user_ip", extractValueFromHtml(html, "id=\"user_ip\" value=\""));
        // extractDynamicValues.put("headtext", extractValueFromHtml(html, "id=\"headtext\" value=\""));
        // extractDynamicValues.put("use_html", extractValueFromHtml(html, "id=\"use_html\" value=\""));
        // extractDynamicValues.put("c_r_k_x_z", extractValueFromHtml(html, "id=\"c_r_k_x_z\" value=\""));
        // extractDynamicValues.put("ci_t", extractValueFromHtml(html, "id=\"ci_t\" value=\""));
        // extractDynamicValues.put("clickbutton", extractValueFromHtml(html, "id=\"clickbutton\" value=\""));
        // extractDynamicValues.put("tempIdx", extractValueFromHtml(html, "id=\"tempIdx\" value=\""));
        // extractDynamicValues.put("use_headtext", extractValueFromHtml(html, "id=\"use_headtext\" value=\""));
        // extractDynamicValues.put("poll", extractValueFromHtml(html, "id=\"poll\" value=\""));
        // extractDynamicValues.put("service_code", extractValueFromHtml(html, "name=\"service_code\" value=\""));
        // extractDynamicValues.put("use_gall_nick", extractValueFromHtml(html, "id=\"use_gall_nick\" value=\""));
        // extractDynamicValues.put("mode", extractValueFromHtml(html, "id=\"mode\" value=\""));
        // extractDynamicValues.put("movieIdx", extractValueFromHtml(html, "id=\"movieIdx\" value=\""));
        // extractDynamicValues.put("series_title", extractValueFromHtml(html, "id=\"series_title\" value=\""));
        // extractDynamicValues.put("series_data", extractValueFromHtml(html, "id=\"series_data\" value=\""));
        // extractDynamicValues.put("headTail", extractValueFromHtml(html, "id=\"headTail\" value=\""));
        // extractDynamicValues.put("nfteasy", extractValueFromHtml(html, "id=\"nfteasy\" value=\"false"));

        return extractDynamicValues;
    }

    private String extractValueFromHtml(String html, String prefix) {

        int startIndex = html.indexOf(prefix) + prefix.length();
        if (startIndex == -1) return "";
        int endIndex = html.indexOf("\"", startIndex);
        
        return html.substring(startIndex, endIndex);
    }

    private String removeCAPTCHA(String url) throws InterruptedException, IOException {
        JSONObject param = new JSONObject();
        Map<String, Object> task = new HashMap<>();
        task.put("type", "ReCaptchaV3TaskProxyLess");
        task.put("websiteKey", SITE_KEY);
        task.put("websiteURL", url);
        param.put("clientKey", API_KEY);
        param.put("task", task);
        String taskId = createTask(param);

        if (Objects.equals(taskId, "")) {
            System.out.println("Failed to create task");
        }
        System.out.println("Got taskId: "+taskId+" / Getting result...");
        String token = "";
        while (true){
            Thread.sleep(1000);
            token = getTaskResult(taskId);
            if (Objects.equals(token, null)) {
                continue;
            }
            break;
        }
        return token;
    }

    public static String getTaskResult(String taskId) throws IOException, InterruptedException {
        JSONObject param = new JSONObject();
        param.put("clientKey", API_KEY);
        param.put("taskId", taskId);
        String parsedJsonStr = requestPost("https://api.capsolver.com/getTaskResult", param);
        JSONObject responseJson = new JSONObject(parsedJsonStr);

        String status = responseJson.getString("status");
        System.out.println(status);
        if (status.equals("ready")) {
            JSONObject solution = responseJson.getJSONObject("solution");
            return solution.get("gRecaptchaResponse").toString();
        }
        if (status.equals("failed") || responseJson.getInt("errorId")!=0) {
            System.out.println("Solve failed! response: "+parsedJsonStr);
            return "error";
        }
        return null;
    }

    public static String createTask(JSONObject param) throws IOException {
        String parsedJsonStr = requestPost("https://api.capsolver.com/createTask", param);
        JSONObject responseJson = new JSONObject(parsedJsonStr);
        return responseJson.get("taskId").toString();
    }

    public static String requestPost(String url, JSONObject param) throws IOException {
        URL ipapi = new URL(url);
        HttpURLConnection c = (HttpURLConnection) ipapi.openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);

        OutputStream os = null;
        os = c.getOutputStream();
        os.write(param.toString().getBytes("UTF-8"));

        c.connect();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(c.getInputStream())
        );
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null)
        { sb.append(line); }

        return sb.toString();
    }
}