package dev.cglab.InterPlanetaryMessaging;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.json.*;

import com.dosse.upnp.UPnP;

public class IpmServer implements Runnable {
	
	class SendItem{
		public String addr;
		public int port;
		public byte[] data;
		
		int tries;
		
		SendItem(String addr, int port, byte[] data){
			this.addr = addr;
			this.port = port;
			this.data = data;
			tries = 0;
		}
	}

	public DatagramSocket aSocket;
	public DatagramChannel aChannel;
	public IpmDhtData DHT;
	public IpmKey serverKey;
	public String host;
	public int port;
	public String thost;
	public int tport;
	
	public String priv_host;
	
	boolean trackerMode;
	
	KeyPair key_pair;
	
	public HashMap<IpmKey, IpmOtrIpfs> chats;
	
	public HashMap<IpmKey, String> addrMapping;
	
	public ArrayDeque<SendItem> sendingQueue;
	
	public BufferedReader cmd_input;
	
	public  IpmUI ui = null;
	
	public IpmServer() {
		this("127.0.0.1", 5654, "127.0.0.1", 5654);
		trackerMode = true;
		System.out.println("In tracker mode, no message allowed");
	}
	
	public IpmServer(String in_host, int in_port, String in_thost, int in_tport) {
		// TODO Auto-generated method stub
		host = in_host;
		port = in_port;
		thost = in_thost;
		tport = in_tport;
		
		serverKey = new IpmKey();
		DHT = new IpmDhtData(serverKey);

		chats = new HashMap<IpmKey, IpmOtrIpfs>();
		
		addrMapping = new HashMap<IpmKey, String>();
		
		try {
			KeyPairGenerator rsa_key_generator = KeyPairGenerator.getInstance("RSA");
			rsa_key_generator.initialize(2048, new SecureRandom());
			key_pair = rsa_key_generator.generateKeyPair();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		cmd_input = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("Server key is: " + Base64.getEncoder().encodeToString(serverKey.key));
		
		trackerMode = false;
		
		//server();
		//send(thost, tport, (new IpmPacket()).getByte());
	}
	
	public void setUI(IpmUI ui) {
		this.ui  = ui;
	}
	
	public void update() throws Exception {
		TimeUnit.MILLISECONDS.sleep(10);
		userCommand();
		if(new Random().nextDouble() < 0.0005) {
			if(tport != port) {
				//send(thost, tport, (new IpmPacket(serverKey, host, port, new IpmKey(), IpmPacket.PING)).getByte());
				send(thost, tport, serverKey, host, port, new IpmKey(), IpmPacket.FIND_NODE);
			}
		}
	}
	
	public void run() {
		try {
			aChannel = DatagramChannel.open();
			aChannel.bind(new InetSocketAddress(port));
			aChannel.configureBlocking(false);
			port = ((InetSocketAddress)(aChannel.getLocalAddress())).getPort();
			UPnP.waitInit();
			if(UPnP.isUPnPAvailable()) {
				if(UPnP.openPortUDP(port)) {
					System.out.println("Map port " + port + " on " + UPnP.getExternalIP() + " success");
					uiAppend("Map port " + port + " on " + UPnP.getExternalIP() + " success");
				}else {
					System.out.println("Map port failed");
				}
			}else {
				System.out.println("No UPnP");
			}
			
			uiAppend("Server key: " + serverKey.base64());
			
			host = priv_host = UPnP.getLocalIP();
			
			if(tport != port) {
				send(thost, tport, serverKey, host, port, new IpmKey(), IpmPacket.PING);
				//send(thost, tport, (new IpmPacket(serverKey, host, port, new IpmKey(), IpmPacket.FIND_NODE)).getByte());
			}
			while(true) {
				Selector selector = Selector.open();
				SelectionKey selKey = aChannel.register(selector, SelectionKey.OP_READ);
				update();
				while(selector.selectNow() > 0){
					Set<SelectionKey> selectionKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = selectionKeys.iterator();
					while (iterator.hasNext()) {
						SelectionKey selectionKey = iterator.next();
						if (selectionKey.isReadable()) {
							ByteBuffer buffer = ByteBuffer.allocate(1024);
							InetSocketAddress anAddr = (InetSocketAddress)(aChannel.receive(buffer));
							buffer.clear();
							IpmPacket receivedPacket = new IpmPacket(buffer.array());
							IpmKey clientKey = receivedPacket.id;
							System.out.println(anAddr.toString() + " " + receivedPacket.ip + ":" + receivedPacket.port + " " + serverKey.distanceBits(clientKey) + " " + receivedPacket.type + " " + Base64.getEncoder().encodeToString(clientKey.key) + " | " + DHT.printBucketsSize());
							handleRequest(receivedPacket, anAddr.getAddress().getHostAddress(), anAddr.getPort());
						}
					 }
				}
				selKey.cancel();
				selector.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void handleRequest(IpmPacket receivedPacket, String clientIP, int clientPort) throws Exception {
		boolean rc = DHT.put(receivedPacket);
		
		switch(receivedPacket.type) {
		case IpmPacket.PING:
			addrMapping.put(receivedPacket.id, clientIP);
			send(receivedPacket.ip, receivedPacket.port, serverKey, host, port, receivedPacket.id, IpmPacket.ACK);
			if(! clientIP.equals(receivedPacket.ip)) {
				send(clientIP, clientPort, receivedPacket.id, clientIP, clientPort, new IpmKey(), IpmPacket.ACK);	
			}
			break;
		case IpmPacket.ACK:
			addrMapping.put(receivedPacket.id, clientIP);
			if(receivedPacket.id.equals(serverKey) && !InetAddress.getByName(receivedPacket.ip).isSiteLocalAddress() && !receivedPacket.ip.equals("127.0.0.1")) {
				host = receivedPacket.ip;
				port = receivedPacket.port;
				System.out.println("update external ip: " + host + ":" + port);
				uiAppend("update external ip: " + host + ":" + port);
			}
			if(!receivedPacket.id.equals(serverKey) && !clientIP.equals(receivedPacket.ip)) {
				send(clientIP, clientPort, receivedPacket.id, clientIP, clientPort, new IpmKey(), IpmPacket.ACK);
			}
			break;
		case IpmPacket.STORE:
			break;
		case IpmPacket.FIND_NODE:
			if(rc) {
				send(receivedPacket.ip, receivedPacket.port, serverKey, host, port, new IpmKey(), IpmPacket.FIND_NODE);
			}
			for(IpmPacket target : DHT.searchById(receivedPacket.id)) {
				System.out.println(target.ip);
				if(!target.id.equals(receivedPacket.id)) {
					send(receivedPacket.ip, receivedPacket.port, target.id, target.ip, target.port, new IpmKey(), IpmPacket.RETURN_NODE);
				}
			}
			break;
		case IpmPacket.FIND_VALUE:
			break;
		case IpmPacket.RETURN_NODE:
			if(rc) {
				send(receivedPacket.ip, receivedPacket.port, serverKey, host, port, new IpmKey(), IpmPacket.FIND_NODE);
			}
			ping(receivedPacket.ip, receivedPacket.port);
			break;
		case IpmPacket.RETURN_VALUE:
			break;
		case IpmPacket.MESSAGE:
			
			if(trackerMode) {
				break;
			}
			
			if(!chats.containsKey(receivedPacket.id)) {
				chats.put(receivedPacket.id, new IpmOtrIpfs(key_pair.getPrivate(), key_pair.getPublic(), this));
			}
			chats.get(receivedPacket.id).rx(new String(receivedPacket.getExtra()), receivedPacket.id);
			break;
		default:
			break;
		}
	}
	
	public void findNode(IpmKey key) {
		for(IpmPacket target : DHT.searchById(key)) {
			send(target.ip, target.port, key, host, port, new IpmKey(), IpmPacket.FIND_NODE);
		}
	}
	
	public void sendMessage(IpmKey key, String cid) {
		IpmPacket receivedPacket = DHT.getById(key);
		if(receivedPacket == null) {
			findNode(key);
			System.out.println("target not found");
			return;
		}
		System.out.println("send message: " + cid);
		send(receivedPacket.ip, receivedPacket.port, serverKey, host, port, receivedPacket.id, IpmPacket.MESSAGE, cid.getBytes());
	}
	
	public void sendNew(String addr, Integer port, byte[] sendData) {
		try {
			aSocket = aSocket == null ? new DatagramSocket() : aSocket;
			InetSocketAddress target = new InetSocketAddress(addr, port);
			//byte[] sendData = text.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, target);
			aSocket.send(sendPacket);
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			if(aSocket != null) {
				//aSocket.close();
			}
		}
	}
	
	public void send(String addr, Integer port, IpmKey k, String i, int p, IpmKey d, byte t) {
		send(addr, port, k, i, p, d, t, new byte[0]);
	}
	
	public void send(String addr, Integer port, IpmKey k, String i, int p, IpmKey d, byte t, byte[] extra) {
		if(t != IpmPacket.RETURN_NODE) {
			addr = addrMapping.containsKey(d) ? addrMapping.get(d) : addr;
		}
		try {
			System.out.println("send: " + t + " -> " + addr + ":" + port + " | " + k.base64() + " " + i + ":" + p);
			InetSocketAddress target = new InetSocketAddress(addr, port);
			IpmPacket packet = new IpmPacket(k, i, p, d, t);
			packet.writeExtra(extra);
			aChannel.send(ByteBuffer.wrap(packet.getByte()), target);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void sendQueue() {
		
	}
	
	public void userCommand() {
		try {
			if(cmd_input.ready()) {
				JSONObject cmd = new JSONObject(cmd_input.readLine());
				IpmKey target = new IpmKey(Base64.getDecoder().decode(cmd.getString("target")));
				if(!chats.containsKey(target) || !chats.get(target).isReady()) {
					chats.put(target, new IpmOtrIpfs(key_pair.getPrivate(), key_pair.getPublic(), this));
					chats.get(target).init(target);
				}else {
					chats.get(target).send(cmd.getJSONObject("msg"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void uiAppend(String text) {
		if(ui != null) {
			ui.append(text);
		}
	}
	
	public void uiSend(String userid, String text) {
		try {
			JSONObject msg = new JSONObject();
			msg.put("text", text);
			
			JSONObject obj = new JSONObject();
			obj.put("target", userid);
			obj.put("msg", msg);
			
			IpmKey target = new IpmKey(Base64.getDecoder().decode(obj.getString("target")));
			if(!chats.containsKey(target) || !chats.get(target).isReady()) {
				chats.put(target, new IpmOtrIpfs(key_pair.getPrivate(), key_pair.getPublic(), this));
				chats.get(target).init(target);
			}else {
				chats.get(target).send(obj.getJSONObject("msg"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void ping(String thost, int tport) {
		send(thost, tport, serverKey, host, port, new IpmKey(), IpmPacket.PING);
		//send("127.0.0.1", tport, serverKey, "127.0.0.1", port, new IpmKey(), IpmPacket.PING);
		if(!priv_host.equals(host)) {
			send(thost, tport, serverKey, priv_host, port, new IpmKey(), IpmPacket.PING);
		}
	}
}
