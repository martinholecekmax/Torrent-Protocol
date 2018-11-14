package main;

import static utils.Constants.TORRENT_ROOT_LOCATION;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import file.FileManager;

enum Process {
	LOAD, CREATE
}

public class ATorrent {
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private String filename;
	private String torrentFileName;
	private String location;
	private String storeLocation;
	private FileManager fileManager;

	public static void main(String[] args) {
		PropertyConfigurator.configure("properties/log4j.properties");
		LOGGER.info("Program Started ...");
		ATorrent aTorrent = new ATorrent();
		aTorrent.torrentProcess(Process.LOAD);
//		aTorrent.torrentProcess(Process.CREATE);
	}

	public ATorrent() {
		initialize();
	}

	public void initialize() {
		try {
			filename = System.getProperty("user.dir") + "/empty_20MB.txt";
			torrentFileName = System.getProperty("user.dir") + "/empty_20MB.temp";
			location = System.getProperty("user.dir") + "\\";
			storeLocation = TORRENT_ROOT_LOCATION + "test\\";

			// Load previous jobs from dat file
//			fileManager.loadJobs();

			// Start Server
			ServerSocket serverSocket = new ServerSocket(0);

			String localIP = getLocalIP();

			Peer peer = new Peer(Optional.empty(), serverSocket.getInetAddress().toString(), localIP,
					serverSocket.getLocalPort());

			fileManager = new FileManager(peer);

			Server listener = new Server(serverSocket, fileManager);
			Thread thread = new Thread(listener, "Listener thread.");
			thread.start();

		} catch (IOException e) {
			LOGGER.fatal("Failed to create server socket", e);
		}
	}

	private String getLocalIP() throws SocketException, UnknownHostException {
		String localIP = "";
		try (final DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			localIP = socket.getLocalAddress().getHostAddress();
		} catch (UnknownHostException e) {
			localIP = InetAddress.getLocalHost().getHostAddress();
		}
		return localIP;
	}

	private void torrentProcess(Process start) {
		switch (start) {
		case CREATE:
			createTorrent(fileManager, filename, location);
			break;
		case LOAD:
			loadTorrent(fileManager, torrentFileName, storeLocation);
			break;
		default:
			createTorrent(fileManager, filename, location);
			break;
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
	private void createTorrent(FileManager fileManager, String filename, String location) {
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
	private void loadTorrent(FileManager fileManager, String torrentFileName, String storeLocation) {
		try {
			TorrentProcessor processor = new TorrentProcessor();
			TorrentMetadata torrentMetadata;
			torrentMetadata = processor.loadMetadataFile(torrentFileName);
			Optional<Job> job = fileManager.createJob(torrentMetadata, storeLocation);
			if (job.isPresent()) {
				DownloadManager downloadManager = new DownloadManager(job.get(), fileManager);
				Thread downloadManagerThread = new Thread(downloadManager, "Download Manager Thread");
				downloadManagerThread.start();
				LOGGER.info(
						"LOAD TORRENT METADATA Pieces: " + job.get().numPieces() + " " + torrentMetadata.getInfoHash());
			} else {
				LOGGER.fatal("Job creation failed.");
			}
		} catch (ClassNotFoundException e) {
			LOGGER.fatal("Data from loaded file are not a TorrentMetadata class type.", e);
		} catch (IOException e) {
			LOGGER.fatal("Loading torrent failed.", e);
		}
	}
}
