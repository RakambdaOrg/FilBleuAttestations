package fr.raksrinana.filbleuattestations;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.codeborne.selenide.WebDriverRunner;
import fr.raksrinana.filbleuattestations.config.Configuration;
import fr.raksrinana.utils.mail.MailUtils;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import java.text.MessageFormat;
import static com.codeborne.selenide.Selenide.*;
import static org.openqa.selenium.By.*;

@Slf4j
public class Main{
	public static void main(String[] args){
		final var parameters = new CLIParameters();
		try{
			JCommander.newBuilder().addObject(parameters).build().parse(args);
		}
		catch(final ParameterException e){
			log.error("Failed to parse arguments", e);
			e.usage();
			return;
		}
		com.codeborne.selenide.Configuration.headless = true;
		var firefoxOptions = new FirefoxOptions();
		WebDriverRunner.setWebDriver(new FirefoxDriver(firefoxOptions));
		Configuration.loadConfiguration(parameters.getConfigurationFile()).ifPresentOrElse(configuration -> {
			log.info("Configuration loaded");
			final var mailSession = MailUtils.getMailSession(configuration.getMail().getHost(), configuration.getMail().getPort(), configuration.getMail().getUsername(), configuration.getMail().getPassword());
			log.info("Created mail session");
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
				$(className("attestation")).findAll(By.tagName("li")).stream().map(elem -> elem.find(By.tagName("a"))).forEach(attestation -> {
					if(!card.getDownloaded().contains(attestation.getText())){
						log.info("Processing attestation '{}'", attestation.getText());
						try{
							var attestationFile = attestation.download(30000);
							attestationFile.deleteOnExit();
							log.info("Downloaded to {}", attestationFile);
							log.info("Sending mail");
							MailUtils.sendMail(mailSession, configuration.getMail().getFromEmail(), configuration.getMail().getFromName(), message -> {
								try{
									message.addRecipients(Message.RecipientType.TO, card.getRecipientEmail());
									message.setSubject(MessageFormat.format("Attestation FilBleu {0}", attestation.getText()));
									BodyPart messageBodyPart = new MimeBodyPart();
									Multipart multipart = new MimeMultipart();
									messageBodyPart.setText(MessageFormat.format("Attestation FilBleu du {0}", attestation.getText()));
									DataSource source = new FileDataSource(attestationFile);
									messageBodyPart.setDataHandler(new DataHandler(source));
									messageBodyPart.setFileName(attestationFile.getName());
									multipart.addBodyPart(messageBodyPart);
									message.setContent(multipart);
								}
								catch(MessagingException e){
									log.error("Failed to prepare mail");
									throw new RuntimeException(e);
								}
							});
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
}
