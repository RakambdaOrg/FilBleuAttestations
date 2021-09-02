package fr.raksrinana.filbleuattestations;

import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.nio.file.Path;

@NoArgsConstructor
@Getter
@Command(name = "filbleuattestations", mixinStandardHelpOptions = true)
public class CLIParameters{
	@Option(names = {
			"-c",
			"--config"
	},
			description = "The path to the configuration file",
			required = true)
	public Path configurationFile;
}

