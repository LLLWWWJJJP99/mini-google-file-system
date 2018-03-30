package cs6378.util;

import java.util.Map;

/**
 * @author 29648
 * used to store type of nodes for each node
 */
public class NodeInfo {
	private Map<Integer, String> nodeInfos;
	public Map<Integer, String> getNodeInfos() {
		return nodeInfos;
	}
	public void setNodeInfos(Map<Integer, String> nodeInfos) {
		this.nodeInfos = nodeInfos;
	}
}
