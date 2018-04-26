package cs6378.node;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import cs6378.message.MetaMessage;
import cs6378.message.HeartbeatMessage;
import cs6378.message.Message;
import cs6378.message.MsgType;
import cs6378.util.FileUtil;
import cs6378.util.NodeInfo;
import cs6378.util.NodeLookup;
import cs6378.util.NodeUtil;

public class MetaServer implements Node {
	private Map<String, List<String>> file_to_chunks;
	private Map<String, Integer> replica_to_server;
	private Map<String, String> chunk_lasttime_updated;
	private Map<Integer, Boolean> serverNeighbors;
	private Map<Integer, Long> servers_lasttime_heartbeat;
	private Map<String, Long> chunk_size;
	private Integer id;
	private NodeLookup nodeLookup;
	private static final String FILEADDR = "config2.txt";
	private Set<Integer> clientNeighbors;
	private final int port;
	private final String ip;
	private static int filename = 0;
	private Map<String, String[]> chunk_to_replica;

	public MetaServer() {
		this.chunk_size = Collections.synchronizedMap(new HashMap<>());
		this.serverNeighbors = Collections.synchronizedMap(new HashMap<>());
		this.clientNeighbors = Collections.synchronizedSet(new HashSet<>());
		this.servers_lasttime_heartbeat = Collections.synchronizedMap(new HashMap<>());
		this.replica_to_server = Collections.synchronizedMap(new HashMap<>());
		this.chunk_lasttime_updated = Collections.synchronizedMap(new HashMap<>());
		this.chunk_to_replica = Collections.synchronizedMap(new HashMap<>());
		this.nodeLookup = new NodeLookup();
		// read ip and port for each node
		NodeInfo info = NodeUtil.readConfig(FILEADDR, nodeLookup.getId_to_addr(), nodeLookup.getId_to_index());
		Map<Integer, String> typeInfos = info.getNodeInfos();
		// set up node type for each node
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
		file_to_chunks = FileUtil.readFileConfig(chunk_to_replica);
	}

	private void init() {
		// start to accept mesasge
		new Thread(new NodeListener(this, port)).start();

		// start to accept heart beat message
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					synchronized (servers_lasttime_heartbeat) {
						// System.out.println(serverNeighbors);
						// System.out.println(servers_lasttime_heartbeat);
						synchronized (serverNeighbors) {
							for (int server : serverNeighbors.keySet()) {
								// check each node if metaserver has not received its' message longer than 15s
								// then this node is dead
								if (servers_lasttime_heartbeat.containsKey(server)
										&& (System.currentTimeMillis() / 1000)
												- servers_lasttime_heartbeat.get(server) >= 15) {
									serverNeighbors.put(server, false);
								}
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
		String realClazz = message.getClass().getSimpleName();
		// process heart beat message and update meta info according to it
		if (realClazz.equals(HeartbeatMessage.class.getSimpleName())) {
			HeartbeatMessage hMessage = (HeartbeatMessage) message;
			serverNeighbors.put(hMessage.getSender(), true);
			servers_lasttime_heartbeat.put(hMessage.getSender(), System.currentTimeMillis() / 1000);
			List<String[]> chunk_server_info = hMessage.getInfos();
			for (String[] info : chunk_server_info) {
				replica_to_server.put(info[0], hMessage.getSender());
				chunk_lasttime_updated.put(info[0], info[1]);
				chunk_size.put(info[0], Long.parseLong(info[2].trim()));
			}
			// accept an message from client
		} else if (realClazz.equals(MetaMessage.class.getSimpleName())) {
			MetaMessage dMessage = (MetaMessage) message;
			System.out.println("DMessage: " + dMessage);
			// Integer chosen_server = choose_server();
			// check if chosen server is dead then return failure message

			// send create message to chosen server
			if (dMessage.getType().equals(MsgType.CREATE)) {
				// private_Message(chosen_server, MsgType.CREATE);
				Integer[] chosen_servers = choose_three_servers();

				boolean all_dead = true;
				for (Integer server : chosen_servers) {
					all_dead &= is_server_dead(server);
				}

				if (!all_dead) {
					String chunk_name = UUID.randomUUID().toString();
					for (Integer server : chosen_servers) {
						if (serverNeighbors.get(server)) {
							private_Message(server, MsgType.CREATE, chunk_name);
						}
					}
					filename++;
				} else {
					System.err.println("Chosen servers all are dead ");
					send_failure(dMessage, "CREATE_OPS");
				}
				System.out.println("<========================================>");
				for (Map.Entry<String, String[]> entry : chunk_to_replica.entrySet()) {
					String[] replicas = entry.getValue();
					String chunk = entry.getKey();
					String part2 = String.join(",", replicas);
					System.out.println(chunk + " || " + part2);
				}
				System.out.println("<========================================>");
				// send append message to chosen server
			} else if (dMessage.getType().equals(MsgType.APPEND)) {
				// randomly choose a file and check whether its last chunk has enough to insert
				// new lines
				List<String> file_list = new ArrayList<>(file_to_chunks.keySet());
				// System.out.println("file_list: " + file_list);
				Random rand = new Random();
				String chosen_file = file_list.get(rand.nextInt(file_list.size()));
				List<String> chunks = file_to_chunks.get(chosen_file);

				// System.out.println("chosen_file: " + chosen_file);
				// System.out.println("chunks: " + chunks);

				String chosen_chunk = chunks.get(chunks.size() - 1);

				int choice = rand.nextInt(10);
				String content = "\n_Add_New_Line_at_" + dMessage.getClock();
				// randomly generate new lines
				if (choice >= 6) {
					byte[] bytes = new byte[2048];
					Arrays.fill(bytes, (byte) 1);
					content = new String(bytes);
				}
				// System.out.println("chosen_chunk: " + chosen_chunk);
				// System.out.println("chunk_size: " + chunk_size);
				long chosen_chunk_size = chunk_size.get(chosen_chunk);
				MetaMessage reply = new MetaMessage();
				reply.setOffset(-1);
				reply.setClock(dMessage.getClock());
				reply.setReceiver(dMessage.getSender());
				reply.setSender(id);
				long content_size = content.getBytes().length;
				// if new lines exceed last chunk's size limit then tell client to create a new
				// chunk on chosen server
				if (8192l - chosen_chunk_size < content_size) {
					String new_replica_name = UUID.randomUUID().toString();
					reply.setType(MsgType.CREATE);
					// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
					// reply.setContent(content + "*" + new_replica_name + "*" + chosen_server);
					// ????????????????????????????????????????????????????????????????????????????????
					// updateChunkInfo(chosen_file, new_replica_name, chosen_server,
					// chosen_chunk_size, chosen_chunk);

				} else {
					// otherwise go last chunk 's corresponding server and append new lines
					reply.setType(MsgType.APPEND);
					int corresponding_server = replica_to_server.get(chosen_chunk);
					reply.setContent(content + "*" + chosen_chunk + "*" + corresponding_server);
				}
				private_Message(reply);
				// receive a new read message from client
			} else if (MsgType.READ.equals(dMessage.getType())) {
				List<String> file_list = new ArrayList<>(file_to_chunks.keySet());
				// System.out.println("file_list: " + file_list);
				Random rand = new Random();
				String chosen_file = file_list.get(rand.nextInt(file_list.size()));
				List<String> chunks = file_to_chunks.get(chosen_file);

				// System.out.println("chosen_file: " + chosen_file);
				// System.out.println("chunks: " + chunks);
				// randomly choose a offset for the read operations
				String chosen_chunk = chunks.get(rand.nextInt(chunks.size()));
				boolean all_dead = true;
				String chosen_replica = null;
				String[] chosen_replicas = chunk_to_replica.get(chosen_chunk);
				for(String replica : chosen_replicas) {
					Integer corresponding_server = replica_to_server.get(replica);
					if(serverNeighbors.get(corresponding_server)) {
						all_dead = false;
						chosen_replica = replica;
						break;
					}
				}
				
				if(all_dead){
					System.err.println(" Meta Server found all file server die");
					send_failure(dMessage, chosen_chunk);
					
				}else {
					int replic_server = replica_to_server.get(chosen_replica);
					long limit = chunk_size.get(chosen_replica);
					long offset = limit == 0 ? 0l : ThreadLocalRandom.current().nextLong(limit);
					System.out.println("file size " + limit + " , offset " + offset);
					MetaMessage reply = new MetaMessage();
					reply.setClock(dMessage.getClock());
					reply.setContent(chosen_replica + "*" + replic_server);
					reply.setOffset(offset);
					reply.setReceiver(dMessage.getSender());
					reply.setSender(id);
					reply.setType(MsgType.READ);
					private_Message(reply);
				}
			}
		}
	}

	private synchronized boolean is_server_dead(Integer chosen_server) {
		return (chosen_server == null || !serverNeighbors.get(chosen_server));
	}

	private synchronized void send_failure(MetaMessage dMessage, String chosen_chunk) {
		MetaMessage failure = new MetaMessage();
		failure.setOffset(-1);
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		failure.setContent(dMessage.getType() + " " + chosen_chunk);
		failure.setClock(dMessage.getClock());
		failure.setReceiver(dMessage.getSender());
		failure.setSender(id);
		failure.setType(MsgType.FAILURE);
		private_Message(failure);
	}

	private synchronized Integer[] choose_three_servers() {
		Integer[] chosen_servers = new Integer[3];
		int index = 0;
		List<Integer> list = new ArrayList<>(serverNeighbors.keySet());
		Random random = new Random();
		while (index < 3) {
			int rand = random.nextInt(list.size());
			Integer num = list.remove(rand);
			chosen_servers[index] = num;
			index++;
		}
		return chosen_servers;
	}

	/**
	 * randomly choose a server
	 * 
	 * @return server id
	 */
	private synchronized Integer choose_chunk() {
		Random random = new Random();
		List<Integer> servers = new ArrayList<>(serverNeighbors.keySet());

		int random_server = random.nextInt(servers.size());
		System.err.println("Server is chosen " + servers.get(random_server));
		return servers.get(random_server);
	}

	@Override
	public synchronized void private_Message(Message message) {
		String addr = nodeLookup.getIP(message.getReceiver());
		int port = Integer.parseInt(nodeLookup.getPort(message.getReceiver()));
		try {
			Socket socket = new Socket(addr, port);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(message);
			oos.close();
			socket.close();
		} catch (ConnectException e) {
			System.out.println("Please Wait For Other Nodes To Be Started");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void private_Message(int receiver, String type, String chunk_name) {
		String addr = nodeLookup.getIP(receiver);
		int port = Integer.parseInt(nodeLookup.getPort(receiver));
		try {
			Socket socket = new Socket(addr, port);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			// send a create message to receiver directly
			if (MsgType.CREATE.equals(type)) {
				String replica_name = UUID.randomUUID().toString();
				String file_name = filename + "#";
				if (serverNeighbors.get(receiver)) {
					file_to_chunks.putIfAbsent(file_name, new ArrayList<>());
					updateChunkInfo(file_name, replica_name, receiver, 0l, chunk_name);

					MetaMessage message = new MetaMessage();
					message.setClock(-1);
					message.setContent(replica_name);
					message.setOffset(-1);
					message.setSender(id);
					message.setReceiver(receiver);
					message.setType(MsgType.CREATE);
					oos.writeObject(message);
					System.out.println(
							"Meta Server Wants to Create " + file_name + " - " + chunk_name + " - " + replica_name);
				} else {
					System.err.println("While Transfering the Create Message, Server " + receiver + " dies");
				}
			}
			oos.close();
			socket.close();
		} catch (ConnectException e) {
			System.out.println("Please Wait For Other Nodes To Be Started");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * update meta data with file name, chunk name and node id and chunk size
	 * 
	 * @param file_name
	 * @param replica_name
	 * @param receiver
	 * @param chunksize
	 * @param chunk_name
	 */
	private synchronized void updateChunkInfo(String file_name, String replica_name, int receiver, long chunksize,
			String chunk_name) {
		file_to_chunks.get(file_name).add(chunk_name);
		replica_to_server.put(replica_name, receiver);
		chunk_to_replica.putIfAbsent(chunk_name, new String[3]);
		String[] replicas = chunk_to_replica.get(chunk_name);
		for (int i = 0; i < replicas.length; i++) {
			if (replicas[i] == null || replicas[i].equals("")) {
				replicas[i] = replica_name;
				break;
			}
		}
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
		chunk_lasttime_updated.put(chunk_name, timeString);
		chunk_size.put(chunk_name, chunksize);
	}

	public static void main(String[] args) {
		MetaServer server = new MetaServer();
		server.init();

		/*
		 * DataMessage message = new DataMessage(); message.setClock(-1);
		 * message.setContent("null"); message.setOffset(1); message.setReceiver(3);
		 * message.setSender(server.id); message.setType(MsgType.CREATE);
		 * server.private_Message(3, MsgType.CREATE);
		 */
	}
}
