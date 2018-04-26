package cs6378.node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
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
import java.util.concurrent.ThreadLocalRandom;

import cs6378.message.MetaMessage;
import cs6378.message.DataMessage;
import cs6378.message.HeartbeatMessage;
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
		// read nodes' ip port from config file
		NodeInfo info = NodeUtil.readConfig(FILEADDR, nodeLookup.getId_to_addr(), nodeLookup.getId_to_index());
		Map<Integer, String> typeInfos = info.getNodeInfos();
		for (Map.Entry<Integer, String> entry : typeInfos.entrySet()) {
			// set ip neighbors according to their type
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
		System.out.println("My ID is " + id);
		// start to receive messages
		new Thread(new NodeListener(this, port)).start();
		// start to send heartbeat message
		sendHeartbeatMessage();
	}

	private void sendHeartbeatMessage() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					private_Message(mserverID, MsgType.HEARTBEAT, null);
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
			list.add(new String[] { file.getName(), timeString, file.length() + "" });
		}
		return list;
	}

	/**
	 * create a new chunk and append the content into it
	 * @param chuck_name chosen new chunk name
	 * @param content content to be appended to the new chunk
	 */
	private synchronized void createFileWithContent(String chuck_name, String content) {
		File file = new File(FILEPREFIX + chuck_name);
		RandomAccessFile raf = null;
		try {
			if (file.createNewFile()) {
				raf = new RandomAccessFile(file, "rw");
				long len = raf.length();
				raf.seek(len);
				raf.writeBytes("\n" + content);
			} else {
				System.err.println(file.getName() + " File Already Exisits");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * create an empty new chunk
	 * @param chuck_name
	 */
	private synchronized void create_file(String chuck_name) {
		File file = new File(FILEPREFIX + chuck_name);
		try {
			if (!file.createNewFile()) {
				System.err.println(file.getName() + " File Already Exisits");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// append a line to specific file
	private void appendLine(File file, String line) {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");
			long len = raf.length();
			raf.seek(len);
			raf.writeBytes("\n" + line);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public synchronized void process_Message(Message message) {
		String realClazz = message.getClass().getSimpleName();
		// meta server tells server to create a new chunk directly
		if (realClazz.equals(MetaMessage.class.getSimpleName())) {
			MetaMessage mMessage = (MetaMessage) message;
			if (mMessage.getType().equals(MsgType.CREATE)) {
				create_file(mMessage.getContent());
			}
		// client send message to server
		} else if (realClazz.equals(DataMessage.class.getSimpleName())) {
			DataMessage dMessage = (DataMessage) message;
			// append a new line to chosen file
			if (dMessage.getType().equals(MsgType.APPEND)) {
				appendLine(new File(FILEPREFIX + dMessage.getFilaName()), dMessage.getContent());
			// create a new chunk and insert content
			} else if (dMessage.getType().equals(MsgType.CREATE)) {
				createFileWithContent(dMessage.getFilaName(), dMessage.getContent());
			// read from chosen chunk starting at offset
			} else if(dMessage.getType().equals(MsgType.READ)) {
				String chunk_name = dMessage.getFilaName();
				String content = randomReadLine(chunk_name, dMessage.getOffset());
				System.out.println("content: " + content);
				DataMessage reply = new DataMessage();
				reply.setClock(dMessage.getClock());
				reply.setFilaName(chunk_name);
				reply.setContent(content);
				reply.setReceiver(dMessage.getSender());
				reply.setSender(id);
				reply.setType(MsgType.READ);
				private_Message(reply);
			}
		}
	}
	
	/**
	 * read lines fron chosen file starting at given offset
	 * @param chunk_name
	 * @param offset to start reading
	 * @return
	 */
	private synchronized String randomReadLine(String chunk_name, long offset) {
		StringBuffer sb = new StringBuffer();
		try (RandomAccessFile raf = new RandomAccessFile(FILEPREFIX + chunk_name, "r")) {
			long end = ThreadLocalRandom.current().nextLong(offset, raf.length());
			int len = (int) (end - offset);
			byte[] bytes = new byte[len];
			raf.readFully(bytes);
			sb.append(new String(bytes));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
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
			if (type.equals(MsgType.HEARTBEAT)) {
				List<String[]> fileList = enquiryFiles();
				HeartbeatMessage message = new HeartbeatMessage();
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

		// System.out.println(server.clientNeighbors);
		// System.out.println(server.serverNeighbors);
		// System.out.println(server.mserverID);
		// server.create_file("1234");
	}
}
