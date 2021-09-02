package fr.raksrinana.filbleuattestations;

import fr.raksrinana.filbleuattestations.config.Card;
import fr.raksrinana.filbleuattestations.config.Configuration;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.By;
import picocli.CommandLine;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.By.*;

@Log4j2
public class Main{
	public static void main(String[] args){
		var parameters = new CLIParameters();
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
		
		Configuration.loadConfiguration(parameters.getConfigurationFile()).ifPresentOrElse(configuration -> {
			var mailer = new Mailer(configuration.getMail());
			Browser.setup(configuration.getBrowser());
			
			try{
				log.info("Configuration loaded");
				
				log.info("Opening page");
				open("https://www.filbleu.fr/espace-perso");
				
				var perturbation = $(className("actperturb"));
				if(perturbation.isDisplayed()){
					log.info("Closing perturbation window");
					perturbation.click();
				}
				
				log.info("Logging in");
				$(id("username")).setValue(configuration.getEmail());
				$(id("password")).setValue(configuration.getPassword());
				$(id("form-login-submit")).find(tagName("button")).click();
				log.info("Logged in");
				
				configuration.getCards().forEach(card -> processCard(mailer, card));
			}
			finally{
				Browser.close();
			}
			Configuration.saveConfiguration(parameters.getConfigurationFile(), configuration);
		}, () -> log.error("Failed to load configuration from {}", parameters.getConfigurationFile()));
	}
	
	private static void processCard(Mailer mailer, Card card){
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
							
							if(mailer.sendMail(card.getRecipientEmail(),
									MessageFormat.format("Attestation FilBleu {0}", attestation.getText()),
									MessageFormat.format("Attestation FilBleu du {0}", attestation.getText()),
									attestationFile)){
								card.getDownloaded().add(attestation.getText());
							}
						}
						catch(Exception e){
							log.error("Failed to send attestation", e);
						}
					}
				});
	}
}
