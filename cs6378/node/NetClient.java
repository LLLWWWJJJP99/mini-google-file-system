package cs6378.node;

import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cs6378.algo.Ricart_Agrawala_Algorithm;
import cs6378.message.MetaMessage;
import cs6378.message.DataMessage;
import cs6378.message.Message;
import cs6378.message.MsgType;
import cs6378.util.NodeUtil;
import cs6378.util.NodeInfo;
import cs6378.util.NodeLookup;

public class NetClient implements Node {
	private final int id;
	private Set<Integer> clientNeighbors;
	private NodeLookup nodeLookup;
	private static final String FILEADDR = "config2.txt";
	private Set<Integer> login_nodes;
	private final String ip;
	private final int port;
	private static final String OUTPUT_TEST = "test.txt";
	private Set<Integer> serverNeighbors;
	private Ricart_Agrawala_Algorithm algorithm;
	private Integer mserverID;

	public NetClient(String id) {
		this.id = Integer.parseInt(id);
		this.serverNeighbors = Collections.synchronizedSet(new HashSet<>());
		this.clientNeighbors = Collections.synchronizedSet(new HashSet<>());
		this.nodeLookup = new NodeLookup();
		NodeInfo info = NodeUtil.readConfig(FILEADDR, nodeLookup.getId_to_addr(), nodeLookup.getId_to_index());
		Map<Integer, String> typeInfos = info.getNodeInfos();
		for (Map.Entry<Integer, String> entry : typeInfos.entrySet()) {
			int nodeid = entry.getKey();
			String type = entry.getValue();
			if (type.equals("server")) {
				serverNeighbors.add(nodeid);
			} else if (type.equals("mserver")) {
				mserverID = nodeid;
			} else if (nodeid != this.id && type.equals("client")) {
				clientNeighbors.add(nodeid);
			}
		}

		if (mserverID == null) {
			System.err.println("Please check config file, it must has one mserver");
			System.exit(1);
		}

		this.ip = nodeLookup.getIP(this.id);
		this.port = Integer.parseInt(nodeLookup.getPort(this.id));
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	private void init() {
		algorithm = new Ricart_Agrawala_Algorithm(clientNeighbors.size(), id);
		login_nodes = Collections.synchronizedSet(new HashSet<>());

		new Thread(new NodeListener(this, port)).start();

		while (!login_nodes.equals(clientNeighbors)) {
			broadcast(MsgType.LOGIN);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		login_nodes.clear();
		System.out.println(this.id + " sets up finely");
	}

	public synchronized void broadcast(String type) {
		if (type.equals(MsgType.REQUEST)) {
			algorithm.setOutstanding_reply_count(clientNeighbors.size());
			algorithm.local_event();
			algorithm.setOur_request_clock(algorithm.getClock());
		}
		for (int neighbor : clientNeighbors) {
			private_Message(neighbor, type);
		}
	}

	public synchronized void private_Message(DataMessage message) {
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
	public synchronized void private_Message(int receiver, String type) {
		if (type.equals(MsgType.REPLY)) {
			algorithm.local_event();
		}

		String addr = nodeLookup.getIP(receiver);
		int port = Integer.parseInt(nodeLookup.getPort(receiver));
		try {
			Socket socket = new Socket(addr, port);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			if (type.equals(MsgType.REQUEST) || type.equals(MsgType.REPLY) || type.equals(MsgType.LOGIN)
					|| type.equals(MsgType.LOGIN_SUCCESS)) {
				Message message = new Message();
				message.setReceiver(receiver);
				message.setSender(id);
				message.setType(type);
				message.setClock(algorithm.getClock());
				oos.writeObject(message);
			} else if (type.equals(MsgType.CREATE) || type.equals(MsgType.READ) || type.equals(MsgType.APPEND)) {
				MetaMessage message = new MetaMessage();
				message.setReceiver(receiver);
				message.setSender(id);
				message.setType(type);
				message.setClock(algorithm.getClock());
				// message.setOffset(-1);
				// message.setContent("null");
				oos.writeObject(message);
			}
			oos.close();
			socket.close();
		} catch (ConnectException e) {
			System.out.println("Please Wait For Other Clients To Be Started");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void process_Message(Message message) {
		String realClazz = message.getClass().getSimpleName();
		if (realClazz.equals(Message.class.getSimpleName())) {
			System.out.println(id + " receives message " + message);
			if (message.getType().equals(MsgType.LOGIN)) {
				System.out.println(message.getSender() + " login to " + message.getReceiver());
				private_Message(message.getSender(), MsgType.LOGIN_SUCCESS);
			} else if (message.getType().equals(MsgType.LOGIN_SUCCESS)) {
				login_nodes.add(message.getSender());
			} else if (message.getType().equals(MsgType.REPLY)) {
				algorithm.process_reply(message);
			} else if (message.getType().equals(MsgType.REQUEST)) {
				if (algorithm.process_request(message)) {
					private_Message(message.getSender(), MsgType.REPLY);
				} else {
					System.err.println("Message is defered: " + message);
					System.err.println("My req time: " + algorithm.getOur_request_clock());
				}
			}
		} else if (realClazz.equals(MetaMessage.class.getSimpleName())) {
			MetaMessage real = (MetaMessage) message;
			if (real.getType().equals(MsgType.FAILURE)) {
				String[] failureInfo = real.getContent().split("\\s+");
				System.err.println(
						failureInfo[1] + " is already dead, your " + failureInfo[0] + " fails at " + real.getClock());
			} else if(real.getType().equals(MsgType.READ)) {
				
				String[] chunk_infos = real.getContent().split("\\*");
				int send_to_server = Integer.parseInt(chunk_infos[1].trim());
				DataMessage dMessage = new DataMessage();
				dMessage.setFilaName(chunk_infos[0]);
				dMessage.setOffset(real.getOffset());
				dMessage.setSender(id);
				dMessage.setReceiver(send_to_server);
				dMessage.setClock(algorithm.getClock());
				dMessage.setContent("");
				dMessage.setType(MsgType.READ);
				private_Message(dMessage);
			} else {
				String content = real.getContent();
				String[] strs = content.split("\\*");
				DataMessage dMessage = new DataMessage();
				dMessage.setClock(algorithm.getClock());
				dMessage.setContent(strs[0]);
				dMessage.setFilaName(strs[1]);
				int chosen_server = Integer.parseInt(strs[2]);
				dMessage.setOffset(-1);
				dMessage.setReceiver(chosen_server);
				dMessage.setSender(id);

				if (real.getType().equals(MsgType.CREATE)) {
					dMessage.setType(MsgType.CREATE);
					private_Message(dMessage);
				} else if (real.getType().equals(MsgType.APPEND)) {
					dMessage.setType(MsgType.APPEND);
					private_Message(dMessage);
				}
			}
		} else if (realClazz.equals(DataMessage.class.getSimpleName())) {
			DataMessage dMessage = (DataMessage) message;
			if(dMessage.getType().equals(MsgType.READ)) {
				System.out.println(id + " reads " + dMessage.getContent() + " from " + dMessage.getFilaName() + " at " + dMessage.getSender());
			}
		}
	}

	private void log() {
		try (FileWriter fw = new FileWriter(OUTPUT_TEST, true)) {
			fw.write(this.id + " enters cs at:" + algorithm.getClock() + "\n");
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void request_critical_section() {
		int times = 0;
		while (times < 10) {

			algorithm.setReques_critical_section(true);
			broadcast(MsgType.REQUEST);
			while (!algorithm.check_critical_section()) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println(id + " enters cs ");
			// enter critical_section
			enter_critical_section();
			System.out.println(algorithm.getClock());
			log();
			// exit_critical_section
			for (int neighbor : algorithm.getDefered_reply()) {
				private_Message(neighbor, MsgType.REPLY);
			}
			algorithm.getDefered_reply().clear();
			algorithm.setReques_critical_section(false);
			System.out.println(id + " exits cs ");
			times++;
			
			int waitTime = 20;
			
			while(waitTime > 0) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				waitTime--;
				if(waitTime == 10) {
					System.err.println("Wait for 10 seconds to make next request");
				}else if(waitTime == 5) {
					System.err.println("Wait for 5 seconds to make next request");
				}else if(waitTime == 15) {
					System.err.println("Wait for 15 seconds to make next request");
				}
			}
		}
		System.out.println("I am Done");
	}

	private void enter_critical_section() {
		Random rand = new Random();
		int choice = rand.nextInt(5);
		if (choice >= 2) {
			private_Message(mserverID, MsgType.READ);
		} else if (choice == 1) {
			private_Message(mserverID, MsgType.READ);
		} else if (choice == 0) {
			private_Message(mserverID, MsgType.READ);
		}
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Please Input ID");
			System.exit(1);
		}
		NetClient node = new NetClient(args[0]);
		node.init();
		// System.out.println(node.clientNeighbors);
		// System.out.println(node.serverNeighbors);
		// System.out.println(node.mserverID);
		node.request_critical_section();
	}

}
