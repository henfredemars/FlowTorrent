
import com.beust.jcommander.Parameter;

//Arguments applicable to any mode
class CommanderMain {
	
	@Parameter(names = {"-help", "-h", "--help"}, help = true,
			description = "Program usage information (this)")
	private boolean help;
	
	boolean getHelp() {
		return help;
	}

}
