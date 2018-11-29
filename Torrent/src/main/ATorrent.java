package main;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import file.FileManager;

public class ATorrent {
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private FileManager fileManager;
	private ServerSocket serverSocket;
	private Server listener = null;
	private ExecutorService downloadExecutorService = null;
	private boolean closed = false;

	public void start() {
		try {
			PropertyConfigurator.configure("properties/log4j.properties");

			LOGGER.info("Program Started ...");

			// Load previous jobs from dat file
//			fileManager.loadJobs();

			getServerSocket();

			String localIP = getLocalIP();

			Peer peer = new Peer(Optional.empty(), serverSocket.getInetAddress().toString(), localIP,
					serverSocket.getLocalPort());

			// Initialize FileManager
			fileManager = new FileManager(peer);

			listener = new Server(serverSocket, fileManager);
			Thread thread = new Thread(listener, "Listener thread.");
			thread.start();

			downloadExecutorService = Executors.newCachedThreadPool();

		} catch (IOException e) {
			LOGGER.fatal("Failed to create server socket", e);
			close();
		}
	}

	private void getServerSocket() {
		// Automatically chosing a port
//		serverSocket = new ServerSocket(0);
//		return;
		
		// Try port 50,000 and if this port is not available increment port number and try again
		boolean done = true;
		int port = 50000;
		while (done) {
			try {
				serverSocket = new ServerSocket(port);
				done = false;
			} catch (IOException e) {
				System.out.println("port:" + port);
				port++;
			}
		}
	}

	private String getLocalIP() throws UnknownHostException, SocketException {
		String localIP = "";
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 0);
			localIP = socket.getLocalAddress().getHostAddress();
			localIP = InetAddress.getLocalHost().getHostAddress();
			LOGGER.info("IP: " + localIP);
			LOGGER.info("Port: " + serverSocket.getLocalPort());
		}
		return localIP;
	}

	/**
	 * Create Metadata file.
	 * 
	 * @param fileManager
	 * @param filename
	 * @param location
	 * @throws IOException
	 */
	public boolean createTorrent(String filename, String location) {
		if (closed) {
			LOGGER.fatal("ATorrent class already closed.");
			return false;
		}
		try {
			TorrentProcessor processor = new TorrentProcessor();
			Job job = processor.createMetadataFile(fileManager, filename, location, "Martin");
			fileManager.contactTracker();
			LOGGER.info("CREATE TORRENT METADATA Pieces: " + job.numPieces() + " " + job.getTorrentInfoHash());
			return true;
		} catch (IOException e) {
			LOGGER.fatal("Creating torrent failed.", e);
			return false;
		}
	}

	/**
	 * Create DownloadManager - Load torrent Metadata.
	 * 
	 * @param fileManager
	 * @param torrentFileName
	 * @param storeLocation
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public Optional<Future<Boolean>> loadTorrent(String torrentFileName, String storeLocation) {
		if (closed) {
			LOGGER.fatal("ATorrent class already closed.");
			return Optional.empty();
		}
		try {
			TorrentProcessor processor = new TorrentProcessor();
			TorrentMetadata torrentMetadata;
			torrentMetadata = processor.loadMetadataFile(torrentFileName);
			Optional<Job> job = fileManager.createJob(torrentMetadata, storeLocation);
			if (job.isPresent()) {
				DownloadManager downloadManager = new DownloadManager(job.get(), fileManager);
				Future<Boolean> downloadJob = downloadExecutorService.submit(downloadManager);
				LOGGER.info(
						"LOAD TORRENT METADATA Pieces: " + job.get().numPieces() + " " + torrentMetadata.getInfoHash());
				return Optional.of(downloadJob);
			} else {
				LOGGER.fatal("Job creation failed.");
			}
		} catch (ClassNotFoundException e) {
			LOGGER.fatal("Data from loaded file are not a TorrentMetadata class type.", e);
		} catch (IOException e) {
			LOGGER.fatal("Loading torrent failed.", e);
		}
		return Optional.empty();
	}

	public void close() {
		if (listener != null) {
			listener.close();
		}
		if (downloadExecutorService != null) {
			closeDownloadExecutor();
		}
		closed = true;
	}

	private void closeDownloadExecutor() {
		try {
			downloadExecutorService.shutdown();
			downloadExecutorService.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Download Executor Termination interrupted!", e);
		} finally {
			if (!downloadExecutorService.isTerminated()) {
				LOGGER.error("Download Executor Cancel non-finished tasks!");
			}
			downloadExecutorService.shutdownNow();
		}
	}
}
