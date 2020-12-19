package dev.cglab.InterPlanetaryMessaging;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Random;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.json.*;

//https://www.novixys.com/blog/how-to-generate-rsa-keys-java/

public abstract class IpmOtr {
	
	public class Keys{
		public Keys() {
			used_mac = new HashSet<String>();
		}
		public BigInteger my_key, others_key;
		public BigInteger my_key_id, others_key_id;
		public BigInteger s;
		public HashSet<String> used_mac;
	}
	
	String[] hash_prefix = {"c", "c_p", "m1", "m1_p", "m2", "m2_p"};
	
	boolean iamalice;
	
	BigInteger p = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
		"29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
		"EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
		"E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
		"EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
		"C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
		"83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
		"670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF", 16);
	BigInteger g = new BigInteger("2");
	
	MessageDigest digest;
	int state;
	Random rand;
	
	PrivateKey priv_key;
	PublicKey pub_key;
	PublicKey other_pub_key;
	
	Keys keys;
	
	BigInteger r;
	BigInteger x;
	BigInteger y;
	BigInteger g_x;
	BigInteger g_y;
	BigInteger s;
	BigInteger g_x_id;
	BigInteger g_y_id;
	
	byte[] c;
	byte[] c_p;
	byte[] m1;
	byte[] m1_p;
	byte[] m2;
	byte[] m2_p;
	
	JSONObject aes_r_g_x;
	String hash_g_x;
	
	JSONObject aes_c_x_b;
	String m_aes_c_x_b;
	
	JSONObject aes_c_x_a;
	String m_aes_c_x_a;
	
	boolean ready;
	
	public IpmOtr(PrivateKey priv, PublicKey pub) throws Exception {
		rand = new SecureRandom(String.valueOf(System.currentTimeMillis()).getBytes());
		digest = MessageDigest.getInstance("SHA-256");
		
		priv_key = priv;
		pub_key = pub;
		keys = new Keys();
		ready = false;
	}
	
	public abstract void tx(JSONObject obj);
	
	public abstract void rx(JSONObject obj);

	public JSONObject dispatcher(JSONObject obj) throws Exception {
		switch(obj.getString("stage")){
		case "bob_init":
			System.out.println("do step 2");
			alice_init(obj);
			break;
		case "alice_init":
			System.out.println("do step 3");
			bob_compute(obj);
			break;
		case "bob_compute":
			System.out.println("do step 4");
			alice_compute_verify(obj);
			break;
		case "alice_compute_verify":
			System.out.println("do step 5");
			bob_verify(obj);
			break;
		case "send":
			return receive(obj);
		default:
			break;
		}
		return null;
	}
	
	public void bob_init() throws Exception {

		JSONObject obj = new JSONObject();
		
		r = new BigInteger(256, rand);
		
		byte[] r_bytes = new byte[128 / 8];
		System.arraycopy(r.toByteArray(), 0, r_bytes, 0, r.toByteArray().length >= 16 ? 16 : r.toByteArray().length);
		
		x = new BigInteger(320, rand);
		
		g_x = g.modPow(x, p);
		
		obj.put("hash_g_x", Base64.getEncoder().encodeToString(digest.digest(g_x.toString().getBytes(StandardCharsets.UTF_8))));
		obj.put("aes_r_g_x", enc(r_bytes, (new JSONObject()).put("g_x", g_x.toString())));
		
		obj.put("stage", "bob_init");
		
		tx(obj);
	}
	
	public void alice_init(JSONObject input) {
		
		aes_r_g_x = input.getJSONObject("aes_r_g_x");
		hash_g_x = input.getString("hash_g_x");
		
		JSONObject obj = new JSONObject();
		
		y = new BigInteger(320, rand);
		
		g_y = g.modPow(y, p);
		
		obj.put("g_y", g_y.toString());
		
		obj.put("stage", "alice_init");
		
		tx(obj);
	}
	
	public void bob_compute(JSONObject input) throws Exception {
		
		g_y = new BigInteger(input.getString("g_y"));
		s = g_y.modPow(x, p);
		
		key_mac_gen();
		
		
		g_x_id = new BigInteger(64, rand);
		
		JSONObject obj_b = new JSONObject();
		obj_b.put("g_x", g_x.toString());
		obj_b.put("g_y", g_y.toString());
		obj_b.put("pub_b", Base64.getEncoder().encodeToString(pub_key.getEncoded()));
		obj_b.put("g_x_id", g_x_id.toString());
		
		String m_b = mac(m1, obj_b);
		
		JSONObject x_b = new JSONObject();
		x_b.put("pub_b", Base64.getEncoder().encodeToString(pub_key.getEncoded()));
		x_b.put("g_x_id", g_x_id.toString());
		x_b.put("sig_b", sign(m_b));
		
		JSONObject aes_c_x_b = enc(c, x_b);
		
		JSONObject out = new JSONObject();
		out.put("r", r.toString());
		out.put("aes_c_x_b", aes_c_x_b);
		out.put("m_aes_c_x_b", mac(m2, aes_c_x_b));
		
		out.put("stage", "bob_compute");
		
		tx(out);
	}
	
	public void alice_compute_verify(JSONObject input) throws Exception {
		BigInteger b_r = new BigInteger(input.getString("r"));
		aes_c_x_b = input.getJSONObject("aes_c_x_b");
		m_aes_c_x_b = input.getString("m_aes_c_x_b");
		
		byte[] r_bytes = new byte[128 / 8];
		System.arraycopy(b_r.toByteArray(), 0, r_bytes, 0, b_r.toByteArray().length >= 16 ? 16 : b_r.toByteArray().length);
		
		g_x = new BigInteger(dec(r_bytes, aes_r_g_x).getString("g_x"));
		
		if(! hash_g_x.equals(Base64.getEncoder().encodeToString(digest.digest(g_x.toString().getBytes(StandardCharsets.UTF_8))))) {
			throw new Exception("g_x hash failed");
		}
		
		s = g_x.modPow(y, p);
		key_mac_gen();
		
		if(! m_aes_c_x_b.equals(mac(m2, aes_c_x_b))){
			throw new Exception("aes_c_x_b mac failed");
		}
		
		JSONObject x_b = dec(c, aes_c_x_b);
		g_x_id = new BigInteger(x_b.getString("g_x_id"));
		
		JSONObject obj_b = new JSONObject();
		obj_b.put("g_x", g_x.toString());
		obj_b.put("g_y", g_y.toString());
		obj_b.put("pub_b", x_b.getString("pub_b"));
		obj_b.put("g_x_id", g_x_id.toString());
		
		other_pub_key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(x_b.getString("pub_b"))));
		
		String m_b = mac(m1, obj_b);
		
		if(! verify(m_b, x_b.getString("sig_b"), other_pub_key)){
			throw new Exception("m_b sig failed");
		}
		
		g_y_id = new BigInteger(64, rand);
		
		JSONObject obj_a = new JSONObject();
		obj_a.put("g_x", g_x.toString());
		obj_a.put("g_y", g_y.toString());
		obj_a.put("pub_a", Base64.getEncoder().encodeToString(pub_key.getEncoded()));
		obj_a.put("g_y_id", g_y_id.toString());
		
		String m_a = mac(m1_p, obj_a);
		
		JSONObject x_a = new JSONObject();
		x_a.put("pub_a", Base64.getEncoder().encodeToString(pub_key.getEncoded()));
		x_a.put("g_y_id", g_y_id.toString());
		x_a.put("sig_a", sign(m_a));
		
		JSONObject aes_c_x_a = enc(c, x_a);
		
		JSONObject out = new JSONObject();
		out.put("aes_c_x_a", aes_c_x_a);
		out.put("m_aes_c_x_a", mac(m2_p, aes_c_x_a));
		
		out.put("stage", "alice_compute_verify");
		
		keys.my_key = y;
		keys.my_key_id = g_y_id;
		keys.others_key = g_x;
		keys.others_key_id = g_x_id;
		
		ready = true;
		
		tx(out);
	}
	
	public void bob_verify(JSONObject input) throws Exception {

		aes_c_x_a = input.getJSONObject("aes_c_x_a");
		m_aes_c_x_a = input.getString("m_aes_c_x_a");
		
		if(! m_aes_c_x_a.equals(mac(m2_p, aes_c_x_a))){
			throw new Exception("aes_c_x_a mac failed");
		}
		
		JSONObject x_a = dec(c, aes_c_x_a);
		g_y_id = new BigInteger(x_a.getString("g_y_id"));
		
		JSONObject obj_a = new JSONObject();
		obj_a.put("g_x", g_x.toString());
		obj_a.put("g_y", g_y.toString());
		obj_a.put("pub_a", x_a.getString("pub_a"));
		obj_a.put("g_y_id", g_y_id.toString());
		
		other_pub_key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(x_a.getString("pub_a"))));
		
		String m_a = mac(m1_p, obj_a);
		
		if(! verify(m_a, x_a.getString("sig_a"), other_pub_key)){
			throw new Exception("m_a sig failed");
		}
		
		keys.my_key = x;
		keys.my_key_id = g_x_id;
		keys.others_key = g_y;
		keys.others_key_id = g_y_id;
		
		ready = true;
	}
	
	public void send(JSONObject message) throws Exception {
		BigInteger curr_s = keys.others_key.modPow(keys.my_key, p);
		byte[] aes_key = new byte[16];
		byte[] mac_key = new byte[16];
		System.arraycopy(digest.digest((curr_s.toString() + "I am the AES key").getBytes(StandardCharsets.UTF_8)), 0, aes_key, 0, 128 / 8);
		System.arraycopy(digest.digest((curr_s.toString() + "I am the MAC key").getBytes(StandardCharsets.UTF_8)), 0, mac_key, 0, 128 / 8);
		
		keys.my_key = new BigInteger(320, rand);
		
		JSONObject body = new JSONObject();
		body.put("my_key_id", keys.my_key_id.toString());
		body.put("others_key_id", keys.others_key_id.toString());
		body.put("next_dh", g.modPow(keys.my_key, p).toString());
		body.put("msg", enc(aes_key, message));
		
		JSONObject out = new JSONObject();
		out.put("body", body);
		out.put("mac", mac(mac_key, body));
		out.put("oldmackeys", new JSONArray(keys.used_mac));
		
		out.put("stage", "send");
		
		keys.used_mac.add(Base64.getEncoder().encodeToString(mac_key));
		keys.my_key_id = keys.my_key_id.add(new BigInteger("1"));
		
		tx(out);
	}
	
	public JSONObject receive(JSONObject message) throws Exception {
		JSONObject body = message.getJSONObject("body");
		
		if(! body.getString("my_key_id").equals(keys.others_key_id.toString()) ? body.getString("others_key_id").equals(keys.my_key_id.toString()) : false) {
			throw new Exception("key id test failed");
		}
		
		BigInteger curr_s = keys.others_key.modPow(keys.my_key, p);
		byte[] aes_key = new byte[16];
		byte[] mac_key = new byte[16];
		System.arraycopy(digest.digest((curr_s.toString() + "I am the AES key").getBytes(StandardCharsets.UTF_8)), 0, aes_key, 0, 128 / 8);
		System.arraycopy(digest.digest((curr_s.toString() + "I am the MAC key").getBytes(StandardCharsets.UTF_8)), 0, mac_key, 0, 128 / 8);
		
		if(! mac(mac_key, body).equals(message.getString("mac"))) {
			throw new Exception("message mac failed");
		}
		
		keys.others_key = new BigInteger(body.getString("next_dh"));
		keys.others_key_id = keys.others_key_id.add(new BigInteger("1"));
		
		return dec(aes_key, body.getJSONObject("msg"));
	}
	
	public JSONObject enc(byte[] key, JSONObject input) throws Exception {
		JSONObject obj = new JSONObject();
		
		Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
		SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
		
		BigInteger nonce = new BigInteger(96, rand);
		obj.put("nonce", nonce.toString());
		byte[] iv = new byte[128 / 8];
		System.arraycopy(nonce.toByteArray(), 0, iv, 0, nonce.toByteArray().length);
		
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
		
		byte[] plaintext = input.toString().getBytes(StandardCharsets.UTF_8);
		byte[] ciphertext = cipher.doFinal(plaintext);
		
		obj.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext));
		
		return obj;
	}
	
	public JSONObject dec(byte[] key, JSONObject input) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
		SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
		
		BigInteger nonce = new BigInteger(input.getString("nonce"));
		byte[] iv = new byte[128 / 8];
		System.arraycopy(nonce.toByteArray(), 0, iv, 0, nonce.toByteArray().length);
		
		cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
		return new JSONObject(new String(cipher.doFinal(Base64.getDecoder().decode(input.getString("ciphertext"))), StandardCharsets.UTF_8));
	}
	
	public void key_mac_gen() {
		c = new byte[16];
		c_p = new byte[16];
		m1 = new byte[16];
		m1_p = new byte[16];
		m2 = new byte[16];
		m2_p = new byte[16];
		System.arraycopy(digest.digest((s.toString() + hash_prefix[0]).getBytes(StandardCharsets.UTF_8)), 0, c, 0, 128 / 8);
		System.arraycopy(digest.digest((s.toString() + hash_prefix[1]).getBytes(StandardCharsets.UTF_8)), 0, c_p, 0, 128 / 8);
		System.arraycopy(digest.digest((s.toString() + hash_prefix[2]).getBytes(StandardCharsets.UTF_8)), 0, m1, 0, 128 / 8);
		System.arraycopy(digest.digest((s.toString() + hash_prefix[3]).getBytes(StandardCharsets.UTF_8)), 0, m1_p, 0, 128 / 8);
		System.arraycopy(digest.digest((s.toString() + hash_prefix[4]).getBytes(StandardCharsets.UTF_8)), 0, m2, 0, 128 / 8);
		System.arraycopy(digest.digest((s.toString() + hash_prefix[5]).getBytes(StandardCharsets.UTF_8)), 0, m2_p, 0, 128 / 8);
	}
	
	public String mac(byte[] mac_key, JSONObject input) {
		byte[] json = input.toString().getBytes();
		byte[] to_be_hash = Arrays.copyOf(json, json.length + mac_key.length);
		System.arraycopy(mac_key, 0, to_be_hash, json.length, mac_key.length);
		return Base64.getEncoder().encodeToString(digest.digest(to_be_hash));
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
	
	public boolean isReady() {
		return ready;
	}
	
}
