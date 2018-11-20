package main;

import static utils.Constants.DEFAULT_INTERVAL;
import static utils.Constants.MAX_CLIENT_THREADS;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import file.CSVFileHandler;
import file.FileManager;
import tracker.TrackerClientSSL;
import tracker.TrackerResponse;

public class DownloadManager implements Callable<Boolean> {

	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private ArrayList<Peer> peers;
	private ArrayList<Peer> connectedPeers;
	private Job job;
	private Peer client;
	private FileManager fileManager;
	private int interval;
	private ExecutorService executorService;
	private boolean started = false;
	private long startTime;

	public DownloadManager(Job job, FileManager fileManager) {
		this.job = job;
		this.client = fileManager.getPeer();
		this.fileManager = fileManager;
		this.interval = DEFAULT_INTERVAL;
		this.connectedPeers = new ArrayList<>();
		executorService = Executors.newFixedThreadPool(MAX_CLIENT_THREADS);
	}

	public Boolean call() {
		Thread.currentThread().setName("Download Manager");
		
		while (!job.isDone()) {
			try {
				Optional<TrackerResponse> response = TrackerClientSSL.getResponse(job.getTorrentMetadata(), client,
						job.getStatus());
				if (response.isPresent()) {
					peers = response.get().getPeers();
//					interval = response.get().getInterval();
					interval = 2000;
					if (setPeerPublicIP(peers)) {
						createDownloadTasks(peers);
					}
				}
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				LOGGER.warn("Thread sleep has been interrupted.", e);
			}
		}
		
		// Measure time of download
		long endTime = System.currentTimeMillis();
		String testId = UUID.randomUUID().toString();
		CSVFileHandler.writeTime((endTime - startTime), testId);
		
		fileManager.contactTracker();
		executorService.shutdown();
		LOGGER.info("Job Finished " + job.getTorrentMetadata().getInfo().getName());
		return true;
	}

	/**
	 * For each Peer create Download Task.
	 * 
	 * @param peers ArrayList of Peers
	 */
	public void createDownloadTasks(ArrayList<Peer> peers) {
		for (Peer peer : peers) {
			try {
				if (job.isDone()) {
					break;
				}

				if (peer.getPeerID().equals(client.getPeerID())) {
					continue;
				}

				// Don't create new task if peer is already connected
				if (connectedPeers.contains(peer)) {
					continue;
				}

				// Check if is localhost
				Socket socket = null;
				if (peer.getIpAddress().equals(client.getIpAddress())) {
					socket = new Socket(peer.getLocalIP(), peer.getPort());
				} else {
					socket = new Socket(peer.getIpAddress(), peer.getPort());
				}
				if (socket.isConnected()) {
					if (!started) {
						startTime = System.currentTimeMillis();
						started = true;
					}					
					
					DownloadTask task = new DownloadTask(socket, fileManager, connectedPeers, peer, job);
					executorService.submit(task);
				}

			} catch (UnknownHostException e) {
				LOGGER.debug("Couldn't connect to a peer. Port: " + peer.getPort() + " IP: " + peer.getIpAddress());
				continue;
			} catch (IOException e) {
				LOGGER.debug("Couldn't create client socket. Port: " + peer.getPort() + " IP: " + peer.getIpAddress());
				continue;
			}
		}
	}

	private boolean setPeerPublicIP(ArrayList<Peer> peers) {
		for (Peer peer : peers) {
			// Don't connect to same program, get public ip address
			if (peer.getPeerID().equals(client.getPeerID())) {
				client.setIpAddress(peer.getIpAddress());
				return true;
			}
		}
		return false;
	}
}
