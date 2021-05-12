package ua.co.k.alpine.screenshoter;


import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ScreenShotService {
    
    private final boolean enabled;

    private final String driverPath;

    private final String targetDirectory;

    public ScreenShotService(boolean enabled, String driverPath, String targetDirectory) {
        this.enabled = enabled;
        this.driverPath = driverPath;
        this.targetDirectory = targetDirectory;
        init();
    }
    private WebDriver driver;

    public void init() {
        if (!enabled) {
            return;
        }

        initDriver();
        setTimeout(30, TimeUnit.SECONDS);
    }

    private void initDriver() {
        if (driverPath != null) {
            Path pathToDriver = Paths.get(driverPath);
            if (!Files.exists(pathToDriver)) {
                throw new RuntimeException("is not a valid location of driver, please check it exists: [" + pathToDriver.toAbsolutePath() + "]");
            }
            System.setProperty("webdriver.chrome.driver", pathToDriver.toString());
            // uncomment in case of security restrictions on chromedriver
//            System.setProperty("webdriver.chrome.whitelistedIps", ""); 
        }

        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("force-device-scale-factor=1");
        options.addArguments("high-dpi-support=1");
        options.addArguments("--no-sandbox");
        options.addArguments("--hide-scrollbars");
        // gl stands for "Graphics library"
        // swiftshader is a library for software rendering
        // I believe in alpine chrome it turned on by default
        // options.addArguments("--use-gl=swiftshader");
        // related options:
        //  --ignore-gpu-blacklist
        //  --disable-software-rasterizer
        //  --disable-dev-shm-usage
        //  --disable-gpu
        
        options.setBinary("");
        driver = new ChromeDriver(options);
    }

    /* package */ void setTimeout(int i, TimeUnit unit) {
        driver.manage().timeouts().pageLoadTimeout(i, unit);
        driver.manage().timeouts().implicitlyWait(i, unit);
    }

    public void createImage(String url, String outFilename, int height, int width) throws IOException {
        if(!enabled) {
            log.warn(ScreenShotService.class.getSimpleName() + " is not enabled");
            return;
        }
        createImage(url, outFilename, height, width, 4);
    }

    /* package */ void createImage(String url, String outFilename, int height, int width, int errorsLeft) throws IOException {
        try {
            String fullOutFilename = warnFileExists(outFilename);
            resize(height, width);
            driver.get(url);
            waitForLoad(driver);
            byte[] scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES);
            BufferedImage res = verifyImageSize(scrFile, height, width);
            ImageIO.write(res, "png", touch(fullOutFilename));
            log.info("file created: {}", fullOutFilename);
        } catch (WebDriverException e) {
            log.error("driver problem", e);
            errorsLeft--;
            if (errorsLeft == 0) {
                throw e;
            } else {
                initDriver();
                createImage(url, outFilename, height, width, errorsLeft);
            }
        }
        // WebDriverException
    }

    private static File touch(String strPath) throws IOException {
        Path path = Paths.get(strPath);
        if (Files.exists(path)) {
            Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
        } else {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }
        return path.toFile();
    }

    private BufferedImage verifyImageSize(byte[] input, int height, int width) throws IOException {
        InputStream in = new ByteArrayInputStream(input);
        BufferedImage originalImage = ImageIO.read(in);
        int originalHeight = originalImage.getHeight();
        int originalWidth = originalImage.getWidth();
        int targetWidth = Math.min(originalWidth, width);
        int targetHeight = Math.min(originalHeight, height);
        BufferedImage res;
        if (targetHeight != originalHeight || targetWidth != originalWidth) {
            res = originalImage.getSubimage(0, 0, targetWidth, targetHeight);
        } else {
            res = originalImage;
        }
        return res;
    }

    private void resize(int height, int width) {

        //noinspection unchecked
        ArrayList<Long> padding = (ArrayList<Long>)((JavascriptExecutor) driver).executeScript(
                "return [window.outerWidth-window.innerWidth, window.outerHeight-window.innerHeight];");
        int paddingWidth = padding.get(0).intValue();
        int paddingHeight = padding.get(1).intValue();

        Dimension actual = driver.manage().window().getSize();
        Dimension expected = new Dimension(width + paddingWidth, height + paddingHeight);

        if (!actual.equals(expected)) {
            driver.manage().window().setSize(expected);
            waitForResize(driver, width + paddingWidth, height + paddingHeight);
        }
    }


    private String warnFileExists(String outFilename) {
        Objects.requireNonNull(outFilename);
        Path path = Paths.get(targetDirectory, outFilename);
        if (Files.exists(path)) {
            log.warn("File [" + path.toAbsolutePath() + "]  exists and it will be overridden!");
        }
        return path.toAbsolutePath().toString();
    }

    void waitForLoad(WebDriver driver) {
        new WebDriverWait(driver, 30).until(wd ->
                ((JavascriptExecutor) wd).executeScript("return document.readyState").equals("complete"));
    }

    void waitForResize(WebDriver driver, int width, int height) {
        String jsGetInnerWidth = "return window.innerWidth | 0;";
        String jsGetInnerHeight = "return window.innerHeight | 0;";
        new WebDriverWait(driver, 30).until(wd ->
                !((JavascriptExecutor) wd).executeScript(jsGetInnerWidth).equals(Integer.toString(width)));
        new WebDriverWait(driver, 30).until(wd ->
                !((JavascriptExecutor) wd).executeScript(jsGetInnerHeight).equals(Integer.toString(height)));
    }

}
