package cs6378.message;

import java.io.Serializable;

public class WakeUpMessage extends Message implements Serializable {
	private static final long serialVersionUID = 6533319295759665729L;
	/**
	 * 
	 */
	private int remote;
	private int version;
	private String content;
	private String replica;

	public int getVersion() {
		return version;
	}

	public String getReplica() {
		return replica;
	}

	public int getRemote() {
		return remote;
	}

	public static class WakeUpMessageBuilder {
		private int version;
		private String content;
		private int remote;
		private String replica;

		public WakeUpMessage build() {
			return new WakeUpMessage(this);
		}

		public WakeUpMessageBuilder version(int version) {
			this.version = version;
			return this;
		}

		public WakeUpMessageBuilder content(String content) {
			this.content = content;
			return this;
		}

		public WakeUpMessageBuilder remote(int remote) {
			this.remote = remote;
			return this;
		}
		
		public WakeUpMessageBuilder replica(String replica) {
			this.replica = replica;
			return this;
		}
	}

	public WakeUpMessage(WakeUpMessageBuilder builder) {
		this.version = builder.version;
		this.content = builder.content;
		this.remote = builder.remote;
		this.replica = builder.replica;
	}
	
	
	
	@Override
	public String toString() {
		return "WakeUpMessage [remote=" + remote + ", version=" + version + ", content=" + content + ", replica="
				+ replica + ", toString()=" + super.toString() + "]";
	}

	public String getContent() {
		return content;
	}

	public static void main(String[] args) {

	}

}
