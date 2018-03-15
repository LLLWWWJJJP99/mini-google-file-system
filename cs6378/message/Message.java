package cs6378.message;

import java.io.Serializable;

public class Message implements Serializable{
	private static final long serialVersionUID = -5009808321221813169L;
	private int sender;
	private int receiver;
	private String type;
	private int clock;
	public int getClock() {
		return clock;
	}
	public void setClock(int clock) {
		this.clock = clock;
	}
	public int getSender() {
		return sender;
	}
	public void setSender(int sender) {
		this.sender = sender;
	}
	public int getReceiver() {
		return receiver;
	}
	public void setReceiver(int receiver) {
		this.receiver = receiver;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public Message() {}
	
	public Message(int counter, String type, int from, int to) {
		this.clock = counter;
		this.type = type;
		this.sender = from;
		this.receiver = to;
	}
	@Override
	public String toString() {
		return "[sender=" + sender + ", receiver=" + receiver + ", type=" + type + ", clock=" + clock + "]";
	}
	
}
