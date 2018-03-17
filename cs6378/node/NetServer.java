package cs6378.node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs6378.message.HearbeatMessage;
import cs6378.message.Message;
import cs6378.message.MsgType;
import cs6378.util.NodeInfo;
import cs6378.util.NodeLookup;
import cs6378.util.NodeUtil;

public class NetServer implements Node {
	private final int id;
	private Set<Integer> serverNeighbors;
	private NodeLookup nodeLookup;
	private Set<Integer> clientNeighbors;
	private static final String FILEADDR = "config2.txt";
	private Integer mserverID;
	private final int port;
	private final String ip;
	private final String FILEPREFIX;

	public NetServer(String id) {
		this.id = Integer.parseInt(id);
		FILEPREFIX = ".//files" + id + "//";
		this.clientNeighbors = Collections.synchronizedSet(new HashSet<>());
		this.serverNeighbors = Collections.synchronizedSet(new HashSet<>());
		this.nodeLookup = new NodeLookup();
		NodeInfo info = NodeUtil.readConfig(FILEADDR, nodeLookup.getId_to_addr(), nodeLookup.getId_to_index());
		Map<Integer, String> typeInfos = info.getNodeInfos();
		for (Map.Entry<Integer, String> entry : typeInfos.entrySet()) {
			int nodeid = entry.getKey();
			String type = entry.getValue();
			if (this.id != nodeid && type.equals("server")) {
				serverNeighbors.add(nodeid);
			} else if (type.equals("mserver")) {
				mserverID = nodeid;
			} else if (type.equals("client")) {
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

	private void init() {
		new Thread(new NodeListener(this, port)).start();

		sendHeartbeatMessage();
	}

	private void sendHeartbeatMessage() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					private_Message(mserverID, MsgType.HEARTBEAT);
					try {
						Thread.sleep(5 * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	// list all file names from current directory
	private synchronized List<String[]> enquiryFiles() {
		List<String[]> list = new ArrayList<>();
		File folder = new File(FILEPREFIX);
		File[] files = folder.listFiles();
		Calendar cal = Calendar.getInstance();
		for (File file : files) {
			cal.setTimeInMillis(file.lastModified());
			String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
			list.add(new String[] { file.getName(), timeString });
		}
		return list;
	}

	@Override
	public synchronized void process_Message(Message message) {
		
	}

	@Override
	public synchronized void private_Message(int receiver, String type) {
		
		String addr = nodeLookup.getIP(receiver);
		int port = Integer.parseInt(nodeLookup.getPort(receiver));
		try {
			Socket socket = new Socket(addr, port);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			if (type.equals(MsgType.HEARTBEAT)) {
				List<String[]> fileList = enquiryFiles();
				HearbeatMessage message = new HearbeatMessage();
				message.setInfos(fileList);
				message.setSender(id);
				message.setReceiver(receiver);
				message.setClock(-1);
				message.setType(MsgType.HEARTBEAT);
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

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Please Input One Node ID");
			System.exit(1);
		}
		NetServer server = new NetServer(args[0]);
		server.init();
	}
}
