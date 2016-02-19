
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

//Arguments for running in chunk mode
@Parameters(commandDescription = "Break an input file into chunks")
class ChunkCommand {
	
	@Parameter(names = "-cs", description = "Size of each chunk for the chunking")
	private Integer cs = 100;
	
	@Parameter(names = "-dir", description = "Output directory", required=true)
	private String outputDirectory;
	
	@Parameter(names = "-file", description = "Input file to be chunked", required=true)
	private String file;

	int getChunkSize() {
		return cs;
	}
	
	String getFile() {
		return file;
	}
	
	String getOutputDirectory() {
		return outputDirectory;
	}
	
}
