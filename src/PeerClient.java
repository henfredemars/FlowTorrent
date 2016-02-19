import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;


class PeerClient implements Runnable {

	String rhost;
	int rport;
	DataInputStream dis = null;
	DataOutputStream dos = null;
	byte[] input_buffer = null;
	byte[] output_buffer = null;
	DownloadData dd = null;
	DownloadData server_dd = null;
	NFO nfo = null;
	Socket s = null;
	int backoff_time = 1000; //Back off of piece list requests
	final int backoff_time_min = 1000;
	final int backoff_time_max = 20000;
	
	PeerClient(DownloadData dd, NFO nfo, String rhost, int rport) {
		this.rhost = rhost;
		this.rport = rport;
		this.nfo = nfo;
		this.dd = dd;
		this.input_buffer = new byte[1000];
		this.output_buffer = new byte[1000];
		while (true) {
		try {
			s = new Socket(rhost,rport);
			break;
		} catch (UnknownHostException e) {
			println("Failed to connect to server.");
			e.printStackTrace(); break;
		} catch (IOException e) {
			println("IO error in connecting. Try again in ten seconds...");
			sleep(10000);
		}
		}
		try {
			dis = new DataInputStream(new BufferedInputStream(s.getInputStream()));
			dos = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
		} catch (IOException e) {
			println("Failed to get streams.");
			e.printStackTrace();
		}
	}
	
	private static void println(String msg) {
		long id = Thread.currentThread().getId();
		String log = String.format("Client #%d: %s",id,msg);
		System.out.println(log);
	}
	
	//Note that the server, if primary server, may lie about piece availability to force
	//  the client to download only from the available chunks
	private void downloadRandomBlock() {
		println("Downloading a block.");
		if (server_dd==null) return; //No server
		PieceData pd = dd.claimOneForDownloadHeldBy(server_dd);
		if (pd==null) {
			println("No blocks that I need are available right now. Backing off a little...");
			sleep(backoff_time);
			backoff_time *= 1.1;
			fetchPieceList();
			return; //Nothing available
		} else {
			backoff_time = backoff_time_min;
		}
		String command = String.format("GETPIECE %s\n",pd.id);
		File pd_file = new File(nfo.getDDir(),pd.id);
		DataOutputStream pdout = null;
		byte[] command_bytes = command.getBytes();
		System.arraycopy(command_bytes,0,output_buffer,0,command_bytes.length);
		try {
			pd_file.createNewFile();
			pdout = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(pd_file)));
			dos.write(output_buffer,0,command_bytes.length);
			dos.flush();
			int bytes_read = 0;
			while (bytes_read < nfo.getSizeOfChunk(pd.id)) {
				int br = dis.read(input_buffer, 0, input_buffer.length);
				if (br==0) {
					sleep(1000);
					continue;
				} else if (br==-1) {
					throw new IOException("Block ended unexpectedly.");
				}
				bytes_read += br;
				pdout.write(input_buffer, 0, br);
			}
			pdout.flush();
			dd.markAsDownloaded(pd.id);
			println(String.format("Piece %s has been downloaded.",pd.id));
		} catch (IOException e) {
			println("Failed to download block from server.");
			e.printStackTrace();
		} finally {
			if (pdout!=null) {
				try {
					pdout.close();
				} catch (IOException e) {
					//Do nothing
				}
			}
		}
	}
	
	private void fetchPieceList() {
		println("Fetching piece list...");
		String command = "FETCHPIECELIST\n";
		byte[] command_bytes = command.getBytes();
		System.arraycopy(command_bytes,0,output_buffer,0,command_bytes.length);
		try {
			dos.write(output_buffer,0,command_bytes.length);
			dos.flush();
			ObjectInputStream ios = new ObjectInputStream(dis);
			server_dd = (DownloadData)ios.readObject();
		} catch (IOException e) {
			println("Failed to fetch piece list.");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		println("Got updated piece list from the server.");
	}
	
	private void disconnect() {
		String goodbye = "GOODBYE\n";
		byte[] goodbye_bytes = goodbye.getBytes();
		System.arraycopy(goodbye_bytes, 0, output_buffer, 0, goodbye_bytes.length);
		try {
			dos.write(output_buffer, 0, goodbye_bytes.length); //Write hello
			dos.flush();
			println("Wrote GOODBYE");
		} catch (IOException e) {
			println("Communication error in disconnect.");
		}
	}
	
	private boolean handshake() {
		String greeting = "HELLOSERVER\n";
		String expected_greeting = "HELLOCLIENT\n";
		byte[] greeting_bytes = greeting.getBytes();
		byte[] expected_greeting_bytes = expected_greeting.getBytes();
		System.arraycopy(greeting_bytes, 0, output_buffer, 0, greeting_bytes.length);
		try {
			int bytes_read = 0;
			dos.write(output_buffer, 0, greeting_bytes.length); //Write hello
			dos.flush();
			println("Wrote HELLOSERVER");
			println("Reading handshake reply...");
			while (bytes_read < expected_greeting_bytes.length) {
				int br = dis.read(input_buffer,bytes_read,
						input_buffer.length-bytes_read); //Read hello
				if (br==0) {
					sleep(1000);
					continue;
				} else if (br==-1) {
					println("Unexpected end of handshake.");
					return false;
				}
				bytes_read += br;
			}
		} catch (IOException e) {
			println("Communication error.");
			return false;
		}
		if (!seg_equals(input_buffer,expected_greeting_bytes,expected_greeting_bytes.length)) {
			println("Handshake failed!");
			return false;
		}
		println("Handshake successful!");
		return true;
	}
	
	public static boolean seg_equals(byte[] a, byte[] b, int len) {
		if (a.length < len) return false;
		if (b.length < len) return false;
		for (int i = 0; i < len; i++) {
			if (a[i]!=b[i]) return false;
		}
		return true;
	}
	
	public static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// Ignored
		}
	}
	
	protected void finalize() {
		//Attempt to close data streams
		if (dis!=null) {
			try {
				dis.close();
			} catch (IOException e) {
				//Ignore
			}
		}
		if (dos!=null) {
			try {
				dos.close();
			} catch (IOException e) {
				//Ignore
			}
		}
	}
	
	@Override
	public void run() {
		println("Running...");
		if (handshake()) {
			fetchPieceList();
			while (dd.piecesRemain()) {
				//Randomly request and download a piece
				downloadRandomBlock();
			}
			disconnect();
			nfo.writeSummary();
			println("All pieces downloaded...");
			sleep(3000);
		} else {
			println("Exiting, bad handshake...");
		}
		println("Server handler is exiting...");
	}
	
}
