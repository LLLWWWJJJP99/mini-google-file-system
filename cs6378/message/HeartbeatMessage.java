package cs6378.message;

import java.io.Serializable;
import java.util.List;

public class HeartbeatMessage extends Message implements Serializable {
	private static final long serialVersionUID = 5223760213410236593L;
	private List<String[]> infos;

	public List<String[]> getInfos() {
		return infos;
	}
	
	@Override
	public String toString() {
		String str = "";
		for(String[] info : infos) {
			str = str + info[0] + ":" + info[1] + " ";
		}
		return "HearbeatMessage [infos=" + str + ", clock=" + getClock() + ", sender=" + getSender()
				+ ", receiver=" + getReceiver() + ", type=" + getType() + "]";
	}

	public void setInfos(List<String[]> infos) {
		this.infos = infos;
	}
	
}
