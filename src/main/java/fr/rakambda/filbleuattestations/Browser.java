package fr.rakambda.filbleuattestations;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import fr.rakambda.filbleuattestations.config.BrowserConfiguration;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import static com.codeborne.selenide.Selenide.closeWebDriver;

@Log4j2
public class Browser {
    private static WebDriver driver;

    @NotNull
    public static WebDriver setup(BrowserConfiguration configuration) {
        Configuration.headless = configuration.isHeadless();
        var driver = getDriver(configuration);
        WebDriverRunner.setWebDriver(driver);
        return driver;
    }

    public static void close() {
        closeWebDriver();
        driver = null;
    }

    private static WebDriver getDriver(@NotNull BrowserConfiguration config) {
        if (Objects.isNull(driver)) {
            driver = switch (config.getDriver()) {
                case EDGE -> getEdgeDriver();
                case CHROME -> getChromeDriver(config);
                case FIREFOX -> getFirefoxDriver();
                case REMOTE -> getRemoteDriver(config);
            };
        }

        return driver;
    }

    @NotNull
    private static FirefoxDriver getFirefoxDriver() {
        var firefoxOptions = new FirefoxOptions();
        return new FirefoxDriver(firefoxOptions);
    }

    @NotNull
    private static ChromeDriver getChromeDriver(@NotNull BrowserConfiguration configuration) {
        var options = new ChromeOptions();

        Optional.ofNullable(configuration.getBinary())
                .ifPresent(binary -> options.setBinary(Paths.get(binary).toFile()));
        if (configuration.isHeadless()){
			options.addArguments("--headless=new");
		}
        return new ChromeDriver(options);
    }

    @SneakyThrows
    @NotNull
    private static RemoteWebDriver getRemoteDriver(@NotNull BrowserConfiguration configuration) {
        var options = new ChromeOptions();
        options.addArguments("--disable-dev-shm-usage");
        return new RemoteWebDriver(new URL(configuration.getRemoteHost()), options);
    }

    @NotNull
    private static EdgeDriver getEdgeDriver() {
        var options = new EdgeOptions();
        return new EdgeDriver(options);
    }
}
