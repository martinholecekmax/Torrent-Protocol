package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class EmptyFileCreator {

	private static final int TWENTY_MB = 20971520;
	public static void main(String[] args) {
		File fileWriter = new File("empty_20MB.txt");
		
		try (BufferedWriter writer = Files.newBufferedWriter(fileWriter.toPath())) {
            while (fileWriter.length() < TWENTY_MB) {
                writer.write("This is just a test !");
                writer.flush();
            }
            System.out.println("1 KB Data is written to the file.!");
        } catch (IOException e) {
            e.printStackTrace();
        }

	}

}
