package fr.rakambda.filbleuattestations;

import fr.rakambda.filbleuattestations.config.Card;
import fr.rakambda.filbleuattestations.config.Configuration;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Objects;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.*;
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
				var username = $(id("username-field"));
				username.click();
				username.setValue(configuration.getEmail());
				
				var password = $(id("password-field"));
				password.click();
				password.setValue(configuration.getPassword());
				
				$(id("login-button")).click();
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
		$$("li[data-card-uid]").asDynamicIterable().stream()
			.filter(e -> Objects.equals(e.attr("data-card-uid"), card.getUid()))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Failed to find card " + card.getId()))
			.click();
		var espacePerso = $(className("espace-perso__factu"));
		if(!espacePerso.exists()) {
			log.warn("Billing area not visible");
			return;
		}
		
		espacePerso
				.findAll(className("list-files__link"))
				.asDynamicIterable()
				.stream()
				.filter(e -> !e.has(cssClass("sr-only")))
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
									MessageFormat.format("Attestation FilBleu de {0}", attestationName),
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
