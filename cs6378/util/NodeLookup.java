package cs6378.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NodeLookup {
	Map<Integer, String> id_to_addr;
	Map<Integer, Integer> id_to_index;
	public NodeLookup() {
		id_to_addr = Collections.synchronizedMap(new HashMap<>());
		id_to_index = Collections.synchronizedMap(new HashMap<>());
	}
	
	public int getIndex(int id) {
		return id_to_index.get(id);
	}
	
	public String getIP(int id) {
		return id_to_addr.get(id).split(":")[1];
	}

	public String getPort(int id) {
		return id_to_addr.get(id).split(":")[0];
	}

	public Map<Integer, String> getId_to_addr() {
		return id_to_addr;
	}

	public Map<Integer, Integer> getId_to_index() {
		return id_to_index;
	}
}
