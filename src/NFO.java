import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

//NFO data file abstraction
class NFO implements Serializable {
	
	private static final long serialVersionUID = 0L;
	private String filename;
	private LinkedHashMap<String,String> chunks;
	private LinkedHashMap<String,Long> chunk_sizes;
	private transient File ddir;
	private transient MessageDigest md;
	
	NFO(File ddir,String filename) {
		chunks = new LinkedHashMap<String,String>();
		chunk_sizes = new LinkedHashMap<String,Long>();
		initMsgDigest();
		this.filename = filename;
		this.ddir = ddir;
	}
	
	synchronized void initMsgDigest() {
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Error, SHA-1 not supported.");
			e.printStackTrace();
		}
	}
	
	synchronized String getFilename() {
		return filename;
	}
	
	synchronized File getDDir() {
		return ddir;
	}
	
	synchronized long getSizeOfChunk(String chunkname) {
		return chunk_sizes.get(chunkname);
	}
	
	synchronized String[] getChunkNames() {
		Set<String> cns = chunks.keySet();
		ArrayList<String> cn = new ArrayList<String>();
		for (String key: cns) {
			cn.add(key);
		}
		return cn.toArray(new String[0]);
	}
	
	static NFO loadNFO(File file) {
		System.out.println("Loading NFO file...");
		if (!file.isFile()) {
			System.out.println("Asked to load NFO that does not exist!");
			return null;
		}
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
			NFO nfo = (NFO) ois.readObject();
			nfo.setDDir(file.getParentFile());
			return nfo;
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Error loading NFO");
			e.printStackTrace();
		} finally {
			if (ois!=null) {
				try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	synchronized boolean addChunk(String chunkName) {
		if (ddir==null) return false;
		File file = new File(ddir,chunkName);
		if (!file.exists()) return false;
		String hash = computeHash(file);
		Long size = file.length();
		chunks.put(chunkName, hash);
		chunk_sizes.put(chunkName, size);
		return true;
	}
	
	synchronized void setDDir(File ddir) {
		this.ddir = ddir;
	}
	
	synchronized boolean chunksPresentAndValid() {
		if (chunks.size()==0) return false;
		if (ddir==null || !ddir.isDirectory()) return false;
		Set<String> keys = chunks.keySet();
		for (String key : keys) {
			if (!verifyChunk(key)) return false;
		}
		return true;
	}
	
	synchronized boolean verifyChunk(String chunkname) {
		Long expectedSize = chunk_sizes.get(chunkname);
		String expectedHash = chunks.get(chunkname);
		if (expectedHash==null || expectedSize==null || chunkname==null) return false;
		File cfile = new File(ddir,chunkname);
		if (!cfile.isFile()) return false;
		if (cfile.length()!=expectedSize) return false;
		String gotHash = computeHash(cfile);
		if (!gotHash.equals(expectedHash)) return false;
		return true;
	}
	
	synchronized private String computeHash(File file) {
		byte[] input_buf = new byte[1000];
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			while (true) {
				int bytes_read = dis.read(input_buf);
				if (bytes_read==-1) {
					break;
				}
				md.update(input_buf, 0, bytes_read);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (dis!=null) {
			try {
				dis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		BigInteger hash = new BigInteger(1,md.digest());
		String hash_str = hash.toString(16);
		while (hash_str.length() < 40) {
			hash_str = '0' + hash_str;
		}
		return hash_str;
	}
	
	synchronized void write() {
		File file = new File(ddir,"NFO.dat");
		if (file.exists()) {
			file.delete();
		}
		ObjectOutputStream oos = null;
		try {
			file.createNewFile();
			oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			oos.writeObject(this);
			oos.close();
		} catch (IOException e) {
			System.out.println("Error writing summary file.");
			e.printStackTrace();
		}
		writeSummary();
	}
	
	synchronized public void writeSummary() {
		File file = new File(ddir,"Summary.txt");
		if (file.exists()) {
			file.delete();
		}
		try {
			file.createNewFile();
			PrintWriter pw = new PrintWriter(new File(ddir,"Summary.txt"));
			pw.println(this.toString());
			pw.close();
		} catch (IOException e) {
			System.out.println("Error writing summary file.");
			e.printStackTrace();
		}
	}
	
	private void readObject(ObjectInputStream inputStream)
            throws IOException, ClassNotFoundException
    {
        inputStream.defaultReadObject();
        initMsgDigest();
    }
	
	synchronized public String toString() {
		StringBuffer str = new StringBuffer();
		str.append("Torrent Info:\n\n");
		str.append(String.format("Number of Chunks: %d\n", chunks.size()));
		str.append("Hash Algorithm: SHA-1\n\n");
		Set<String> keys = chunks.keySet();
		for (String chunk_name : keys) {
			str.append(String.format("%-10s %s\n",chunk_name,chunks.get(chunk_name)));
		}
		return str.toString();
	}

}
