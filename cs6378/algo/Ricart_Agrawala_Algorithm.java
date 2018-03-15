package cs6378.algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cs6378.message.Message;

public class Ricart_Agrawala_Algorithm {
	private boolean reques_critical_section;
	private int our_request_clock;
	private LamportClock clock;
	private int outstanding_reply_count;
	private List<Integer> defered_reply;
	private final int my_id;
	
	public int getOutstanding_reply_count() {
		return outstanding_reply_count;
	}

	public void setOutstanding_reply_count(int outstanding_reply_count) {
		this.outstanding_reply_count = outstanding_reply_count;
	}

	public boolean isReques_critical_section() {
		return reques_critical_section;
	}
	
	public int getOur_request_clock() {
		return our_request_clock;
	}

	public synchronized void setOur_request_clock(int our_request_clock) {
		this.our_request_clock = our_request_clock;
	}

	public void setReques_critical_section(boolean reques_critical_section) {
		this.reques_critical_section = reques_critical_section;
	}

	public Ricart_Agrawala_Algorithm(int n, int id) {
		this.my_id = id;
		clock = new LamportClock(1);
		reques_critical_section = false;
		defered_reply = Collections.synchronizedList(new ArrayList<>());
		outstanding_reply_count = n;
	}
	
	public List<Integer> getDefered_reply() {
		return defered_reply;
	}

	public synchronized void local_event() {
		clock.local_Event();
	}
	
	public synchronized boolean check_critical_section() {
		return outstanding_reply_count == 0 && reques_critical_section;
	}

	public static void main(String[] args) {

	}

	public int getClock() {
		return clock.getClock();
	}

	public synchronized void process_reply(Message message) {
		clock.msg_event(message);
		outstanding_reply_count--;
	}
	
	public synchronized boolean process_request(Message message) {
		clock.msg_event(message);
		if(reques_critical_section && (our_request_clock < message.getClock() || 
				our_request_clock == message.getClock() && my_id < message.getSender()
				)) {
			defered_reply.add(message.getSender());
			return false;
		}else {
			return true;
		}
	}
}
