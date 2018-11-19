package test;

import static utils.Constants.PROJECT_ROOT_DIRECTORY;
import static utils.Constants.TORRENT_ROOT_LOCATION;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import main.ATorrent;

class ClientTest {
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private String storeLocation;
	private String torrentFileName;

	public static void main(String[] args) {
		ClientTest clientTest = new ClientTest();
		clientTest.test();
	}
	
	void test() {
//		torrentFileName = PROJECT_ROOT_DIRECTORY + "/empty_1MB.temp";
//		torrentFileName = PROJECT_ROOT_DIRECTORY + "/empty_20MB.temp";
		torrentFileName = PROJECT_ROOT_DIRECTORY + "/empty_100MB.temp";
		storeLocation = TORRENT_ROOT_LOCATION + "test\\";		
				
		LOGGER.info("Test Started ...");
		for (int i = 0; i < 20; i++) {
			runClient();
			LOGGER.info("Test done: " + i);
		}
		LOGGER.info("Trial FINISHED");
	}
	
	public void runClient() {		
		ATorrent aTorrent = new ATorrent();
		aTorrent.start();
		Optional<Future<Boolean>> downloadJob = aTorrent.loadTorrent(torrentFileName, storeLocation);	
		if (downloadJob.isPresent()) {
			try {
				downloadJob.get().get();
			} catch (InterruptedException e) {
				LOGGER.error("Download process has been interrupted.");					
			} catch (ExecutionException e) {
				LOGGER.error("Test Terminated.");					
			}
		}
		try {
			aTorrent.close();
			System.gc();
		} catch (IOException e) {
			LOGGER.error("Terminated Error. " + e.getMessage());
		}
	}
}
