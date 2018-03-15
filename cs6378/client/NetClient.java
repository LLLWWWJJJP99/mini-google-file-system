package cs6378.client;

import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cs6378.algo.Ricart_Agrawala_Algorithm;
import cs6378.message.Message;
import cs6378.message.MsgType;
import cs6378.util.FileUtil;
import cs6378.util.NodeLookup;

public class NetClient {
	private final int id;
	private Set<Integer> neighbors;
	private NodeLookup nodeLookup;
	private static final String FILEADDR = "config2.txt";
	private Set<Integer> login_nodes;
	private final String ip;
	private final int port;
	private static final String OUTPUT_TEST = "test.txt";
	private Ricart_Agrawala_Algorithm algorithm;

	public NetClient(String id) {
		this.id = Integer.parseInt(id);
		this.nodeLookup = new NodeLookup();
		this.neighbors = FileUtil.readConfig(FILEADDR, id, nodeLookup.getId_to_addr(), nodeLookup.getId_to_index());
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
		algorithm = new Ricart_Agrawala_Algorithm(neighbors.size(), id);
		login_nodes = Collections.synchronizedSet(new HashSet<>());

		new Thread(new ClientListener(this)).start();

		while (!login_nodes.equals(neighbors)) {
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
			algorithm.setOutstanding_reply_count(neighbors.size());
			algorithm.local_event();
			algorithm.setOur_request_clock(algorithm.getClock());
		}
		for (int neighbor : neighbors) {
			private_Message(neighbor, type);
		}
	}

	public synchronized void private_Message(int receiver, String type) {
		//?????????????????????
		if(type.equals(MsgType.REPLY)) {
			algorithm.local_event();
		}
		
		String addr = nodeLookup.getIP(receiver);
		int port = Integer.parseInt(nodeLookup.getPort(receiver));
		try {
			Socket socket = new Socket(addr, port);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			Message message = new Message();
			message.setReceiver(receiver);
			message.setSender(id);
			message.setType(type);
			message.setClock(algorithm.getClock());
			oos.writeObject(message);
			oos.close();
			socket.close();
		} catch (ConnectException e) {
			System.out.println("Please Wait For Other Clients To Be Started");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void process_Message(Message message) {
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
			}
		}
	}
	
	private void log() {
		try(FileWriter fw = new FileWriter(OUTPUT_TEST, true)){
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
			System.out.println("times:" + times);
		}
		System.out.println("I am Done");
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Please Input ID");
			System.exit(1);
		}
		NetClient node = new NetClient(args[0]);
		node.init();
		node.request_critical_section();
	}

}
