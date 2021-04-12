package dev.cglab.InterPlanetaryMessaging;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class IpmUtils {
	public static String getIpfsByCid(String cid) {
		try {
			TimeUnit.SECONDS.sleep(1);
			String cmd = "ipfs cat " + cid ;// + " | xargs echo ";
			Runtime run = Runtime.getRuntime();
			System.out.println(cmd);
			Process process = run.exec(cmd);
			BufferedReader cmdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			StringBuffer buff = new StringBuffer();
			while((line = cmdout.readLine()) != null) {
				buff.append(line);
				//System.out.println(line);
			}
			return new String(Base64.getDecoder().decode(buff.toString().trim()));
			
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	public static String putStringToIpfs(String str) {
		try {
			FileWriter fw = new FileWriter("tempjson");
			fw.write(Base64.getEncoder().encodeToString(str.getBytes()));
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
			String cid = line.split(" ")[1].trim();
			
			return cid;
			
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
}
