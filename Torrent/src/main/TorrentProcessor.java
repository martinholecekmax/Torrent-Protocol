package main;

import static utils.Constants.ANNOUNCE;
import static utils.Constants.PIECE_SIZE;
import static utils.Constants.TORRENT_EXTENSION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import file.FileHandler;
import file.FileManager;

public class TorrentProcessor {

	/**
	 * Create Torrent info field from the list of files.
	 * 
	 * @param fileHandler
	 * @param files
	 * @return
	 * @throws IOException
	 */
	public TorrentFileInfo createTorrentFileInfo(FileHandler fileHandler, ArrayList<File> files) throws IOException {
		List<TorrentFile> torrentFiles = new ArrayList<>();
		long length = 0;

		String filePath = "";
		for (File file : files) {
			filePath = fileHandler.stripFilePaths(file, fileHandler.getRoot());
			ArrayList<String> disectedPath = fileHandler.getDirectoryHierarchy(filePath);
			TorrentFile torrentFile = new TorrentFile(disectedPath, file.length());
			torrentFiles.add(torrentFile);
			length += file.length();
		}

		List<Piece> pieces = fileHandler.getAllPieces(files);
		TorrentFileInfo info = new TorrentFileInfo(torrentFiles, fileHandler.getRootFilename(), PIECE_SIZE, pieces,
				length);
		return info;
	}

	/**
	 * Create new Metadata file.
	 * 
	 * @param fileManager
	 * @param filename
	 * @param location
	 * @param createdBy
	 * @return
	 * @throws IOException
	 */
	public boolean createMetadataFile(FileManager fileManager, String filename, String location, String createdBy)
			throws IOException {
		FileHandler fileHandler = new FileHandler();
		ArrayList<File> files = fileHandler.getFiles(filename);
		TorrentFileInfo info = createTorrentFileInfo(fileHandler, files);
		Date createdAt = new Date(System.currentTimeMillis());
		TorrentMetadata torrentMetadata = new TorrentMetadata(ANNOUNCE, info, createdBy, createdAt);

		FileOutputStream fileOutputStream = new FileOutputStream(location + info.getName() + TORRENT_EXTENSION);
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
		objectOutputStream.writeObject(torrentMetadata);
		objectOutputStream.close();
		fileOutputStream.close();
		
		// TODO Cleanup this method
		
		Job job = new Job(info.getPieces(), files, torrentMetadata);
		job.setDone(true);
		for (Piece piece : job.getPieces()) {
			piece.setStored(true);
		}
		fileManager.add(job);
		return true;
	}

	/**
	 * Load a Metadata File from the disk by using ObjectInputStream.
	 * 
	 * @param filename - Location of a Metadata file on the disk.
	 * @return a Metadata file loaded from the disk.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public TorrentMetadata loadMetadataFile(String filename) throws IOException, ClassNotFoundException {
		FileInputStream fileInputStream = new FileInputStream(filename);
		ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
		TorrentMetadata torrentMetadata = (TorrentMetadata) objectInputStream.readObject();
		objectInputStream.close();
		fileInputStream.close();
		return torrentMetadata;
	}
}
