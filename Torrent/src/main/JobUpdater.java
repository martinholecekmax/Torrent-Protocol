package main;

import static utils.Constants.DEFAULT_INTERVAL;

import file.FileManager;

public class JobUpdater implements Runnable {

	private FileManager fileManager;
	private boolean running;

	public JobUpdater(FileManager fileManager) {
		this.fileManager = fileManager;
		this.running = true;
	}

	@Override
	public void run() {
		while (running) {
			updateJobs();
			try {
				Thread.sleep(DEFAULT_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void updateJobs() {
		if (fileManager.size() > 0) {
			fileManager.contactTracker();			
		}
	}
}
