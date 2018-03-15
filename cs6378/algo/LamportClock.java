package cs6378.algo;

import cs6378.message.Message;

public class LamportClock {
	private int clock = 0;
	private int d = 1;

	public LamportClock(int d) {
		this.d = d;
	}

	public synchronized void local_Event() {
		this.clock += d;
	}

	public int getClock() {
		return clock;
	}

	public int getD() {
		return d;
	}

	public synchronized void msg_event(Message message) {
		this.clock += d;
		if (message.getClock() + d > this.clock) {
			this.clock = message.getClock() + d;
		}
	}

	public static void main(String[] args) {
		LamportClock clock = new LamportClock(10);
		clock.clock = 2;
		Message message = new Message(1, "", 1, 1);
		clock.msg_event(message);
		System.out.println(clock.clock);
	}
}
