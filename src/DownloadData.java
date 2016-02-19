import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;


//Synchronized download information
class DownloadData implements Serializable {
	
	private static final long serialVersionUID = 0L;
	private transient NFO nfo = null;
	ArrayList<PieceData> pieces = null;
	
	DownloadData(NFO nfo_file) {
		pieces = new ArrayList<PieceData>();
		nfo = nfo_file;
		String[] chunks = nfo_file.getChunkNames();
		for (int i = 0; i<chunks.length; i++) {
			String chunkname = chunks[i];
			PieceData pd = new PieceData(chunkname);
			if (nfo_file.verifyChunk(chunkname)) {
				pd.status = PieceStatus.HAVE;
			} else {
				pd.status = PieceStatus.WANT;
			}
			pieces.add(pd);
		}
	}
	
	DownloadData(ArrayList<PieceData> pieces) {
		this.pieces = pieces;
		clearProgress();
	}
	
	synchronized void markNotAvailable(String id) {
		//Used in server mode only
		//Piece should only get uploaded once
		for (int i = 0; i < pieces.size(); i++) {
			PieceData pd = pieces.get(i);
			if (pd.id.equals(id)) {
				pd.status = PieceStatus.WANT;
			}
		}
	}
	
	synchronized PieceData getPiece(String id) {
		for (PieceData pd : pieces) {
			if (pd.id.equals(id)) {
				return pd;
			}
		}
		return null;
	}
	
	synchronized void install(DownloadData pieces) {
		this.pieces = pieces.pieces;
		clearProgress();
	}
	
	synchronized void clearProgress() {
		for (int i = 0; i < pieces.size(); i++) {
			pieces.get(i).status = PieceStatus.WANT;
		}
	}
	
	synchronized PieceData claimOneForDownloadHeldBy(DownloadData dd) {
		Collections.shuffle(pieces);
		for (PieceData pd: pieces) {
			if (pd.status==PieceStatus.WANT && dd.getPiece(pd.id).status==PieceStatus.HAVE) {
				pd.status=PieceStatus.DOWNLOADING;
				return pd;
			}
		}
		return null;
	}
	
	synchronized boolean piecesRemain() {
		for (PieceData pd: pieces) {
			if (pd.status==PieceStatus.WANT) {
				return true;
			}
		}
		return false;
	}
	
	synchronized void markAsDownloaded(String id) {
		if (nfo==null) {
			System.out.println("Error! NFO null on request to mark as downloaded.");
			System.out.println("Cannot verify peice integrity.");
			System.exit(1);
		} else {
			if (nfo.verifyChunk(id)) {
				for (PieceData pd: pieces) {
					if (pd.status==PieceStatus.DOWNLOADING && pd.id.equals(id)) {
						pd.status=PieceStatus.HAVE;
						return;
					}
				}
				System.out.println(String.format("Failed to mark piece: %s",id));
			} else {
				System.out.println(String.format("Chunk %s failed to verify and was discarded.",id));
				File file = new File(nfo.getDDir(),id);
				file.delete();
			}
		}
	}

}
