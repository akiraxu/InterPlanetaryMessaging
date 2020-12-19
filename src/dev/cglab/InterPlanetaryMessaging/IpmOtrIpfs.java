package dev.cglab.InterPlanetaryMessaging;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class IpmOtrIpfs extends IpmOtr {
	
	public String cid; 
	public JSONObject obj;
	public IpmServer server;
	public IpmKey ipmkey;

	public IpmOtrIpfs(PrivateKey priv, PublicKey pub, IpmServer server) throws Exception {
		super(priv, pub);
		cid = null;
		this.server = server;
	}

	@Override
	public void tx(JSONObject obj) {
		this.cid = null;
		try {
			FileWriter fw = new FileWriter("tempjson");
			fw.write(Base64.getEncoder().encodeToString(obj.toString().getBytes()));
			fw.close();
			
			//String cmd = "ipfs add tempjson 2>/dev/null| tr -d '\\n'| tr -d '\\r' | sed 's/.*added\\s*\\(\\w*\\)\\s*tempjson.*/\\1/'; rm tempjson";
			String cmd = "ipfs add tempjson";
			Runtime run = Runtime.getRuntime();
			Process process = run.exec(cmd);
			BufferedReader cmdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
			/*String line;
			StringBuffer buff = new StringBuffer();
			cmdout.read
			while((line = cmdout.readLine()) != null) {
				buff.append(line);
			}
			cid = buff.toString().trim();
			*/
			String line = cmdout.readLine();
			cid = line.split(" ")[1].trim();
			
			server.sendMessage(ipmkey, cid);
			
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void rx(JSONObject obj) {
		try {
			this.obj = dispatcher(obj);
			if(this.obj != null) {
				System.out.println(this.obj);
				server.uiAppend(ipmkey.base64() + " says: " + this.obj.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void rx(String cid, IpmKey key) {
		ipmkey = key;
		rx(cid.trim());
	}
	
	public void init(IpmKey key) throws Exception {
		ipmkey = key;
		bob_init();
	}
	
	public JSONObject rx(String cid) {
		this.obj = null;
		try {
			TimeUnit.SECONDS.sleep(1);
			String cmd = "ipfs cat " + cid ;// + " | xargs echo ";
			Runtime run = Runtime.getRuntime();
			Process process = run.exec(cmd);
			BufferedReader cmdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			StringBuffer buff = new StringBuffer();
			while((line = cmdout.readLine()) != null) {
				buff.append(line);
				//System.out.println(line);
			}
			rx(new JSONObject(new String(Base64.getDecoder().decode(buff.toString().trim()))));
			
			return this.obj;
			
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

}
