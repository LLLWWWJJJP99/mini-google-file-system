package cs6378.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import cs6378.message.Message;

public class NodeListener implements Runnable {
	
	private Node node;
	private final int myPort;
	public NodeListener(Node node, int port) {
		this.node = node;
		myPort = port;
	}

	public static void main(String[] args) {

	}

	@Override
	public void run() {
		try (ServerSocket server = new ServerSocket(myPort);) {
			while(true) {
				Socket accept = server.accept();
				ObjectInputStream ois = new ObjectInputStream(accept.getInputStream());
				Message message = (Message) ois.readObject();
				node.process_Message(message);
				ois.close();
				accept.close();
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

}
