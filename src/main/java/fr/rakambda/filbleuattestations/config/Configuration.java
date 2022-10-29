package fr.rakambda.filbleuattestations.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
@NoArgsConstructor
@Getter
public class Configuration{
	private static final ObjectReader objectReader;
	private static final ObjectWriter objectWriter;
	@JsonProperty("email")
	private String email;
	@JsonProperty("password")
	private String password;
	@JsonProperty("cards")
	private Set<Card> cards;
	@JsonProperty("mail")
	private Mail mail;
	@JsonProperty("browser")
	private BrowserConfiguration browser = new BrowserConfiguration();
	
	@NonNull
	public static Optional<Configuration> loadConfiguration(@NonNull final Path path){
		if(Files.isRegularFile(path)){
			try(final var fis = Files.newBufferedReader(path)){
				return Optional.ofNullable(objectReader.readValue(fis));
			}
			catch(final IOException e){
				log.error("Failed to read settings in {}", path, e);
			}
		}
		return Optional.empty();
	}
	
	public static void saveConfiguration(@NonNull final Path path, @NonNull Configuration configuration){
		try{
			objectWriter.writeValueAsString(configuration);
			objectWriter.writeValue(path.toFile(), configuration);
			log.info("Wrote settings to {}", path);
		}
		catch(final IOException e){
			log.error("Failed to write settings to {}", path, e);
		}
	}
	
	static{
		final var mapper = new ObjectMapper();
		mapper.setVisibility(mapper.getSerializationConfig()
				.getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectReader = mapper.readerFor(Configuration.class);
		objectWriter = mapper.writerFor(Configuration.class);
	}
}