package cs6378.node;

import cs6378.message.Message;

/**
 * @author 29648
 * Node means one machine in the network, MetaServer, Normal Server and Client all implement this class
 */
public interface Node {
	
	/**
	 * process a message received by listener
	 * @param message
	 */
	public void process_Message(Message message);
	
	
	/**
	 * @param receiver id of node to receive message
	 * @param type of sent message
	 */
	public void private_Message(int receiver, String type, String chunk_name);
	
	/**
	 * send a private message
	 * @param message to be sent
	 */
	public void private_Message(Message message);
}
