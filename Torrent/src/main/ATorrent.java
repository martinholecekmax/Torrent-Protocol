package main;

import static utils.Constants.TORRENT_ROOT_LOCATION;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import file.FileManager;

public class ATorrent {
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);

	public static void main(String[] args) {
		PropertyConfigurator.configure("properties/log4j.properties");
		LOGGER.info("Program Started ...");
		ATorrent aTorrent = new ATorrent();
		boolean create = true;
//		create = false;
		aTorrent.start(create);
	}

	public void start(boolean start) {
		try {
//			String filename = TORRENT_ROOT_LOCATION + "torrent test\\test.txt";
//			String torrentFileName = TORRENT_ROOT_LOCATION + "test.temp";
			
//			String filename = TORRENT_ROOT_LOCATION + "Alpha";
//			String torrentFileName = TORRENT_ROOT_LOCATION + "Alpha.temp";	

			String filename = System.getProperty("user.dir") + "/empty_20MB.txt";			
			String torrentFileName =  System.getProperty("user.dir") + "/empty_20MB.temp";
			String location = System.getProperty("user.dir") + "\\";
			
//			String location = TORRENT_ROOT_LOCATION;
			String storeLocation = TORRENT_ROOT_LOCATION + "test\\";

			// Load previous jobs from dat file
//			fileManager.loadJobs();

			// Start Server
			ServerSocket serverSocket = new ServerSocket(0);
			
			String localIP = "";
			try (final DatagramSocket socket = new DatagramSocket()) {
				socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
				localIP = socket.getLocalAddress().getHostAddress();
			} catch (UnknownHostException e) {
				localIP = InetAddress.getLocalHost().getHostAddress();				
			}
			
			Peer peer = new Peer(Optional.empty() ,serverSocket.getInetAddress().toString(),localIP, serverSocket.getLocalPort());

			// Initialize FileManager
			FileManager fileManager = new FileManager(peer);

			Server listener = new Server(serverSocket, fileManager);
			Thread thread = new Thread(listener, "Listener thread.");
			thread.start();

			if (start) {
				createTorrent(fileManager, filename, location);
			} else {
				loadTorrent(fileManager, torrentFileName, storeLocation);
			}

		} catch (IOException e) {
			LOGGER.fatal("Failed to create server socket", e);
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
			Job job = fileManager.createJob(torrentMetadata, storeLocation);
			DownloadManager downloadManager = new DownloadManager(job, fileManager);
			Thread downloadManagerThread = new Thread(downloadManager, "Download Manager Thread");
			downloadManagerThread.start();
			LOGGER.info("LOAD TORRENT METADATA Pieces: " + job.numPieces() + " " + torrentMetadata.getInfoHash());
		} catch (ClassNotFoundException e) {
			LOGGER.fatal("Data from loaded file are not a TorrentMetadata class type.", e);
		} catch (IOException e) {
			LOGGER.fatal("Loading torrent failed.", e);
		}
	}
}
