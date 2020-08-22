package fr.raksrinana.filbleuattestations;

import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine;
import java.nio.file.Path;

@NoArgsConstructor
@Getter
@CommandLine.Command(name = "filbleuattestations", mixinStandardHelpOptions = true)
public class CLIParameters{
	@CommandLine.Option(names = {
			"-c",
			"--config"
	},
			description = "The path to the configuration file",
			required = true)
	public Path configurationFile;
}

