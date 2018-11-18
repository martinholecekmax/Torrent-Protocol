package main;

import static utils.Constants.TORRENT_ROOT_LOCATION;

import java.io.Closeable;
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

import file.FileManager;

enum Process {
	LOAD, CREATE
}

public class ATorrent implements Closeable{
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private String filename;
	private String torrentFileName;
	private String location;
	private String storeLocation;
	private FileManager fileManager;
	private ServerSocket serverSocket;
	private Server listener;
	private ExecutorService downloadExecutorService = null;
	private boolean isInitialize = false;
	
	public ATorrent() {
		initialize();
	}
	
	public void initialize() {
		try {
			LOGGER.info("Program Started ...");

			filename = System.getProperty("user.dir") + "/empty_20MB.txt";
			torrentFileName = System.getProperty("user.dir") + "/empty_20MB.temp";
			location = System.getProperty("user.dir") + "\\";
			storeLocation = TORRENT_ROOT_LOCATION + "test\\";

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
			isInitialize = true;
			
		} catch (IOException e) {
			LOGGER.fatal("Failed to create server socket", e);
			isInitialize = false;
		}
	}

	private String getLocalIP() throws SocketException, UnknownHostException {
		String localIP = "";
		try (final DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 0);
			localIP = socket.getLocalAddress().getHostAddress();
		} catch (UnknownHostException e) {
			localIP = InetAddress.getLocalHost().getHostAddress();
		}
		return localIP;
	}

	public Optional<Future<Boolean>> torrentProcess(Process start) {
		if (!isInitialize) {
			return Optional.empty();
		}
		switch (start) {
		case CREATE:
			createTorrent();
			return Optional.empty();
		case LOAD:
			return loadTorrent();
		default:
			createTorrent();
			return Optional.empty();
		}
	}

	/**
	 * Create Metadata file.
	 * 
	 * @param fileManager
	 * @param filename
	 * @param location
	 * @throws IOException
	 */
	private void createTorrent() {
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
	private Optional<Future<Boolean>> loadTorrent() {
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

	@Override
	public void close() throws IOException {
		if (listener != null) {
			listener.close();			
		}
		if (downloadExecutorService != null) {
			downloadExecutorService.shutdownNow();
		}
	}
}
