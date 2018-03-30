package cs6378.message;

import java.io.Serializable;

/**
 * @author 29648
 * message transfered bewteen clients and meta server
 */
public class MetaMessage extends Message implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1689511291853870076L;
	private long offset;
	private String content;
	public long getOffset() {
		return offset;
	}
	public void setOffset(long offset) {
		this.offset = offset;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	@Override
	public String toString() {
		return "DataMessage [offset=" + offset + ", content=" + content + ", clock=" + getClock()
				+ ", sender=" + getSender() + ", receiver=" + getReceiver() + ", type=" + getType()
				+ "]";
	}
}
