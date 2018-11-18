package main;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import tracker.TrackerClientSSL;

public class TorrentMain {
	
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
		
	public static void main(String[] args) {
		PropertyConfigurator.configure("properties/log4j.properties");
		TorrentMain main = new TorrentMain();
//		main.runTrials(1);
		for (int i = 0; i < 20; i++) {
			main.runClient();
			LOGGER.info("Test done: " + i);
		}
		LOGGER.info("Trial FINISHED");
//		main.runServer();
	}
	
	public void runServer() {		
		@SuppressWarnings("resource")
		ATorrent aTorrent = new ATorrent();
		aTorrent.torrentProcess(Process.CREATE);		
	}
	
	public void runClient() {		
		ATorrent aTorrent = new ATorrent();
		Optional<Future<Boolean>> downloadJob = aTorrent.torrentProcess(Process.LOAD);	
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
	
	public void runTrials(int numberTrials) {				
		for (int i = 0; i < numberTrials; i++) {			
			ATorrent aTorrent = new ATorrent();
			aTorrent.torrentProcess(Process.CREATE);
			ATorrent aTorrent2 = new ATorrent();
			Optional<Future<Boolean>> downloadJob = aTorrent2.torrentProcess(Process.LOAD);	
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
			} catch (IOException | InterruptedException e) {
				LOGGER.error("Terminated Error. " + e.getMessage());
			}
		}		
	}
}
