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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import file.FileManager;

public class ATorrent {
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private FileManager fileManager;
	private ServerSocket serverSocket;
	private Server listener;
	private ExecutorService downloadExecutorService = null;

	public void start() {
		try {
			PropertyConfigurator.configure("properties/log4j.properties");

			LOGGER.info("Program Started ...");

			// Load previous jobs from dat file
//			fileManager.loadJobs();

			serverSocket = new ServerSocket(0);

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

	private String getLocalIP() throws UnknownHostException {
		String localIP = "";
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 0);
			localIP = socket.getLocalAddress().getHostAddress();
			localIP = InetAddress.getLocalHost().getHostAddress();
		} catch (SocketException e) {
			localIP = InetAddress.getLocalHost().getHostAddress();
			LOGGER.fatal("Get private IP address failed.", e);
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
	public void createTorrent(String filename, String location) {
		try {
			TorrentProcessor processor = new TorrentProcessor();
			Job job = processor.createMetadataFile(fileManager, filename, location, "Martin");
			fileManager.contactTracker();
			LOGGER.info("CREATE TORRENT METADATA Pieces: " + job.numPieces() + " " + job.getTorrentInfoHash());
		} catch (IOException e) {
			LOGGER.fatal("Creating torrent failed.", e);
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
			downloadExecutorService.shutdownNow();
		}
	}
}
