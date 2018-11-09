package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utility {
	public static String getHahSHA256(byte[] input) {
		String output = null;
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			sha.update(input);
			byte[] hash = sha.digest();
			output = String.format("%032X", new BigInteger(1, hash));
		} catch (NoSuchAlgorithmException e) {
			System.out.println("This Algorithm is not available.");
			return null;
		}
		return output;
	}
	
	public static String getHahSHA1(byte[] input) {
		String output = null;
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			sha.update(input);
			byte[] hash = sha.digest();
			output = String.format("%032X", new BigInteger(1, hash));
		} catch (NoSuchAlgorithmException e) {
			System.out.println("This Algorithm is not available.");
			return null;
		}
		return output;
	}
	
	/**
	 * Generate checksum from seriliazable input.
	 * This method uses MD5 algorithm to create hash.
	 * 
	 * @param input
	 * @return String representation of message digest of MD5 algorithm which is 35 characters long.
	 */
	public static String generateChecksum(Serializable input){
		byte[] thedigest = null;
		ObjectOutputStream objectOutputStream = null;
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(input);
			MessageDigest md = MessageDigest.getInstance("MD5");
			thedigest = md.digest(byteArrayOutputStream.toByteArray());
		} catch (Exception e) {
			System.out.println("Error in Object Output Stream.");
			return null;
		} finally {
			if (objectOutputStream != null) {
				try {
					objectOutputStream.close();
				} catch (IOException e) {
					System.out.println("Error in closing the Object Output Stream.");
					return null;
				}				
			}
		}
		return String.format("%032X", new BigInteger(1, thedigest));
	}
}
