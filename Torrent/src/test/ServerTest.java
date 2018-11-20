package test;

import static utils.Constants.PROJECT_ROOT_DIRECTORY;

import org.apache.log4j.Logger;

import main.ATorrent;

class ServerTest {
	
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private String location;
	private String filename;
	
	public static void main(String[] args) {
		ServerTest serverTest = new ServerTest();
		serverTest.test();
	}
	
	void test() {		
//		filename = PROJECT_ROOT_DIRECTORY + "/empty_1MB.txt";
		filename = PROJECT_ROOT_DIRECTORY + "/empty_20MB.txt";
//		filename = PROJECT_ROOT_DIRECTORY + "/empty_100MB.txt";
		location = PROJECT_ROOT_DIRECTORY + "\\";
		
		LOGGER.info("Server Started");
		runServer();
	}
	
	public void runServer() {			
		ATorrent aTorrent = new ATorrent();
		aTorrent.start();
		aTorrent.createTorrent(filename, location);
	}
}
