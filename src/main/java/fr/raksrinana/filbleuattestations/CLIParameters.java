package fr.raksrinana.filbleuattestations;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class CLIParameters{
	@Parameter(names = {"-c", "--config"}, description = "The path to the configuration file", converter = PathConverter.class, required = true)
	@Getter
	public Path configurationFile;
}

