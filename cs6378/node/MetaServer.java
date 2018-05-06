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
import cs6378.message.CommitMessage;
import cs6378.message.HeartbeatMessage;
import cs6378.message.Message;
import cs6378.message.MsgType;
import cs6378.message.WakeUpMessage;
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
	private Map<String, Long> replica_size;
	private Map<String, String[]> chunk_to_replica;
	private Map<String, String> replica_to_chunk;
	private Map<String, Integer> replica_version;
	private Integer id;
	private NodeLookup nodeLookup;
	private static final String FILEADDR = "config2.txt";
	private Set<Integer> clientNeighbors;
	private final int port;
	private final String ip;
	private static int filename = 0;

	public MetaServer() {
		this.replica_size = Collections.synchronizedMap(new HashMap<>());
		this.serverNeighbors = Collections.synchronizedMap(new HashMap<>());
		this.clientNeighbors = Collections.synchronizedSet(new HashSet<>());
		this.servers_lasttime_heartbeat = Collections.synchronizedMap(new HashMap<>());
		this.replica_to_server = Collections.synchronizedMap(new HashMap<>());
		this.chunk_lasttime_updated = Collections.synchronizedMap(new HashMap<>());
		this.chunk_to_replica = Collections.synchronizedMap(new HashMap<>());
		this.replica_to_chunk = Collections.synchronizedMap(new HashMap<>());
		this.replica_version = Collections.synchronizedMap(new HashMap<>());
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
		for(Map.Entry<String, String[]> entry : chunk_to_replica.entrySet()) {
			String[] replicas = entry.getValue();
			for(String replica : replicas) {
				replica_version.put(replica, 0);
				replica_to_chunk.put(replica, entry.getKey());
			}
		}
	}

	public void init() {
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
		System.err.println(id + " receivers " + message);
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
				replica_size.put(info[0], Long.parseLong(info[2].trim()));
			}
			// accept an message from client
		} else if (realClazz.equals(MetaMessage.class.getSimpleName())) {
			MetaMessage dMessage = (MetaMessage) message;
			// Integer chosen_server = choose_server();
			// check if chosen server is dead then return failure message

			// send create message to chosen server
			if (dMessage.getType().equals(MsgType.CREATE)) {
				Integer[] chosen_servers = choose_three_servers();

				boolean any_dead = false;
				for (Integer server : chosen_servers) {
					any_dead |= is_server_dead(server);
				}

				if (!any_dead) {
					String chunk_name = UUID.randomUUID().toString();
					for (Integer server : chosen_servers) {
						if (serverNeighbors.get(server)) {
							private_Message(server, MsgType.CREATE, chunk_name);
						}
					}
					filename++;
				} else {
					StringBuilder sb = new StringBuilder();
					for (Integer server : chosen_servers) {
						if (is_server_dead(server)) {
							sb.append(server);
							sb.append(",");
						}
					}
					System.err.println("Chosen servers: " + sb.toString() + " are dead ");
					send_failure(dMessage, "CREATE_OPS_for_" + sb.toString());
				}
				/*System.out.println("<========================================>");
				for (Map.Entry<String, String[]> entry : chunk_to_replica.entrySet()) {
					String[] replicas = entry.getValue();
					String chunk = entry.getKey();
					String part2 = String.join(",", replicas);
					System.out.println(chunk + " || " + part2);
				}
				System.out.println("<========================================>");*/
				// send append message to chosen server
			} else if (dMessage.getType().equals(MsgType.APPEND)) {
				// randomly choose a file and check whether its last chunk has enough to insert
				// new lines
				List<String> file_list = new ArrayList<>(file_to_chunks.keySet());
				Random rand = new Random();
				String chosen_file = file_list.get(rand.nextInt(file_list.size()));
				List<String> chunks = file_to_chunks.get(chosen_file);

				String chosen_chunk = chunks.get(chunks.size() - 1);

				int choice = rand.nextInt(10);
				String content = "\n_Add_New_Line";
				// randomly generate new lines
				/*if (choice >= 6) {
					byte[] bytes = new byte[2048];
					Arrays.fill(bytes, (byte) 1);
					content = new String(bytes);
				}*/
				content += "_at_" + dMessage.getClock();
				String[] chosen_replicas = chunk_to_replica.get(chosen_chunk);
				System.out.println("<==============Append Operations Starts=================>");
				for(String chosen_replica : chosen_replicas) {
					System.out.println("chosen_replica: " + chosen_replica);
					long chosen_replica_size = replica_size.get(chosen_replica);
					long content_size = content.getBytes().length;
					// if new lines exceed last chunk's size limit then tell client to create a new
					// chunk on chosen server
					if (8192l - chosen_replica_size < content_size) {
						String new_chunk_name = UUID.randomUUID().toString();
						Integer[] three_alive_servers = choose_three_servers(true);
						
						for (Integer server : three_alive_servers) {
							private_Message(server, MsgType.CREATE, new_chunk_name);
						}
						break;
					} else {
						Integer related_server = replica_to_server.get(chosen_replica);
						if (serverNeighbors.get(related_server)) {
							System.out.println("Sent to alive Server:" + content);
							MetaMessage reply = new MetaMessage();
							reply.setOffset(-1);
							reply.setClock(dMessage.getClock());
							reply.setReceiver(dMessage.getSender());
							reply.setSender(id);

							// otherwise go last chunk 's corresponding server and append new lines
							reply.setType(MsgType.APPEND);
							reply.setContent(content + "*" + chosen_replica + "*" + related_server);
							
							//update replica version number
							replica_version.put(chosen_replica, replica_version.get(chosen_replica) + 1);
							private_Message(reply);
						}
					}
				}
				System.out.println("<==============Append Operations Ends=================>");
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
				for (String replica : chosen_replicas) {
					Integer corresponding_server = replica_to_server.get(replica);
					if (serverNeighbors.get(corresponding_server)) {
						all_dead = false;
						chosen_replica = replica;
						break;
					}
				}

				if (all_dead) {
					System.err.println(" Meta Server found all file server die");
					send_failure(dMessage, chosen_chunk);

				} else {
					int replic_server = replica_to_server.get(chosen_replica);
					long limit = replica_size.get(chosen_replica);
					long offset = limit == 0 ? 0l : ThreadLocalRandom.current().nextLong(limit);
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
		} else if (realClazz.equals(CommitMessage.class.getSimpleName())) {
			CommitMessage cmMessage = (CommitMessage) message;
			if(cmMessage.getType().equals(MsgType.GET_ALIVE_SERVERS)) {
				Set<Integer> alive_servers = new HashSet<>();
				for(Map.Entry<Integer, Boolean> server : serverNeighbors.entrySet()) {
					if(server.getValue()) {
						alive_servers.add(server.getKey());
					}
				}
				System.err.println( id + " sends alive_servers: " + alive_servers);
				CommitMessage get_alive_servers = new CommitMessage.CommitMessageBuilder()
						.clock(0)
						.alive_servers(alive_servers)
						.receiver(cmMessage.getSender())
						.sender(cmMessage.getReceiver())
						.type(MsgType.GET_ALIVE_SERVERS)
						.build();
				private_Message(get_alive_servers);
			}else {
				System.err.println("Wrong Type Commit Message:");
			}
		}else if(realClazz.equals(WakeUpMessage.class.getSimpleName())) {
			WakeUpMessage wMessage = (WakeUpMessage) message;
			if(wMessage.getType().equals(MsgType.QUERY_VERSION)) {
				int receiver = wMessage.getSender();
				String replica_name = wMessage.getReplica();
				int version = wMessage.getVersion();
				String related_chunk = replica_to_chunk.get(replica_name);
				String[] replicas = chunk_to_replica.get(related_chunk);
				int max = 0;
				String chosen_replica = null;
				for(String replica : replicas) {
					if(!replica.equals(replica_name)) {
						int other_version = replica_version.get(replica);
						if(other_version > max) {
							max = other_version;
							chosen_replica = replica;
						}
					}
				}
				if(max == version) {
					WakeUpMessage query_version = new WakeUpMessage.WakeUpMessageBuilder()
							.content("")
							.version(-1)
							.replica(replica_name)
							.build();
					query_version.setReceiver(receiver);
					query_version.setSender(id);
					query_version.setType(MsgType.QUERY_VERSION);
					private_Message(query_version);
					System.err.println("*query_version 1: " + query_version);
				}else {
					Integer related_server = replica_to_server.get(chosen_replica);
					WakeUpMessage query_version = new WakeUpMessage.WakeUpMessageBuilder()
							.remote(related_server)
							.content("")
							.replica(chosen_replica)
							.recover_replica(replica_name)
							.version(max)
							.build();
					query_version.setReceiver(receiver);
					query_version.setSender(id);
					query_version.setType(MsgType.QUERY_VERSION);
					private_Message(query_version);
					System.err.println("*query_version 1: " + query_version);
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
	
	private synchronized Integer[] choose_three_servers(boolean alive) {
		Integer[] chosen_servers = new Integer[3];
		int index = 0;
		List<Integer> list = new ArrayList<>();
		for(Integer server : serverNeighbors.keySet()) {
			if(serverNeighbors.get(server) == alive) {
				list.add(server);
			}
		}
		if(list.size() < 3) {
			throw new RuntimeException(" # of Alive Sever is below 3 ");
		}
		
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
	 * @param replicasize
	 * @param chunk_name
	 */
	private synchronized void updateChunkInfo(String file_name, String replica_name, int receiver, long replicasize,
			String chunk_name) {
		file_to_chunks.get(file_name).add(chunk_name);
		replica_to_server.put(replica_name, receiver);
		chunk_to_replica.putIfAbsent(chunk_name, new String[3]);
		replica_to_chunk.putIfAbsent(replica_name, chunk_name);
		String[] replicas = chunk_to_replica.get(chunk_name);
		for (int i = 0; i < replicas.length; i++) {
			if (replicas[i] == null || replicas[i].equals("")) {
				replicas[i] = replica_name;
				break;
			}
		}
		replica_version.put(replica_name, 0);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
		chunk_lasttime_updated.put(chunk_name, timeString);
		replica_size.put(replica_name, replicasize);
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
