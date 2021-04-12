package dev.cglab.InterPlanetaryMessaging;
import java.security.SecureRandom;
import java.util.Base64;
import java.security.MessageDigest;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class IpmKey {
	public final byte[] key;
	
	public IpmKey() {
		byte[] randomBytes = new byte[IpmConfig.KEY_LENGTH];
		(new SecureRandom(String.valueOf(System.currentTimeMillis()).getBytes())).nextBytes(randomBytes);
		key = randomBytes;
	}
	
	public IpmKey(String str) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
		key = new byte[IpmConfig.KEY_LENGTH];
		System.arraycopy(hash, 0, key, 0, IpmConfig.KEY_LENGTH);
	}
	
	public IpmKey(byte[] b) throws Exception {
		if(b.length != IpmConfig.KEY_LENGTH) {
			System.out.println(b.length);
			throw new Exception("Key length not match");
		}
		key = b;
	}
	
	public String toString() {
		String str = "";
		for(byte b : key) {
			str += String.format("%8s", Integer.toBinaryString(b & 0xff)).replace(" ", "0");
		}
		BigInteger bitint = (new BigInteger(1, key));
		System.out.println(str);
		System.out.println((new BigInteger(key)).xor(bitint));
		return str;
	}
	
	public BigInteger distance(IpmKey another) {
		BigInteger k = (new BigInteger(1, key)).xor(new BigInteger(1, another.key));
		return k;
	}
	
	public int distanceBits(IpmKey another) {
		return distance(another).bitLength();
	}
	
	public boolean isEmpty() {
		for(byte b : key) {
			if(b != 0) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return (new BigInteger(1, key)).hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
        if (!(o instanceof IpmKey)) {
            return false;
        }
        IpmKey another = (IpmKey)o;
        return (new BigInteger(1, key)).hashCode() == (new BigInteger(1, another.key)).hashCode();
	}
	public String base64() {
		return Base64.getEncoder().encodeToString(key);
	}
}
