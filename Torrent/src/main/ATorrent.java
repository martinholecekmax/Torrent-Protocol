package main;

import static utils.Constants.TORRENT_ROOT_LOCATION;

import java.io.IOException;
import java.net.ServerSocket;

import file.FileManager;

public class ATorrent {

	public static void main(String[] args) {
		ATorrent aTorrent = new ATorrent();
//		aTorrent.start(true);			
		aTorrent.start(false);			
	}

	public void start(boolean start) {
		try {
			String filename = TORRENT_ROOT_LOCATION + "torrent test\\test.txt";
//			String filename = TORRENT_ROOT_LOCATION + "Alpha";
			String location = TORRENT_ROOT_LOCATION;
			String torrentFileName = TORRENT_ROOT_LOCATION + "test.temp";
//			String torrentFileName = TORRENT_ROOT_LOCATION + "Alpha.temp";
			String storeLocation = TORRENT_ROOT_LOCATION + "test3\\";

			// Load previous jobs from dat file
//			fileManager.loadJobs();

			// Start Server
			ServerSocket serverSocket = new ServerSocket(0);
			
			Peer peer = new Peer(serverSocket.getInetAddress().toString(), serverSocket.getLocalPort());
			
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
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 *  Create Metadata file.
	 *  
	 * @param fileManager
	 * @param filename
	 * @param location
	 * @throws IOException
	 */
	private void createTorrent(FileManager fileManager, String filename, String location) throws IOException {
		TorrentProcessor processor = new TorrentProcessor();
		processor.createMetadataFile(fileManager, filename, location, "Martin");
		System.out.println("CREATE TORRENT METADATA");
	}

	/**
	 *  Create DownloadManager - Load torrent Metadata.
	 *  
	 * @param fileManager
	 * @param torrentFileName
	 * @param storeLocation
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void loadTorrent(FileManager fileManager, String torrentFileName, String storeLocation)
			throws IOException, ClassNotFoundException {
		TorrentProcessor processor = new TorrentProcessor();
		TorrentMetadata torrentMetadata = processor.loadMetadataFile(torrentFileName);
		Job job = fileManager.createJob(torrentMetadata, storeLocation);
		DownloadManager downloadManager = new DownloadManager(job, fileManager);
		Thread downloadManagerThread = new Thread(downloadManager, "Download Manager Thread");
		downloadManagerThread.start();
		System.out.println("LOAD TORRENT METADATA");
	}
}
