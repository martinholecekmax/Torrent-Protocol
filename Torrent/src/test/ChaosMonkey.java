package test;

import java.util.Arrays;
import java.util.Random;

/**
 * 
 * @author Maxim
 *
 */
public class ChaosMonkey {
	
	/**
	 * This method randomly change piece data to test reliability of a code.
	 * 
	 * @param data
	 * @param changePercentage
	 * @return
	 */
	public static boolean randomlyChangePiece(byte[] data, int changePercentage) {
		if (changePercentage == 0) {
			return false;
		}
		Random random = new Random();	
		float decimal = (float) changePercentage / 100;
		if (Math.random() < decimal) {
			int length = data.length;		
			int changeLocation = random.nextInt(length);				
			data[changeLocation] = (byte) random.nextInt(256);	
			return true;
		}
		return false;
	}	
	
	/**
	 * Just for testing of the functionality of this class
	 * @param args
	 */
	public static void main(String[] args) {		
		int counter = 0;
		for (int i = 0; i < 100; i++) {
			byte[] data = new byte[] { (byte) 0, (byte) 1, (byte) 2, (byte) 3};
			boolean check = ChaosMonkey.randomlyChangePiece(data, 1);
			System.out.println(Arrays.toString(data));
			if (check) {
				counter++;
			}
		}
		System.out.println(counter);
	}
}
