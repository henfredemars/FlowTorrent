
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

//Arguments for running in chunk mode
@Parameters(commandDescription = "Reconstruct an input file from its chunks")
class UnchunkCommand {
	
	@Parameter(names = "-dir", description = "Input directory", required=true)
	private String inputDirectory;
	
	String getInputDirectory() {
		return inputDirectory;
	}
	
}
