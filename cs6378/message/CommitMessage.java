package cs6378.message;

import java.io.Serializable;
import java.util.Set;

public class CommitMessage extends Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9108493555029207229L;

	private CommitMessage(CommitMessageBuilder builder) {
		this.setClock(builder.clock);
		this.setReceiver(builder.receiver);
		this.setSender(builder.sender);
		this.alive_servers = builder.alive_servers;
		this.setType(builder.type);
	}

	public static class CommitMessageBuilder {
		private int clock;
		private int receiver;
		private int sender;
		private String type;
		private Set<Integer> alive_servers;

		public CommitMessageBuilder clock(int clock) {
			this.clock = clock;
			return this;
		}

		public CommitMessageBuilder receiver(int receiver) {
			this.receiver = receiver;
			return this;
		}

		public CommitMessageBuilder sender(int sender) {
			this.sender = sender;
			return this;
		}

		public CommitMessageBuilder type(String type) {
			this.type = type;
			return this;
		}

		public CommitMessageBuilder alive_servers(Set<Integer> alive_servers) {
			this.alive_servers = alive_servers;
			return this;
		}

		public CommitMessage build() {
			return new CommitMessage(this);
		}
	}

	private Set<Integer> alive_servers;

	public Set<Integer> getAlive_servers() {
		return alive_servers;
	}
}
