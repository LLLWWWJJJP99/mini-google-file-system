package cs6378.node;

import cs6378.message.Message;

/**
 * @author 29648
 * Node means one machine in the network, MetaServer, Normal Server and Client all implement this class
 */
public interface Node {
	public void process_Message(Message message);
	
	public void private_Message(int receiver, String type);
}
