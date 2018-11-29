package main;

import static utils.Constants.CONTACT_TRACKER_TIMER;

import org.apache.log4j.Logger;

import file.FileManager;

public class JobUpdater implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private FileManager fileManager;
	private boolean running;

	public JobUpdater(FileManager fileManager) {
		this.fileManager = fileManager;
		this.running = true;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("Job Updater");
		while (running) {
			updateJobs();			
			try {
				Thread.sleep(CONTACT_TRACKER_TIMER);
			} catch (InterruptedException e) {
				if (running) {
					LOGGER.error("Job Updater Thread sleep has been interrupted.");
				} else {
					LOGGER.info("Job Updater Thread has been closed.");
				}				
				break;
			}
		}
	}

	private void updateJobs() {
		if (fileManager.size() > 0) {
			fileManager.contactTracker();			
		}
	}
	
	public void close() {
		running = false;
	}
}
