package cs6378.node;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs6378.message.HearbeatMessage;
import cs6378.message.Message;
import cs6378.util.FileUtil;
import cs6378.util.NodeInfo;
import cs6378.util.NodeLookup;
import cs6378.util.NodeUtil;

public class MetaServer implements Node {
	private Map<String, List<String>> file_to_chunks;
	private Map<String, Integer> chunk_to_server;
	private Map<String, String> chunk_lasttime_updated;
	private Map<Integer, Boolean> serverNeighbors;
	private Map<Integer, Long> servers_lasttime_heartbeat;
	private Integer id;
	private NodeLookup nodeLookup;
	private static final String FILEADDR = "config2.txt";
	private Set<Integer> clientNeighbors;
	private final int port;
	private final String ip;

	public MetaServer() {
		this.serverNeighbors = Collections.synchronizedMap(new HashMap<>());
		this.clientNeighbors = Collections.synchronizedSet(new HashSet<>());
		this.servers_lasttime_heartbeat = Collections.synchronizedMap(new HashMap<>());
		this.chunk_to_server = Collections.synchronizedMap(new HashMap<>());
		this.chunk_lasttime_updated = Collections.synchronizedMap(new HashMap<>());
		this.nodeLookup = new NodeLookup();
		NodeInfo info = NodeUtil.readConfig(FILEADDR, nodeLookup.getId_to_addr(), nodeLookup.getId_to_index());
		Map<Integer, String> typeInfos = info.getNodeInfos();
		for (Map.Entry<Integer, String> entry : typeInfos.entrySet()) {
			int nodeid = entry.getKey();
			String type = entry.getValue();
			if (type.equals("server")) {
				// set to true if server alive otherwise false for dead node
				serverNeighbors.put(nodeid, false);
			} else if (type.equals("mserver") && this.id == null) {
				this.id = nodeid;
			} else if (type.equals("client")) {
				clientNeighbors.add(nodeid);
			}
		}

		if (this.id == null) {
			System.err.println("Please check config file, it must has one mserver");
			System.exit(1);
		}

		this.ip = nodeLookup.getIP(this.id);
		this.port = Integer.parseInt(nodeLookup.getPort(this.id));
		file_to_chunks = FileUtil.readFileConfig();
	}

	private void init() {
		new Thread(new NodeListener(this, port)).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					synchronized (servers_lasttime_heartbeat) {
						System.out.println(serverNeighbors);
						System.out.println(servers_lasttime_heartbeat);
						for (int server : serverNeighbors.keySet()) {
							if (servers_lasttime_heartbeat.containsKey(server) && (System.currentTimeMillis() / 1000)
									- servers_lasttime_heartbeat.get(server) >= 15) {
								serverNeighbors.put(server, false);
							}
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	@Override
	public synchronized void process_Message(Message message) {
		System.out.println(message);
		if (message.getClass().getSimpleName().equals(HearbeatMessage.class.getSimpleName())) {
			HearbeatMessage hMessage = (HearbeatMessage) message;
			serverNeighbors.put(hMessage.getSender(), true);
			servers_lasttime_heartbeat.put(hMessage.getSender(), System.currentTimeMillis() / 1000);
			List<String[]> chunk_server_info = hMessage.getInfos();
			for(String[] info : chunk_server_info) {
				chunk_to_server.put(info[0], hMessage.getSender());
				chunk_lasttime_updated.put(info[0], info[1]);
			}
		}
	}

	@Override
	public synchronized void private_Message(int receiver, String type) {

	}

	public static void main(String[] args) {
		MetaServer server = new MetaServer();
		server.init();
	}
}
