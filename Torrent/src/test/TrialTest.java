package test;

import static utils.Constants.PROJECT_ROOT_DIRECTORY;
import static utils.Constants.TORRENT_ROOT_LOCATION;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import main.ATorrent;
import tracker.TrackerClientSSL;

public class TrialTest {
	
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);	
	private String storeLocation;
	private String torrentFileName;
	private String location;
	private String filename;
	
	public static void main(String[] args) {
		TrackerClientSSL.deleteAllPeers();
		TrialTest trialTest = new TrialTest();
		trialTest.runTrials(50);
	}	

	public void runTrials(int numberTests) {				
		// torrentFileName = PROJECT_ROOT_DIRECTORY + "/empty_1MB.temp";
		// filename = PROJECT_ROOT_DIRECTORY + "/empty_1MB.txt";
		
		torrentFileName = PROJECT_ROOT_DIRECTORY + "/empty_20MB.temp";
		filename = PROJECT_ROOT_DIRECTORY + "/empty_20MB.txt";
		
		// torrentFileName = PROJECT_ROOT_DIRECTORY + "/empty_100MB.temp";
		// filename = PROJECT_ROOT_DIRECTORY + "/empty_100MB.txt";
		
		storeLocation = TORRENT_ROOT_LOCATION + "test\\";			
		location = PROJECT_ROOT_DIRECTORY + "\\";
		
		for (int i = 0; i < numberTests; i++) {			
			ATorrent aTorrent = new ATorrent();
			aTorrent.start();
			aTorrent.createTorrent(filename, location);
			ATorrent aTorrent2 = new ATorrent();
			aTorrent2.start();
			Optional<Future<Boolean>> downloadJob =  aTorrent2.loadTorrent(torrentFileName, storeLocation);	
			if (downloadJob.isPresent()) {
				try {
					downloadJob.get().get();
				} catch (InterruptedException e) {
					LOGGER.error("Download process has been interrupted.");					
				} catch (ExecutionException e) {
					LOGGER.error("Test " + i + " Terminated.");					
				}
			}
			aTorrent.close();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				LOGGER.error("Terminated Error. " + e.getMessage());
			}
			aTorrent2.close();
			System.gc();
			LOGGER.info("Test " + i + " Terminated.");
			TrackerClientSSL.deleteAllPeers();
		}	
		LOGGER.info("Trial Finished ...");
	}
}
