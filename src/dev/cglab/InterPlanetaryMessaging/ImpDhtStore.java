package dev.cglab.InterPlanetaryMessaging;

import java.util.ArrayList;
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
			data.put(key, store);
		}
		data.get(key).put(src.id, src);
		System.out.println(key.base64());
	}
	public ArrayList<IpmPacket> get(IpmKey key) {
		System.out.println(key.base64());
		if(data.get(key) == null) {
			return null;
			/*
			LinkedHashMap<IpmKey,IpmPacket> all = new LinkedHashMap<IpmKey,IpmPacket>();
			for(LinkedHashMap<IpmKey,IpmPacket> val : data.values()) {
				for(IpmPacket pkg : val.values()) {
					all.put(pkg.id, pkg);
				}
			}
			return (IpmPacket[]) all.values().toArray();
			*/
		}

		return new ArrayList<IpmPacket>(data.get(key).values());
	}
}
