import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;


//Handles communication with the client
public class ServerClientHandler implements Runnable {
	
	DataInputStream dis = null;
	DataOutputStream dos = null;
	Socket s = null;
	byte[] input_buffer = null;
	byte[] output_buffer = null;
	File ddir = null;
	DownloadData pieces = null;
	boolean server = false;
	
	public ServerClientHandler(Socket s, DownloadData pieces, File ddir, boolean serverMode) {
		this.s = s;
		this.input_buffer = new byte[1000];
		this.output_buffer = new byte[1000];
		this.pieces = pieces;
		this.ddir = ddir;
		this.server = serverMode;
		try {
			dis = new DataInputStream(new BufferedInputStream(s.getInputStream()));
			dos = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
		} catch (IOException e) {
			println("Failed to get streams.");
			e.printStackTrace();
		}
	}
	
	public static boolean seg_equals(byte[] a, byte[] b, int len) {
		if (a.length < len) return false;
		if (b.length < len) return false;
		for (int i = 0; i < len; i++) {
			if (a[i]!=b[i]) return false;
		}
		return true;
	}

	@Override
	public void run() {
		//Client handling protocol implemented here
		println("Running...");
		if (handshake()) {
			while (true) { //Process commands
				try {
					int bytes_read = 0;
					char last_byte = '0';
					println("Waiting for next command...");
					while (last_byte!='\n') {
						int br = dis.read(input_buffer,bytes_read,
								input_buffer.length-bytes_read);
						if (br==-1 || br==0) {
							sleep(1000);
							continue;
						}
						if (br==0) continue;
						bytes_read += br;
						last_byte = (char)input_buffer[bytes_read-1];
					}
					byte[] string_buffer = new byte[bytes_read];
					System.arraycopy(input_buffer, 0, string_buffer, 0, 
							bytes_read);
					String command = new String(string_buffer);
					if (command.equals("FETCHPIECELIST\n")) { //Client asks for piece list
						println("Client asked for piece list.");
						ObjectOutputStream oos = new ObjectOutputStream(dos);
						synchronized(pieces) {
							oos.writeObject(pieces);
						}
						oos.flush();
						println("Piece list sent to client.");
					} else if (command.startsWith("GETPIECE ")) {
						String[] parts = command.split(" ");
						int id = Integer.valueOf(parts[1].trim());
						println("Client requested piece " + String.valueOf(id));
						upload_piece(id);
						println(String.format("Piece %d has been uploaded.",id));
						if (server) {
							pieces.markNotAvailable(String.valueOf(id));
						}
					} else if (command.equals("GOODBYE\n")) {
						println("Client disconnected, ending session.");
						return;
					} else {
						println("Unknown command, terminating...");
						return;
					}
				} catch (IOException e) {
					println("Error reading from socket.");
				}
			}
		} else {
			println("Exiting, bad handshake...");
		}
		println("Client handler is exiting...");
	}
	
	private void upload_piece(int id) {
		println(String.format("Uploading piece %d.",id));
		File fo = new File(ddir,String.valueOf(id));
		DataInputStream fdis = null;
		try {
			fdis = new DataInputStream(new BufferedInputStream(
					new FileInputStream(fo)));
			while (true) {
				int fbr = fdis.read(output_buffer, 0, output_buffer.length);
				if (fbr==0) {
					sleep(1000);
					continue;
				} else if (fbr==-1) {
					break;
				}
				dos.write(output_buffer, 0, fbr);
			}
			dos.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			println("Error writing block to output stream.");
			e.printStackTrace();
		} finally {
			if (fdis!=null) {
				try {
					fdis.close();
				} catch (IOException e) {
					//Do nothing
				}
			}
		}
		
	}
	
	private boolean handshake() {
		String greeting = "HELLOCLIENT\n";
		String expected_greeting = "HELLOSERVER\n";
		byte[] greeting_bytes = greeting.getBytes();
		byte[] expected_greeting_bytes = expected_greeting.getBytes();
		System.arraycopy(greeting_bytes, 0, output_buffer, 0, greeting_bytes.length);
		try {
			int bytes_read = 0;
			println("Waiting for handshake...");
			while (bytes_read < expected_greeting_bytes.length) {
				int br = dis.read(input_buffer,bytes_read,
						input_buffer.length-bytes_read); //Read hello
				if (br==0) {
					sleep(1000);
					continue;
				} else if (br==-1) {
					println("Handshake ended prematurely");
					return false;
				}
				bytes_read += br;
			}
			dos.write(output_buffer, 0, greeting_bytes.length); //Write hello
			dos.flush();
			println("Wrote HELLOCLIENT");
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
	
	public static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// Ignored
		}
	}
	
	private static void println(String msg) {
		long id = Thread.currentThread().getId();
		String log = String.format("ServerClientHander #%d: %s",id,msg);
		System.out.println(log);
	}
	
	protected void finalize() {
		try {
			if (dis!=null) dis.close(); dis=null;
			if (dos!=null) dos.close(); dos=null;
			if (s!=null && !s.isClosed()) s.close(); s=null;
		} catch (IOException e) { 
			println("Error closing socket in finailization");
		}
	}

}
