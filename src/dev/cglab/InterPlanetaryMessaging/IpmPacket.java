package dev.cglab.InterPlanetaryMessaging;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

public class IpmPacket {
	public final static byte PING = 0;
	public final static byte ACK = 1;
	public final static byte STORE = 2;
	public final static byte FIND_NODE = 3;
	public final static byte FIND_VALUE = 4;
	public final static byte RETURN_NODE = 5;
	public final static byte RETURN_VALUE = 6;
	public final static byte MESSAGE = 7;
	
	public IpmKey id;
	public String ip;
	public int port;
	public IpmKey reply;
	public byte type;
	
	public byte[] extra;
	
	public final int dataLength = IpmConfig.KEY_LENGTH * 2 + 4 + 2 + 1;
	
	public IpmPacket() {
		id = new IpmKey();
		ip = "127.0.0.1";
		port = 5654;
		reply = new IpmKey();
		type = 0;
		extra = new byte[0];
	}
	
	public IpmPacket(IpmKey k, String i, int p, IpmKey d, byte t) {
		id = k;
		ip = i;
		port = p;
		reply = d;
		type = t;
		extra = new byte[0];
	}
	
	public IpmPacket(byte[] data) throws Exception {
		//generate new checksum
		CRC32 checksum = new CRC32();
		checksum.update(data, 0, dataLength);
		long crc = checksum.getValue();
		
		//get checksum in packet
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.put(Arrays.copyOfRange(data, dataLength, dataLength + Long.BYTES));
		buffer.flip();
		
		if(crc != buffer.getLong()) {
			throw new Exception("checksum failed");
		}
		
		id = new IpmKey(Arrays.copyOfRange(data, 0, IpmConfig.KEY_LENGTH));
		
		ip = (data[IpmConfig.KEY_LENGTH] & 0xFF) + "." + (data[IpmConfig.KEY_LENGTH + 1] & 0xFF) + "." + (data[IpmConfig.KEY_LENGTH + 2] & 0xFF) + "." + (data[IpmConfig.KEY_LENGTH + 3] & 0xFF);
		
		buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.put(new byte[2]);
		buffer.put(Arrays.copyOfRange(data, IpmConfig.KEY_LENGTH + 4, IpmConfig.KEY_LENGTH + 4 + 2));
		buffer.flip();
		port = buffer.getInt();
		
		reply = new IpmKey(Arrays.copyOfRange(data, IpmConfig.KEY_LENGTH + 4 + 2, dataLength - 1));
		type = Arrays.copyOfRange(data, dataLength - 1, dataLength)[0];
		
		extra = Arrays.copyOfRange(data, dataLength + Long.BYTES, data.length);
	}
	
	public byte[] getByte() {
		byte[] out = new byte[dataLength + Long.BYTES + extra.length];
		int pos = 0;
		
		//put node id
		for(byte b : id.key) {
			out[pos] = b;
			pos++;
		}
		
		//put node ip
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		for(String b : ip.split("\\.")) {
			//buffer.flip();
			buffer.putInt(0, Integer.parseInt(b));
			out[pos] = buffer.array()[Integer.BYTES - 1];
			pos++;
		}
		
		//put node port
		//buffer.flip();
		buffer.putInt(0, port);
		out[pos] = buffer.array()[Integer.BYTES - 2];
		pos++;
		out[pos] = buffer.array()[Integer.BYTES - 1];
		pos++;
		
		//put content key
		for(byte b : reply.key) {
			out[pos] = b;
			pos++;
		}
		
		out[pos] = type;
		pos++;
		
		//checksum
		CRC32 checksum = new CRC32();
		checksum.update(out, 0, dataLength);
		long crc = checksum.getValue();
		buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(0, crc);
		for(byte b : buffer.array()) {
			out[pos] = b;
			pos++;
		}

		System.arraycopy(extra, 0, out, pos, extra.length);
		
		return out;
	}
	
	public void writeExtra(byte[] arr) {
		extra = arr;
	}
	
	public byte[] getExtra() {
		return extra;
	}
}
