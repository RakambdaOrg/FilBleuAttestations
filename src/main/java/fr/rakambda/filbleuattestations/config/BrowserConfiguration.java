package fr.rakambda.filbleuattestations.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
public class BrowserConfiguration{
	@Nullable
	@JsonProperty("binary")
	private String binary;
	@JsonProperty("headless")
	private boolean headless = false;
	@NonNull
	@JsonProperty("driver")
	private Driver driver = Driver.CHROME;
	@Nullable
	@JsonProperty("remoteHost")
	private String remoteHost;
}
