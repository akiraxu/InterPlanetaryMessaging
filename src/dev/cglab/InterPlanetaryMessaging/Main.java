package dev.cglab.InterPlanetaryMessaging;
public class Main {
	
	public static void main(String[] args) {
		IpmServer server = null;
		IpmUI ui;
		Thread daemon = null;
		if(args.length == 4) {
			server = new IpmServer(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
			daemon = new Thread(server);
		}else if(args.length == 1) {
			(new IpmServer()).run();
		}else {
			server = new IpmServer("127.0.0.1", 0, "45.77.158.95", 5654);
			daemon = new Thread(server);
		}
		if(server != null) {
			ui = new IpmUI(server);
			server.setUI(ui);
			daemon.start();
		}
	}
}
