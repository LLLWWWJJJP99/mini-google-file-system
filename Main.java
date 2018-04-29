import cs6378.node.MetaServer;
import cs6378.node.NetClient;
import cs6378.node.NetServer;

public class Main {

	public static void main(String[] args) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				MetaServer mServer = new MetaServer();
				mServer.init();
			}
		}).start();
		
		int[] servers = new int [] {1, 2, 3, 4, 5};
		for(int i = 0; i < servers.length; i++) {
			final int j = servers[i];
			new Thread(new Runnable() {
				@Override
				public void run() {
					NetServer mServer = new NetServer(j + "");
					mServer.init();
				}
			}).start();
		}
		
		int[] clients = new int [] {6, 7, 8};
		for(int i = 0; i < clients.length; i++) {
			final int j = clients[i];
			new Thread(new Runnable() {
				@Override
				public void run() {
					NetClient client = new NetClient(j+"");
					client.init();
					client.request_critical_section();
				}
			}).start();
		}
	}
}
