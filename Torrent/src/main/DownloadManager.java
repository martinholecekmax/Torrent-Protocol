package main;

import static utils.Constants.DEFAULT_INTERVAL;
import static utils.Constants.MAX_CLIENT_THREADS;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;

import file.FileManager;
import tracker.TrackerClient;
import tracker.TrackerResponse;

public class DownloadManager implements Runnable{
	
	private ArrayList<Peer> peers;
	private ArrayList<Peer> connectedPeers;
	private Job job;
	private Peer client;
	private FileManager fileManager;
	private int interval;
	private ExecutorService executorService;
	
	public DownloadManager(Job job, FileManager fileManager) {
		this.job = job;
		this.client = fileManager.getPeer();
		this.fileManager = fileManager;
		this.interval = DEFAULT_INTERVAL;
		this.connectedPeers = new ArrayList<>();
		executorService = Executors.newFixedThreadPool(MAX_CLIENT_THREADS);		
	}
	
	@Override
	public void run() {
		while(!job.isDone()) {
			try {
				TrackerResponse response = TrackerClient.getResponse(job.getTorrentMetadata(), client, job.getStatus());
				peers = response.getPeers();
				interval = response.getInterval();
				createDownloadTasks(peers);
				Thread.sleep(interval);
			} catch (InterruptedException | IOException | JSONException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Job Finished "+ job.getTorrentMetadata().getInfo().getName());
	}	
	
	/**
	 * For each Peer create Download Task.
	 * 
	 * @param peers ArrayList of Peers
	 */
	public void createDownloadTasks(ArrayList<Peer> peers) {
		for (Peer peer : peers) {
			try {
				// Don't connect to same program, get public ip address
				if (peer.getPeerID().equals(client.getPeerID())) {
					client.setIpAddress(peer.getIpAddress());
					continue;
				}
				
				// Don't create new task if peer is already connected
				if (connectedPeers.contains(peer)) {
					continue;
				}
				
				// Check if is localhost
				Socket socket = null;
				if (peer.getIpAddress().equals(client.getIpAddress())) {
					socket = new Socket("localhost", peer.getPort());					
				} else {
					socket = new Socket(peer.getIpAddress(), peer.getPort());
				}
				
				DownloadTask task = new DownloadTask(socket, fileManager, connectedPeers, peer, job);

				// executorService.submit(task);
				
				Thread thread = new Thread(task, "Download task thread");
				thread.start();
				
				System.out.println("Peer " + "Connected: " + peer.getPeerID() + peer.getIpAddress() + ":" + peer.getPort());
			} catch (UnknownHostException e) {
				continue;
			} catch (IOException e) {
				continue;
			}
		}
	}
}
