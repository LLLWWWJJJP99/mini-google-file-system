package cs6378.server;

import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetServer {
	private int id;
	private ServerSocket myServer;
	private List<Integer> neighbors;
	
	static class NodeLookUp{
		Map<Integer, Integer> map;
		public NodeLookUp() {
			map = Collections.synchronizedMap(new HashMap<>());
		}
	}
}
