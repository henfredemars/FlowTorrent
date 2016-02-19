import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

//Configuration of the peer if not using the command line options
class NetworkConfig {

	ArrayList<Integer> peerIndices;
	ArrayList<Integer> portNumbers;
	ArrayList<Integer> peerNeighborOnes;
	ArrayList<Integer> peerNeighborTwos;
	
	NetworkConfig(String path) {
		File f = new File(path);
		Scanner s = null;
		try {
			s = new Scanner(f);
		} catch (FileNotFoundException e) {
			System.out.println("Failed to open configuration file");
			e.printStackTrace();
		}
		peerIndices = new ArrayList<Integer>();
		portNumbers = new ArrayList<Integer>();
		peerNeighborOnes = new ArrayList<Integer>();
		peerNeighborTwos = new ArrayList<Integer>();
		while (s!= null && s.hasNextInt()) {
			peerIndices.add(s.nextInt());
			portNumbers.add(s.nextInt());
			peerNeighborOnes.add(s.nextInt());
			peerNeighborTwos.add(s.nextInt());
		}
		System.out.println(String.format("Read %d peer information units.",peerIndices.size()));
	}
	
	int getPortNumber(int peerNumber) {
		int row = peerIndices.indexOf(peerNumber);
		if (row==-1) System.out.println(String.format(
				"getPortNumber: Peer %s does not exist in table.",peerNumber));
		return portNumbers.get(row);
	}
	
	int getNeighborOne(int peerNumber) {
		int row = peerIndices.indexOf(peerNumber);
		if (row==-1) System.out.println(String.format(
				"getNeighborOne - Peer %s does not exist in table.",peerNumber));
		return peerNeighborOnes.get(row);
	}
	
	int getNeighborTwo(int peerNumber) {
		int row = peerIndices.indexOf(peerNumber);
		if (row==-1) System.out.println(String.format(
				"getNeighborTwo - Peer %s does not exist in table.",peerNumber));
		return peerNeighborTwos.get(row);
	}
	
}
