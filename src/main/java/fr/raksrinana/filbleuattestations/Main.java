package fr.raksrinana.filbleuattestations;

import com.codeborne.selenide.WebDriverRunner;
import fr.raksrinana.filbleuattestations.config.Configuration;
import fr.raksrinana.filbleuattestations.config.Mail;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import picocli.CommandLine;
import javax.activation.FileDataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import static com.codeborne.selenide.Selenide.*;
import static org.openqa.selenium.By.*;

@Slf4j
public class Main{
	public static void main(String[] args){
		final var parameters = new CLIParameters();
		var cli = new CommandLine(parameters);
		cli.registerConverter(Path.class, Paths::get);
		cli.setUnmatchedArgumentsAllowed(true);
		try{
			cli.parseArgs(args);
		}
		catch(final CommandLine.ParameterException e){
			log.error("Failed to parse arguments", e);
			cli.usage(System.out);
			return;
		}
		com.codeborne.selenide.Configuration.headless = true;
		WebDriverRunner.setWebDriver(getDriver());
		Configuration.loadConfiguration(parameters.getConfigurationFile()).ifPresentOrElse(configuration -> {
			log.info("Configuration loaded");
			var mailer = buildMailer(configuration.getMail());
			log.info("Created mailer");
			log.info("Opening page");
			open("https://www.filbleu.fr/espace-perso");
			final var perturbation = $(className("actperturb"));
			if(perturbation.isDisplayed()){
				log.info("Closing perturbation window");
				perturbation.click();
			}
			log.info("Logging in");
			$(id("username")).setValue(configuration.getEmail());
			$(id("password")).setValue(configuration.getPassword());
			$(id("form-login-submit")).find(tagName("button")).click();
			log.info("Logged in");
			configuration.getCards().forEach(card -> {
				log.info("Processing card {}", card.getId());
				open("https://www.filbleu.fr/espace-perso/mes-cartes/" + card.getId());
				$(className("attestation"))
						.findAll(By.tagName("li"))
						.stream()
						.map(elem -> elem.find(By.tagName("a")))
						.forEach(attestation -> {
							if(!card.getDownloaded().contains(attestation.getText())){
								log.info("Processing attestation '{}'", attestation.getText());
								try{
									var attestationFile = attestation.download(30000);
									attestationFile.deleteOnExit();
									log.info("Downloaded to {}", attestationFile);
									log.info("Sending mail to {}", card.getRecipientEmail());
									
									var email = EmailBuilder.startingBlank()
											.from(configuration.getMail().getFromName(), configuration.getMail().getFromEmail())
											.to(card.getRecipientEmail())
											.withSubject(MessageFormat.format("Attestation FilBleu {0}", attestation.getText()))
											.withPlainText(MessageFormat.format("Attestation FilBleu du {0}", attestation.getText()))
											.withAttachment(attestationFile.getName(), new FileDataSource(attestationFile))
											.buildEmail();
									
									mailer.sendMail(email);
									log.info("Mail sent");
									card.getDownloaded().add(attestation.getText());
								}
								catch(Exception e){
									log.error("Failed to send attestation", e);
								}
							}
						});
			});
			closeWebDriver();
			Configuration.saveConfiguration(parameters.getConfigurationFile(), configuration);
		}, () -> log.error("Failed to load configuration from {}", parameters.getConfigurationFile()));
	}
	
	private static WebDriver getDriver(){
		// var firefoxOptions = new FirefoxOptions();
		// var driver = new FirefoxDriver(firefoxOptions);
		
		var chromeOptions = new ChromeOptions();
		chromeOptions.setHeadless(true);
		var driver = new ChromeDriver(chromeOptions);
		
		return driver;
	}
	
	private static Mailer buildMailer(Mail mail){
		return MailerBuilder.withSMTPServer(mail.getHost(), mail.getPort(), mail.getUsername(), mail.getPassword())
				.buildMailer();
	}
}
