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
				open("https://www.filbleu.fr/mon-espace-perso");
				
				var perturbation = $(className("actperturb"));
				if(perturbation.isDisplayed()){
					log.info("Closing perturbation window");
					perturbation.click();
				}
				
				log.info("Logging in");
				$(id("username-field")).setValue(configuration.getEmail());
				$(id("password-field")).setValue(configuration.getPassword());
				$(id("login-button")).find(tagName("button")).click();
				log.info("Logged in");
				
				configuration.getCards().forEach(card -> {
					try{
						processCard(mailer, card);
					}catch(Exception e){
						log.error("Failed to process card", e);
					}
				});
			}
			finally{
				Browser.close();
			}
			Configuration.saveConfiguration(parameters.getConfigurationFile(), configuration);
		}, () -> log.error("Failed to load configuration from {}", parameters.getConfigurationFile()));
	}
	
	private static void processCard(Mailer mailer, Card card){
		log.info("Processing card {}", card.getId());
		open("https://www.filbleu.fr/mon-espace-perso/mes-cartes");
		$$("li[data-card-uid]").stream()
			.filter(e -> Objects.equals(e.attr("data-card-uid"), card.getUid()))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Failed to find card " + card.getId()))
			.click();
		$(className(".espace-perso__factu"))
				.findAll(By.cssClass("list-files__link"))
				.stream()
				.filter(e -> !e.hasClass("sr-only"))
				.forEach(attestation -> {
					var attestationName = attestation.getText().strip().toLowerCase();
					if(!card.getDownloaded().contains(attestationName)){
						log.info("Processing attestation '{}'", attestationName);
						try{
							var attestationFile = attestation.download(30000);
							attestationFile.deleteOnExit();
							log.info("Downloaded to {}", attestationFile);
							log.info("Sending mail to {}", card.getRecipientEmail());
							
							if(mailer.sendMail(card.getRecipientEmail(),
									MessageFormat.format("Attestation FilBleu {0}", attestationName),
									MessageFormat.format("Attestation FilBleu du {0}", attestationName),
									attestationFile)){
								card.getDownloaded().add(attestationName);
							}
						}
						catch(Exception e){
							log.error("Failed to send attestation", e);
						}
					}
				});
	}
}
