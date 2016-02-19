import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

//Class provides static methods to handle chunk commands
//TODO generate Summary and NFO files and check NFO when undoing a chunk command
class FileChunker {
	
	// Split file into chunks in the output directory
	static void split(File path_in, File path_out, int chunk_size) {
		//Initialize variables
		if (!path_in.exists()) {
			System.out.println("Error: input file does not exist.");
			System.exit(1);
		}
		NFO nfo_file = new NFO(path_out,path_in.getName());
		DataInputStream din = null;
		DataOutputStream dout = null;
		File output_file = null;
		long tot_bytes_written = 0;
		long cur_bytes_written = 0;
		int file_split_counter = 0;
		byte[] bytes = new byte[1000];
		//Open file for reading
		try {
			din = new DataInputStream(
					new BufferedInputStream(new FileInputStream(path_in)));
		} catch (FileNotFoundException e) {
			System.out.println("Could not find the source file.");
			e.printStackTrace();
		}
		//Open first file for writing
		try {
			if (!path_out.exists()) {
				path_out.mkdir();
			}
			output_file = mkOutputFile(path_out,file_split_counter);
			if(!output_file.createNewFile()) {
				System.out.println("Could not create file: output file exists.");
				System.exit(1);
			}
			file_split_counter++;
			dout = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(output_file)));
		} catch (IOException e1) {
			System.out.println("Error opening an output file for writing.");
			e1.printStackTrace();
		}
		//Split the file into parts
		try {
			while (true) {
				int bytes_read = din.read(bytes);
				if (bytes_read <= 0) {
					break; //End of input
				} else { //Read some bytes, write them to file
					if (cur_bytes_written + bytes_read > chunk_size*1000) {
						dout.close(); //Must move to next file
						if (!nfo_file.addChunk(output_file.getName())) {
							System.out.println("Error: NFO reports chunk cannot be added");
						}
						output_file = mkOutputFile(path_out,file_split_counter);
						file_split_counter++;
						output_file.createNewFile();
						dout = new DataOutputStream(
								new BufferedOutputStream(
										new FileOutputStream(output_file)));
						cur_bytes_written = 0;
					}
					dout.write(bytes, 0, bytes_read);
					cur_bytes_written += bytes_read;
					tot_bytes_written += bytes_read;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (din!=null)
				try {
					din.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (dout!=null)
				try {
					dout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		if (!nfo_file.addChunk(output_file.getName())) {
			System.out.println("Error: NFO reports chunk cannot be added");
		}
		
		//Transfer statistics
		System.out.println(String.format("Transferred %d bytes into %d files",
				tot_bytes_written,file_split_counter));
		
		//Write NFO file
		System.out.println("Writing NFO chunk properties...");
		nfo_file.write();
	}
	
	static File mkOutputFile(File path_out, int file_split_counter) {
		File ddir = path_out;
		return new File(ddir.getAbsolutePath(),String.valueOf(file_split_counter));
	}
	
	static void rejoin(File path_in) {
		NFO nfo_file = NFO.loadNFO(new File(path_in,"NFO.dat"));
		File file = new File(path_in,nfo_file.getFilename());
		String[] chunks = nfo_file.getChunkNames();
		File[] ddir_files = new File[chunks.length];
		for (int i = 0; i<chunks.length; i++) {
			ddir_files[i] = new File(path_in,chunks[i]);
		}
		byte[] bytes = new byte[1000];
		DataOutputStream dout = null;
		DataInputStream din = null;
		try {
			if (!file.createNewFile()) {
				System.out.println("Error: Output file already exists");
				System.exit(1);
			}
			dout = new DataOutputStream(
						new BufferedOutputStream(
						 new FileOutputStream(file)));
		} catch (IOException e) {
			System.out.println("Failed to open the output file.");
			e.printStackTrace();
		}
		for (int i = 0; i < ddir_files.length; i++) {
			try { //Open an input file
				din = new DataInputStream(
					   new BufferedInputStream(
					    new FileInputStream(ddir_files[i])));
			} catch (FileNotFoundException e) {
				System.out.println("Failed to open an input file chunk.");
				e.printStackTrace();
			}
			while (true) { //Add bytes to end of output file
				int bytes_read = 0;
				try {
					bytes_read = din.read(bytes);
				} catch (IOException e) {
					System.out.println("Error reading chunk from file.");
					e.printStackTrace();
				}
				if (bytes_read <= 0) break; //EOF
				try {
					dout.write(bytes,0,bytes_read);
				} catch (IOException e) {
					System.out.println("Error writing chunk to file.");
					e.printStackTrace();
				}
			} // All bytes transferred for one chunk file
			if (din!=null) {
				try {
					din.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} // All input files concatenated
		if (dout!=null) {
			try {
				dout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//Xfer statistics
		System.out.println(String.format("Compacted %d chunks.",ddir_files.length));
		
	}

}
