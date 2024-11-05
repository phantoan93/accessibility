package accessibility;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Why trust axe-core?
 *  https://www.deque.com/axe/axe-core/
 *  Reference: https://docs.deque.com/devtools-for-web/4/en/java-test-selenium
 *  url list in src/main/resources/url.txt
 *  Run test and check result in accessibility_report.json
 */

public class AccessibilityTest {
    private WebDriver driver;
    private List<String> urls;

    @BeforeClass
    public void setUp() throws IOException {
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--start-maximized");
        chromeOptions.addArguments("--disable-popup-blocking");
        driver = new ChromeDriver(chromeOptions);
        urls = readUrlsFromFile("src/main/resources/url.txt"); // File chứa danh sách các URL
    }

    @Test
    public void testAccessibility() throws IOException, InterruptedException {
        JSONArray resultsArray = new JSONArray();

        for (String url : urls) {
            driver.get(url);
            Thread.sleep(2000);
            String axeScript = new String(Files.readAllBytes(Paths.get("src/main/resources/axe.min.js")));
            ((JavascriptExecutor) driver).executeScript(axeScript);

            String axeResult = (String) ((JavascriptExecutor) driver).executeScript(
                    "return axe.run().then(results => { return JSON.stringify(results); });");

            JSONObject responseJSON = new JSONObject(axeResult);
            JSONArray violations = responseJSON.getJSONArray("violations");

            // Ghi thời gian kết thúc
            Instant endTime = Instant.now();
            JSONObject result = new JSONObject();
            result.put("url", url);
            result.put("totalViolations", violations.length()); // Tổng số vi phạm
            result.put("violations", violations);
            result.put("testingDatetime", endTime);
            resultsArray.put(result);
        }

        // Ghi kết quả vào file JSON
        try (FileWriter fileWriter = new FileWriter("accessibility_report.json")) {
            fileWriter.write(resultsArray.toString(4)); // Ghi với indent 4 cho dễ đọc
        }
    }

    @AfterClass
    public void tearDown() {
        driver.quit();
    }

    private List<String> readUrlsFromFile(String filePath) throws IOException {
        List<String> urls = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                urls.add(line.trim()); // Thêm từng URL vào danh sách
            }
        }
        return urls;
    }
}

