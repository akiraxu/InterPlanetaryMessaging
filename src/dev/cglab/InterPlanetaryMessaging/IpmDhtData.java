package dev.cglab.InterPlanetaryMessaging;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class IpmDhtData {
	
	public class Bucket{
		int size;
		LinkedHashMap<IpmKey, IpmPacket> data;
		IpmPacket last = null;
		public Bucket(int n) {
			size = n;
			data = new LinkedHashMap<IpmKey, IpmPacket>(){
	            protected boolean removeEldestEntry(Map.Entry<IpmKey, IpmPacket> eldest) {
	            	last = eldest.getValue();
	                return size() > n;
	            }
	        };
		}
		public boolean put(IpmPacket item) {
			boolean rc = data.remove(item.id) == null;
			/*
			if(data.size() >= size && size > 0) {
				try {
					Field tail = data.getClass().getDeclaredField("tail");
					tail.setAccessible(true);
					data.remove(((Entry<IpmKey, IpmPacket>)(tail.get(data))).getKey());
				} catch (Exception e) {
					//data.remove(data.entrySet().iterator().next().getKey());
					data.remove(((Entry<IpmKey, IpmPacket>)(data.entrySet().toArray()[data.size() - 1])).getKey());
				}
			}
			*/
			data.put(item.id, item);
			if(last != null) {
				//ping last
				last = null;
			}
			return rc; //return true if the item not exist yet
		}
		public IpmPacket get(IpmKey key) {
			return data.get(key);
		}
		public IpmPacket[] getAll() {
			return data.values().toArray(new IpmPacket [0]);
		}
	}
	
	public final IpmKey myId;
	public Bucket[] buckets;
	
	public IpmDhtData(IpmKey id) {
		int k = IpmConfig.KEY_LENGTH * Byte.SIZE;
		myId = id;
		buckets = new Bucket[k+1];
		for(int i = 0; i <= k; i++) {
			buckets[i] = new Bucket(i);
		}
	}
	
	public boolean put(IpmPacket item) {
		int k = myId.distanceBits(item.id);
		//System.out.println("Put in " + k);
		return buckets[k].put(item);
	}
	
	public IpmPacket[] bulkPut(IpmPacket[] items) {
		ArrayList<IpmPacket> list = new ArrayList<IpmPacket>();
		for(IpmPacket item : items) {
			if(put(item)) {
				list.add(item);
			}
		}
		return (IpmPacket[]) list.toArray(); //return new added elements
	}
	
	public IpmPacket[] searchById(IpmKey key) {
		int distance = key.distanceBits(myId);
		IpmPacket[] out = buckets[distance].getAll();
		while(out.length == 0 && distance < buckets.length) {
			out = buckets[distance].getAll();
			distance ++;
		}
		while(out.length == 0 && distance > 0) {
			distance --;
			out = buckets[distance].getAll();
		}
		return out;
	}
	
	public IpmPacket getById(IpmKey key) {
		return buckets[key.distanceBits(myId)].get(key);
	}
	
	public String printBucketsSize() {
		String str = "";
		for(Bucket b : buckets) {
			str += b.data.size() + ",";
		}
		return str;
	}
}
