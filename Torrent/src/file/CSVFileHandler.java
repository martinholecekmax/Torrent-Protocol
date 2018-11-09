package file;

import java.io.FileWriter;
import java.io.IOException;

public class CSVFileHandler {
	
	private static String file = "statistics.csv";

	public static void writeTime(String type, int index){
		try {
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.append(type);
			fileWriter.append(",");
			fileWriter.append(index + "");
			fileWriter.append(",");
			fileWriter.append(System.currentTimeMillis() + "");
			fileWriter.append("/n");
			fileWriter.close();
		} catch (IOException e) {
//			e.printStackTrace();
			System.out.println("ERROR");
		}
	}
}
