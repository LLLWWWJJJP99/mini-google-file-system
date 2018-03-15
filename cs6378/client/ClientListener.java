package cs6378.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import cs6378.message.Message;

public class ClientListener implements Runnable {
	
	NetClient node;
	
	public ClientListener(NetClient node) {
		this.node = node;
	}

	public static void main(String[] args) {

	}

	@Override
	public void run() {
		try (ServerSocket server = new ServerSocket(node.getPort());) {
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
