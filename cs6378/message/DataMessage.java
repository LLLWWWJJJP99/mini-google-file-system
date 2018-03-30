package cs6378.message;

import java.io.Serializable;

/**
 * @author 29648
 * message transfered between server and clients
 */
public class DataMessage extends Message implements Serializable {
	private static final long serialVersionUID = 1753984007018495742L;
	private long offset;
	private String filaName;
	private String content;
	
	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public String getFilaName() {
		return filaName;
	}

	public void setFilaName(String filaName) {
		this.filaName = filaName;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public static void main(String[] args) {
		
	}

}
