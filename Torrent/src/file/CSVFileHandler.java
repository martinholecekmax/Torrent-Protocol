package file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import main.ATorrent;

public class CSVFileHandler {

	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);

	public static void writeTime(String type, int index) {

		try {
			File file = new File("statistics.csv");
			FileWriter fileWriter;
			if (file.exists()) {
				fileWriter = new FileWriter(file, true);// if file exists append to file. Works fine.
			} else {
				file.createNewFile();
				fileWriter = new FileWriter(file);
			}
			fileWriter.append(type);
			fileWriter.append(",");
			fileWriter.append(index + "");
			fileWriter.append(",");
			fileWriter.append(System.currentTimeMillis() + " ms");
			fileWriter.append(",");
			fileWriter.append(System.currentTimeMillis() + "");
			fileWriter.append(",");
			fileWriter.append("\n");
			fileWriter.close();
		} catch (IOException e) {
			LOGGER.error("Writing into file failed.", e);
		}
	}

	public static void writeTime(String type) {
		try {
			File file = new File("statistics.csv");
			FileWriter fileWriter;
			if (file.exists()) {
				fileWriter = new FileWriter(file, true);// if file exists append to file. Works fine.
			} else {
				file.createNewFile();
				fileWriter = new FileWriter(file);
			}
			fileWriter.append(type);
			fileWriter.append(",");
			fileWriter.append(System.currentTimeMillis() + " ms");
			fileWriter.append(",");
			fileWriter.append(System.currentTimeMillis() + "");
			fileWriter.append(",");
			fileWriter.append("\n");
			fileWriter.close();
		} catch (IOException e) {
			LOGGER.error("Writing into file failed.", e);
		}
	}
}
