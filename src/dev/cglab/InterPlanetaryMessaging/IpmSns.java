package dev.cglab.InterPlanetaryMessaging;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class IpmSns {
	
	public class Post{
		public long time;
		public String text;
		public Post(long t, String str) {
			time = t;
			text = str;
		}
	}
	
	PrivateKey priv_key;
	PublicKey pub_key;
	MessageDigest digest;
	Random rand;
	ArrayList<Post> posts;
	JSONObject postsJson;
	JSONObject deliverable;
	String deliverable_str;
	int packNum = 10;
	
	public IpmSns(PrivateKey priv, PublicKey pub, IpmServer server) throws Exception {
		rand = new SecureRandom(String.valueOf(System.currentTimeMillis()).getBytes());
		digest = MessageDigest.getInstance("SHA-256");
		priv_key = priv;
		pub_key = pub;
		posts = new ArrayList<Post>();
		postsJson = new JSONObject();
		postsJson.put("pub", Base64.getEncoder().encodeToString(pub_key.getEncoded()));
		postsJson.put("ver", System.currentTimeMillis() / 1000);
	}
	
	public void writePost(String text) {
		posts.add(new Post(System.currentTimeMillis(), text));
		try {
			packNotes();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String get() {
		return deliverable_str;
	}
	
	public void packNotes() throws Exception {
		
		int i = 0;
		JSONArray arr = new JSONArray();
		for(Post aPost : posts) {
			JSONObject note = new JSONObject();
			note.put("time", aPost.time / 1000);
			note.put("text", aPost.text);
			arr.put(note);
		}
		postsJson.put("notes", arr);
		postsJson.put("ver", System.currentTimeMillis() / 1000);
		
		deliverable = new JSONObject();
		String dataStr = postsJson.toString();
		deliverable.put("data", dataStr);
		deliverable.put("sig", sign(dataStr));
		
		deliverable_str = deliverable.toString();
	}
	
	public JSONArray readNotes(String str) {
		JSONObject obj = new JSONObject(str);
		JSONObject notes = new JSONObject(obj.getString("data"));
		try {
			PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(notes.getString("pub"))));
			if(verify(obj.getString("data"), obj.getString("sig"), pub)) {
				return notes.getJSONArray("notes");
			}else {
				return new JSONArray();
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return new JSONArray();
	}
	
	public String sign(String str) throws Exception {
		Signature instance = Signature.getInstance("SHA256withRSA");
		instance.initSign(priv_key);
		instance.update(str.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(instance.sign());
	}
	
	public boolean verify(String msg, String signature, PublicKey publicKey) throws Exception {
		Signature instance = Signature.getInstance("SHA256withRSA");
		instance.initVerify(publicKey);
		instance.update(msg.getBytes(StandardCharsets.UTF_8));
		return instance.verify(Base64.getDecoder().decode(signature));
	}
}
