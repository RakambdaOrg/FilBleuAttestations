package fr.raksrinana.filbleuattestations;

import com.codeborne.selenide.WebDriverRunner;
import fr.raksrinana.filbleuattestations.config.Configuration;
import fr.raksrinana.filbleuattestations.config.Mail;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import picocli.CommandLine;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Properties;
import static com.codeborne.selenide.Selenide.*;
import static org.openqa.selenium.By.*;

@Log4j2
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
			var mailSession = buildMailSession(configuration.getMail());
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
									
									sendMail(mailSession, configuration, card.getRecipientEmail(),
											MessageFormat.format("Attestation FilBleu {0}", attestation.getText()),
											MessageFormat.format("Attestation FilBleu du {0}", attestation.getText()),
											attestationFile);
									
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
	
	private static void sendMail(Session session, Configuration configuration, String to, String subject, String body, File attachment) throws MessagingException, IOException{
		var message = new MimeMessage(session);
		message.setFrom(new InternetAddress(configuration.getMail().getFromName()));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		message.setSubject(subject);
		
		Multipart multipart = new MimeMultipart();
		
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(body, "text/plain");
		multipart.addBodyPart(mimeBodyPart);
		
		if(attachment != null && attachment.exists()){
			MimeBodyPart attachmentBodyPart = new MimeBodyPart();
			attachmentBodyPart.attachFile(attachment);
			multipart.addBodyPart(attachmentBodyPart);
		}
		
		message.setContent(multipart);
		
		Transport.send(message);
	}
	
	private static Session buildMailSession(Mail mail){
		Properties prop = new Properties();
		prop.put("mail.smtp.auth", true);
		prop.put("mail.smtp.starttls.enable", "true");
		prop.put("mail.smtp.host", mail.getHost());
		prop.put("mail.smtp.port", mail.getPort());
		prop.put("mail.smtp.ssl.trust", mail.getHost());
		
		return Session.getInstance(prop, new Authenticator(){
			@Override
			protected PasswordAuthentication getPasswordAuthentication(){
				return new PasswordAuthentication(mail.getUsername(), mail.getPassword());
			}
		});
	}
	
	private static WebDriver getDriver(){
		// var firefoxOptions = new FirefoxOptions();
		// var driver = new FirefoxDriver(firefoxOptions);
		
		var chromeOptions = new ChromeOptions();
		chromeOptions.setHeadless(true);
		var driver = new ChromeDriver(chromeOptions);
		
		return driver;
	}
}
