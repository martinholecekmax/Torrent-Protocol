package test;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import main.ATorrent;
import tracker.TrackerClientSSL;

public class TrialTest {
	
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}	

	public void runTrials(int numberTrials) {				
		for (int i = 0; i < numberTrials; i++) {			
			ATorrent aTorrent = new ATorrent();
//			aTorrent.torrentProcess(Process.CREATE);
			ATorrent aTorrent2 = new ATorrent();
			Optional<Future<Boolean>> downloadJob =  aTorrent.loadTorrent("", "");	
			if (downloadJob.isPresent()) {
				try {
					downloadJob.get().get();
				} catch (InterruptedException e) {
					LOGGER.error("Download process has been interrupted.");					
				} catch (ExecutionException e) {
					LOGGER.error("Test " + i + " Terminated.");					
				}
			}
			try {
				aTorrent.close();
				aTorrent2.close();
				System.gc();
				TrackerClientSSL.deleteAllPeers();
				LOGGER.info("Test " + i + " Terminated.");
				Thread.sleep(100);
			} catch (InterruptedException e) {
				LOGGER.error("Terminated Error. " + e.getMessage());
			}
		}		
	}

}
