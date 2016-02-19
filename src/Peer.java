import java.io.File;
import java.util.ArrayList;

//Represents full peer, manages both server and client threads
class Peer implements Runnable {
	
	DownloadData my_dd = null;
	File ddir = null;
	NFO nfo = null;
	ArrayList<Thread> peer_servers = null;
	ArrayList<Thread> peer_clients = null;
	
	Peer(File ddir) {
		File nfo_file = new File(ddir,"NFO.dat");
		if (!nfo_file.isFile()) {
			System.out.println("NFO file cannot be found.");
			System.exit(1);
		}
		nfo = NFO.loadNFO(nfo_file);
		my_dd = new DownloadData(nfo);
		peer_servers = new ArrayList<Thread>();
		peer_clients = new ArrayList<Thread>();
		this.ddir = ddir;
	}
	
	synchronized void addPeerServer(String host,int port,boolean serverMode) {
		Thread pit = new Thread(new PeerServer(my_dd,nfo,host,port,serverMode));
		peer_servers.add(pit);
	}
	
	synchronized void addPeerClient(String rhost, int rport) {
		Thread pit = new Thread(new PeerClient(my_dd,nfo,rhost,rport));
		peer_clients.add(pit);
	}
	
	synchronized void cleanPeerThreads() {
		ArrayList<Thread> new_peer_servers = new ArrayList<Thread>();
		for (int i = 0; i < peer_servers.size(); i++) {
			if (peer_servers.get(i).isAlive()) {
				new_peer_servers.add(peer_servers.get(i));
			}
		}
		peer_servers = new_peer_servers;
		ArrayList<Thread> new_peer_clients = new ArrayList<Thread>();
		for (int i = 0; i < peer_clients.size(); i++) {
			if (peer_clients.get(i).isAlive()) {
				new_peer_clients.add(peer_clients.get(i));
			}
		}
		peer_clients = new_peer_clients;
	}
	
	public static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// Ignored
		}
	}
	
	synchronized void startPeerThreads() {
		for (Thread t : peer_servers) {
			t.start();
		}
		for (Thread t: peer_clients) {
			t.start();
		}
	}

	@Override
	public void run() {
		startPeerThreads();
		while ((peer_servers!=null && peer_servers.size() > 0) ||
				(peer_clients!=null && peer_clients.size() > 0)) {
			cleanPeerThreads();
			sleep(2000);
		} //Wait until threads are all done, then piece the file together
		System.out.println("Server and client threads have terminated. Rejoin file.");
		FileChunker.rejoin(ddir);
	}

}
