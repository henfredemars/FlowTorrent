import java.io.File;

import com.beust.jcommander.JCommander;

//Creates and launches a peer. Peer is server if it happens to have all the chunks
//FlowTorrent is the main class of the program
//The program ends when all peers have disconnected
//Server mode indicates that we only serve each piece exactly once
public class FlowTorrent {
	
	public static void main(String[] args) {
		System.out.println("FlowTorrent starting up!");
		
		//Configure an argument parser
		CommanderMain cm = new CommanderMain();
		ChunkCommand cc = new ChunkCommand();
		UnchunkCommand uc = new UnchunkCommand();
		ExecuteCommand ec = new ExecuteCommand();
		JCommander jc = new JCommander(cm);
		jc.addCommand("chunk", cc);
		jc.addCommand("unchunk", uc);
		jc.addCommand("execute", ec);
		jc.setProgramName("FlowTorrent");
		
		//Parse command line arguments
		jc.parse(args);
		
		//Figure out how we're being used and act accordingly
		//The chunker generates the torrent file
		String command = jc.getParsedCommand();
		if (cm.getHelp() || command==null) {
			jc.usage();
		} else if (command.equals("chunk")) {
			System.out.println("Chunk command selected.");
			FileChunker.split(new File(cc.getFile()), new File(cc.getOutputDirectory()),
					cc.getChunkSize());
		} else if (command.equals("unchunk")) {
			System.out.println("Unchunk command selected.");
			FileChunker.rejoin(new File(uc.getInputDirectory()));
		} else if (command.equals("execute")) {
			System.out.println("Execute selected");
			
			//Parse a configuration file if there was one
			int rport;
			int port;
			String rhost = ec.getRHost();
			String host = ec.getHost();
			String configPath = ec.getConfigFile();
			int peerNumber = ec.getPeerNumber();
			if (configPath!=null && peerNumber!=-1) {
				NetworkConfig nc = new NetworkConfig(configPath);
				int rpeer = nc.getNeighborOne(peerNumber);
				rport = nc.getPortNumber(rpeer);
				port = nc.getPortNumber(peerNumber);
			} else {
				rport = ec.getRPort();
				port = ec.getPort();
			}
			boolean serverMode = ec.getServerMode();
			File ddir = new File(ec.getOutputDirectory());
			Peer peer = new Peer(ddir);
			peer.addPeerServer(host,port,serverMode);
			if (rhost!=null && !serverMode) {
				peer.addPeerClient(rhost,rport);
			}
			peer.run();
		}
		
	}

}
