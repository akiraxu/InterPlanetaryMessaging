package dev.cglab.InterPlanetaryMessaging;

import java.util.LinkedHashMap;

public class ImpDhtStore {
	
	LinkedHashMap<IpmKey, LinkedHashMap<IpmKey,IpmPacket>> data;
	
	public ImpDhtStore() {
		data = new LinkedHashMap<IpmKey, LinkedHashMap<IpmKey,IpmPacket>>();
	}
	
	public void put(IpmKey key, IpmPacket src) {
		LinkedHashMap<IpmKey,IpmPacket> store;
		if(data.get(key) == null) {
			store = new LinkedHashMap<IpmKey,IpmPacket>();
		}
		data.get(key).put(src.id, src);
	}
	public IpmPacket[] get(IpmKey key) {
		if(data.get(key) == null) {
			return null;
		}
		return (IpmPacket[]) data.get(key).values().toArray();
	}
}
