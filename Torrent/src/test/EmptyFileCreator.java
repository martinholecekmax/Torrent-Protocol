package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class EmptyFileCreator {

	private static final int TWENTY_MB = 20971520;
	private static final int HUNDRED_MB = 104857600;
	private static final int ONE_MB = 1048576;
	private static final String EMPTY_ONE_MB = "empty_1MB.txt";
	private static final String EMPTY_TWENTY_MB = "empty_20MB.txt";
	private static final String EMPTY_HUNDRED_MB = "empty_100MB.txt";
	
	public static void main(String[] args) {
		EmptyFileCreator emptyFileCreator = new EmptyFileCreator();
		emptyFileCreator.createDummyFile(EMPTY_ONE_MB, ONE_MB);
		emptyFileCreator.createDummyFile(EMPTY_TWENTY_MB, TWENTY_MB);
		emptyFileCreator.createDummyFile(EMPTY_HUNDRED_MB, HUNDRED_MB);
	}

	private void createDummyFile(String filename, int size) {
		File fileWriter = new File(filename);		
		try (BufferedWriter writer = Files.newBufferedWriter(fileWriter.toPath())) {
            while (fileWriter.length() < size) {
                writer.write("But most of all, Chris is my hero! ");
                writer.flush();
            }
            System.out.println("100 MB Data is written to the file.!");
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
