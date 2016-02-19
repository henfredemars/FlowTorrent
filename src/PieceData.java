import java.io.Serializable;


//Information about the download status of one piece
public class PieceData implements Serializable {
	
	private static final long serialVersionUID = 0L;
	public String id;
	public PieceStatus status;
	
	public PieceData(String id) {
		this.id = id;
		this.status = PieceStatus.WANT;
	}

}
