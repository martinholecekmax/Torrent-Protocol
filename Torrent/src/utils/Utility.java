package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

import main.ATorrent;

public class Utility {
	
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	
	/**
	 * Generate SHA256 hash from input bytes.
	 *
	 * @param input - byte array
	 * @return - String representation of message digest of the input.
	 */
	public static String getHahSHA256(byte[] input) {
		String output = null;
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			sha.update(input);
			byte[] hash = sha.digest();
			output = String.format("%032X", new BigInteger(1, hash));
		} catch (NoSuchAlgorithmException e) {
			LOGGER.fatal("The Algorithm is not available.", e);
			return null;
		}
		return output;
	}
	
	/**
	 * Generate SHA1 hash from input bytes.
	 *
	 * @param input - byte array
	 * @return - String representation of message digest of the input.
	 */
	public static String getHahSHA1(byte[] input) {
		String output = null;
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			sha.update(input);
			byte[] hash = sha.digest();
			output = String.format("%032X", new BigInteger(1, hash));
		} catch (NoSuchAlgorithmException e) {
			LOGGER.fatal("The Algorithm is not available.", e);
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
			LOGGER.fatal("Error in generating check sum.", e);
			return null;
		} finally {
			if (objectOutputStream != null) {
				try {
					objectOutputStream.close();
				} catch (IOException e) {
					LOGGER.fatal("Error in closing the Object Output Stream.", e);
					return null;
				}				
			}
		}
		return String.format("%032X", new BigInteger(1, thedigest));
	}
}
