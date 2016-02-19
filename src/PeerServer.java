import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;


//Serve our pieces to other peers
//We own the listen socket
//Shutdown if no clients connected and one was connected before
class PeerServer implements Runnable {
	
	DownloadData dd = null;
	NFO nfo = null;
	ArrayList<Thread> clientHandlers = null;
	ServerSocket socket = null;
	boolean shutdown_requested = false;
	boolean server = false;
	boolean clientsEverConnected = false;
	String host;
	int port;
	
	PeerServer(DownloadData dd, NFO nfo, String host, int port, boolean serverMode) {
		this.dd = dd;
		this.nfo = nfo;
		this.host = host;
		this.port = port;
		this.server = serverMode;
		clientHandlers = new ArrayList<Thread>();
		InetSocketAddress addr = new InetSocketAddress(host,port);
		try {
			socket = new ServerSocket();
			socket.bind(addr,20);
			socket.setSoTimeout(1000);
		} catch (IOException e) {
			println("Failed to bind server socket.");
			e.printStackTrace();
		}
	}
	
	private void clean_handlers() {
		ArrayList<Thread> newClientHandlers = new ArrayList<Thread>();
		for (int i = 0; i < clientHandlers.size(); i++) {
			if (clientHandlers.get(i).isAlive()) {
				newClientHandlers.add(clientHandlers.get(i));
			}
		}
		clientHandlers = newClientHandlers;
	}
	
	protected void finalize() {
		if (socket!=null) {
			try {
				socket.close();
			} catch (IOException e) {
				println("Failed to close socket on finalization.");
			}
		}
	}
	
	private static void println(String msg) {
		long id = Thread.currentThread().getId();
		String log = String.format("Server #%d: %s",id,msg);
		System.out.println(log);
	}

	@Override
	public void run() {
		println("Listening...");
		while (!clientsEverConnected || (clientHandlers!=null && clientHandlers.size() > 0)) {
			try {
				Socket s = socket.accept();
				println("Accepted connection.");
				println("Dispatching client handler...");
				clientHandlers.add(new Thread(new ServerClientHandler(s,dd,nfo.getDDir(),server)));
				clientHandlers.get(clientHandlers.size()-1).start();
				clientsEverConnected = true;
			} catch (SocketTimeoutException e) {
				clean_handlers();
				if (shutdown_requested) {
					println("Shutting down server...");
					return;
				}
			} catch (IOException e) {
				println("Error accepting connection.");
			}
		}
	}
	
	
	

}
