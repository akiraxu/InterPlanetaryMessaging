package dev.cglab.InterPlanetaryMessaging;

import java.security.PrivateKey;
import java.security.PublicKey;

import org.json.JSONArray;

public class IpmSnsIpfs extends IpmSns {

	public IpmSnsIpfs(PrivateKey priv, PublicKey pub, IpmServer server) throws Exception {
		super(priv, pub, server);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String get() {
		return IpmUtils.putStringToIpfs(deliverable_str);
	}
	
	@Override
	public JSONArray readNotes(String cid) {
		return super.readNotes(IpmUtils.getIpfsByCid(cid));
	}

}
