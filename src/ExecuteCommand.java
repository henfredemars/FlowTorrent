
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

//Arguments for running in execute mode
@Parameters(commandDescription = "Run an instance of Peer")
class ExecuteCommand {
	
	@Parameter(names = "-host", description = "Host address to bind our ports")
	private String host = "127.0.0.1";
	
	@Parameter(names = "-rhost", description = "Remote peer address")
	private String rhost;
	
	@Parameter(names = "-port", description = "Peer communication port")
	private Integer port = 5154;
	
	@Parameter(names = "-rport", description = "Remote peer port")
	private Integer rport = 5154;
	
	@Parameter(names = "-dir", description = "Output directory", required=true)
	private String outputDirectory;
	
	@Parameter(names = "-config", description = "Configuration file")
	private String configFile;
	
	@Parameter(names = {"-pn","peerNumber"}, description = "Index of peer for config")
	private Integer peerNumber = -1;
	
	@Parameter(names = "-server", description = "Only serve one copy of each piece")
	private Boolean serverMode = false;

	String getHost() {
		return host;
	}
	
	String getRHost() {
		return rhost;
	}
	
	int getPort() {
		return port;
	}
	
	int getRPort() {
		return rport;
	}
	
	String getOutputDirectory() {
		return outputDirectory;
	}
	
	String getConfigFile() {
		return configFile;
	}
	
	int getPeerNumber() {
		return peerNumber;
	}
	
	boolean getServerMode() {
		return serverMode;
	}
	
}
