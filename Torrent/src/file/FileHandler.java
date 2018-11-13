package file;

import static utils.Constants.PIECE_SIZE;
import static utils.Constants.TORRENT_ROOT_LOCATION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;

import main.Piece;
import main.TorrentFile;
import main.TorrentFileInfo;
import main.TorrentMetadata;
import utils.Utility;

public class FileHandler {
	public File root;

	/**
	 * Get Root Directory
	 * 
	 * @return File directory
	 */
	public File getRoot() {
		return root;
	}

	/**
	 * If root is file then substring extension, otherwise leave it.
	 * 
	 * @return String of root file
	 */
	public String getRootFilename() {
		return FilenameUtils.getBaseName(root.getName());
	}

	/**
	 * Create file structure for TorrentMetadata at whereToStore location.
	 * 
	 * @param torrentMetadata - Torrent File that contains inforamtion about the files.
	 * @param whereToStore - Where on the disk will be files stored.
	 * @return ArrayList of files that has been created on the disk.
	 * @throws IOException
	 */
	public ArrayList<File> createFileStructure(TorrentMetadata torrentMetadata, String whereToStore)
			throws IOException {
		String rootDir = torrentMetadata.getInfo().getName();
		Path root = Paths.get(whereToStore, rootDir);
		String fullRoot = findAvailableDirectory(root.toString());
		return createFilesFromTorrentInfo(torrentMetadata.getInfo(), fullRoot);
	}
	
	/**
	 * Create file structure from Torrent Info at the location spacified by root variable.
	 * 
	 * @param info contains list of all files that will be created.
	 * @param root directory where files will be stored
	 * @return list of newly created files
	 * @throws IOException
	 */
	private ArrayList<File> createFilesFromTorrentInfo(TorrentFileInfo info, String root) throws IOException {
		ArrayList<File> files = new ArrayList<>();
		for (TorrentFile file: info.getFiles()) {			
			String filePath = getFullPath(file.getPath(), root);			
			Path path = Paths.get(filePath);
			Files.createDirectories(path.getParent());			
			preallocateFile(filePath, file.getLength());
			File filePlaceholder = new File(filePath);
			files.add(filePlaceholder);
		}
		return files;
	}
	
	/**
	 * If directory already exists create get new one suffixed with number.
	 * 
	 * @param directory - location of the directory
	 * @return new location of directory if exist otherwise return input directory
	 * @throws FileExistsException if the directory name reached maximal value of Integer
	 */
	public String findAvailableDirectory(String directory) throws FileExistsException {		
		Path path = Paths.get(String.format("%s", directory));
		if(!Files.exists(path)) {
			return path.toString();
		}
		
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			path =  Paths.get(String.format("%s (%d)", directory, i));
			if(!Files.exists(path)) {
				return path.toString();
			}
		}
		throw new FileExistsException("Could not create directory, all posible names already exists");
	}
			
	/**
	 * This method combines list of string into file path.
	 * 
	 * @param files - list of file paths
	 * @param root - root directory
	 * 
	 * @return String filename
	 */
	public String getFullPath(List<String> files, String root) {		
		StringJoiner joiner = new StringJoiner(File.separator);	
		for (String file : files) {
			joiner.add(file);
		}
		Path path = Paths.get(root, joiner.toString());
		return path.toString();
	}
		
	/**
	 * Return list of Pieces of all files. Each piece contains 
	 * index (offset in files where to read from) and hash of the 
	 * files' piece that has been taken from the offset.
	 * 
	 * @param files - list of files
	 * @return List of Pieces where each piece contains index and hash
	 * @throws IOException
	 */
	public List<Piece> getAllPieces(ArrayList<File> files) throws IOException {
		List<Piece> pieces = new ArrayList<>();
		byte[] read = new byte[PIECE_SIZE];
		int numberOfPieces = calculateNumPieces(getTotalLength(files), PIECE_SIZE);
		for (int index = 0; index <= numberOfPieces; index++) {
			read = getPiece(files, index);
			String hash = Utility.getHahSHA1(read);
			Piece piece = new Piece(index, hash);
			pieces.add(piece);
		}
		return pieces;
	}

	/**
	 * Get bytes from the files or file depends on size of piece and files.
	 * 
	 * @param listFiles - ArrayList of files from which bytes will be read
	 * @param index     - offset of the bytes to read
	 * @return byte array that contains bytes from files at the offset
	 * @throws IOException if the given file does not denote an existing, writable
	 *                     regular file and a new regular file of that name cannot
	 *                     be created, or if some other error occurs while opening
	 *                     or creating the file
	 */
	public static byte[] getPiece(ArrayList<File> listFiles, int index) throws IOException {
		byte[] read = new byte[PIECE_SIZE];
		int startingPosition = index * PIECE_SIZE;
		int remainder = 0;
		int numberRead = 0;
		int totalBytesRead = 0;

		for (File file : listFiles) {
			// File bigger then starting position goto next file
			if (file.length() <= startingPosition) {
				startingPosition -= file.length();
				continue;
			}
			// some bytes are from this file, and remaining from next
			if (file.length() - startingPosition < PIECE_SIZE - remainder) {
				// Get bytes from starting position until length
				byte[] pieceFromFirstFile = new byte[(int) (file.length() - startingPosition)];
				numberRead = readChunk(file.getAbsolutePath(), pieceFromFirstFile, startingPosition);
				totalBytesRead += numberRead;
				int start = 0;
				for (int i = remainder; i < read.length; i++) {
					if (start == numberRead || numberRead == -1)
						break;
					read[i] = pieceFromFirstFile[start++];
				}
				// add remainder
				remainder += file.length() - startingPosition;
				startingPosition = 0;
				continue;
			} else {
				byte[] pieceFromFirstFile = new byte[PIECE_SIZE];
				numberRead = readChunk(file.getAbsolutePath(), pieceFromFirstFile, startingPosition);
				totalBytesRead += numberRead;
				int start = 0;
				for (int i = remainder; i < read.length; i++) {
					if (start == numberRead)
						break;
					read[i] = pieceFromFirstFile[start++];
				}
				break;
			}
		}
		// Last piece, not full size of piece
		if (totalBytesRead < PIECE_SIZE) {
			byte[] bytesRead = new byte[totalBytesRead];
			for (int i = 0; i < bytesRead.length; i++) {
				bytesRead[i] = read[i];
			}
			return bytesRead;
		}
		return read;
	}

	/**
	 * Store bytes into the file or files at the offset denoted by (PIECE_SIZE *
	 * index).
	 * 
	 * @param listFiles  - ArrayList of files at which bytes will be written into
	 * @param piece      - bytes to write
	 * @param pieceIndex - offset of the files
	 * @throws IOException if the given string does not denote an existing, writable
	 *                     regular file and a new regular file of that name cannot
	 *                     be created, or if some other error occurs while opening
	 *                     or creating the file
	 */
	public static void storePiece(ArrayList<File> listFiles, byte[] piece, int pieceIndex) throws IOException {
		int startingPosition = pieceIndex * PIECE_SIZE;
		int remainder = 0;
		for (File file : listFiles) {
			// File bigger then starting position goto next file
			if (file.length() < startingPosition) {
				startingPosition -= file.length();
				continue;
			}
			// some bytes are from this file, and remaining from next
			if (file.length() - startingPosition < PIECE_SIZE - remainder) {
				// Get bytes from starting position until length
				byte[] pieceFromFirstFile = new byte[(int) (file.length() - startingPosition)];
				for (int i = 0; i < pieceFromFirstFile.length; i++) {
					pieceFromFirstFile[i] = piece[i + remainder];
				}
				writeToFile(file.getAbsolutePath(), pieceFromFirstFile, startingPosition, pieceFromFirstFile.length);
				remainder += pieceFromFirstFile.length;
				startingPosition = 0;
				continue;
			} else {
				// Write remaining bytes
				byte[] pieceFromFirstFile = new byte[piece.length - remainder];
				for (int i = 0; i < pieceFromFirstFile.length; i++) {
					pieceFromFirstFile[i] = piece[i + remainder];
				}
				writeToFile(file.getAbsolutePath(), pieceFromFirstFile, startingPosition, pieceFromFirstFile.length);
				break;
			}
		}
	}

	/**
	 * Get list of filenames striped of absolute path, for example, if we have path
	 * "C:\\Users\\Maxim\\Desktop\\torrent\\week1\\" where torrent root is folder
	 * called "torrent" then this method will take of this
	 * "C:\\Users\\Maxim\\Desktop\\" path and will return only "torrent\\week1\\..."
	 * paths.
	 * 
	 * @param files         - List of files from which method will get filenames
	 * @param rootDirectory - torrent root directory which will be striped of the
	 *                      filenames
	 * @return ArrayList of String filenames
	 */
	public ArrayList<String> stripFilePaths(ArrayList<File> files, File rootDirectory) {
		ArrayList<String> filenames = new ArrayList<>();
		for (File file : files) {
			filenames.add(rootDirectory.toURI().relativize(file.toURI()).getPath());
		}
		return filenames;
	}

	/**
	 * Get list of filenames striped of absolute path, for example, if we have path
	 * "C:\\Users\\Maxim\\Desktop\\torrent\\week1\\" where torrent root is folder
	 * called "torrent" then this method will take of this
	 * "C:\\Users\\Maxim\\Desktop\\" path and will return only "torrent\\week1\\..."
	 * paths.
	 * 
	 * @param files         - List of files from which method will get filenames
	 * @param rootDirectory - torrent root directory which will be striped of the
	 *                      filenames
	 * @return ArrayList of String filenames
	 */
	public String stripFilePaths(File file, File rootDirectory) {
		if (file.equals(rootDirectory)) {
			File root = rootDirectory.getParentFile();
			return (root.toURI().relativize(file.toURI()).getPath());
		}
		return (rootDirectory.toURI().relativize(file.toURI()).getPath());
	}

	/**
	 * This method will parse String of file path and return ArrayList of folders up
	 * to top-most (torrent root) directory. The last element in the list is the
	 * name of the file, and the elements preceding it indicate the directory
	 * hierarchy in which this file is situated.
	 * 
	 * @param filePath - Path in string format
	 * @return a list of string elements that specify the path of the file, relative
	 *         to the topmost directory.
	 */
	public ArrayList<String> getDirectoryHierarchy(String filePath) {
		ArrayList<String> pathList = new ArrayList<>();
		Path path = Paths.get(filePath);
		for (Path subpath : path) {
			pathList.add(subpath.toString());
		}
		return pathList;
	}

	/**
	 * Get list of files from specified filename, if filename is just a single file
	 * return list with only one element containing that file, otherwise if the
	 * filename is directory, then get all files that are located inside that
	 * directory.
	 * 
	 * @param filename - torrent root file or directory from which will torrent
	 *                 create metadata file
	 * @return List of files that are in the folder specified by filename
	 * @throws FileExistsException is raised if file does not exits
	 */
	public ArrayList<File> getFiles(String filename) throws FileExistsException {
		File file = new File(filename);
		root = file;

		ArrayList<File> listFiles = new ArrayList<>();

		if (file.exists()) {
			if (file.isDirectory()) {
				Collection<File> files = FileUtils.listFiles(file, null, true);
				for (File file2 : files) {
					listFiles.add(file2);
				}
			} else if (file.isFile()) {
				listFiles.add(file);
			}
		} else {
			throw new FileExistsException();
		}
		return listFiles;
	}

	/**
	 * Concatenate torrent root location (where the files will be stored) with
	 * filename. This will be useful for saving torrent into default path.
	 * 
	 * @param filename
	 * @return
	 */
	public String getAbsoluteFilename(String filename) {
		Path path = Paths.get(TORRENT_ROOT_LOCATION, filename);
		return path.toString();
	}

	/**
	 * Get total length in bytes of all files from the list.
	 * 
	 * @param files - List of files
	 * @return The length in bytes of all files added together
	 */
	public long getTotalLength(ArrayList<File> files) {
		long length = 0;
		for (File f : files) {
			length += f.length();
		}
		return length;
	}

	/**
	 * Calculate number of pieces that will be file or files divided into. This
	 * method uses ceiling function which will round up division.
	 * 
	 * @param length    - the length of a file or files in bytes
	 * @param pieceSize - the length of chunk in which will be file divided into
	 * @return length divided by piece size and rounded up to get number of pieces
	 */
	public int calculateNumPieces(long length, int pieceSize) {
		return (int) Math.floor((double) length / pieceSize);
	}

	/**
	 * Create an Empty file with specified size. This is used for allocation of
	 * memory for large files.
	 * 
	 * @param filename - must be a file and not directory
	 * @param size     - size of a new file
	 * @throws IOException if the given string does not denote an existing, writable
	 *                     regular file and a new regular file of that name cannot
	 *                     be created, or if some other error occurs while opening
	 *                     or creating the file
	 */
	public void preallocateFile(String filename, long size) throws IOException {
		RandomAccessFile randomAccessFile = new RandomAccessFile(filename, "rw");
		randomAccessFile.setLength(size);
		randomAccessFile.close();
	}

	/**
	 * Write data of length size into the file at the offset position. If length is
	 * less then zero, do nothing.
	 * 
	 * @param filename - must be a file and not directory
	 * @param input    - data to write
	 * @param position - offset position of where to write in file
	 * @param length   - number of byte to write, must be positive number
	 * @throws IOException if the given string does not denote an existing, writable
	 *                     regular file and a new regular file of that name cannot
	 *                     be created, or if some other error occurs while opening
	 *                     or creating the file
	 * 
	 */
	public static void writeToFile(String filename, byte[] input, int position, int length) throws IOException {
		if (length > 0) {
			RandomAccessFile randomAccessFile = new RandomAccessFile(filename, "rw");
			randomAccessFile.seek(position);
			randomAccessFile.write(input, 0, length);
			randomAccessFile.close();
		}
	}

	/**
	 * Read chunk from file at the offset
	 * 
	 * @param filename
	 * @param chunk
	 * @param offset   - a position measured in bytes from the beginning of the file
	 * @return number of bytes that has been read, empty bytes of chunk length or -1
	 *         if EOF (end of file) if NullInputStream is not available
	 * @throws IOException if the given string does not denote an existing, writable
	 *                     regular file and a new regular file of that name cannot
	 *                     be created, or if some other error occurs while opening
	 *                     or creating the file
	 */
	public static int readChunk(String filename, byte[] chunk, long offset) throws IOException {
		RandomAccessFile randomAccessFile = new RandomAccessFile(filename, "r");
		int numBytes = 0;
		if (offset < randomAccessFile.length()) {
			randomAccessFile.seek(offset);
			if (chunk.length + offset > randomAccessFile.length()) {
				numBytes = randomAccessFile.read(chunk, 0, (int) (randomAccessFile.length() - offset));
				randomAccessFile.close();
				return numBytes;
			} else {
				numBytes = randomAccessFile.read(chunk, 0, chunk.length);
				randomAccessFile.close();
				return numBytes;
			}
		} else {
			NullInputStream nullInputStream = new NullInputStream(chunk.length);
			if (nullInputStream.available() > 0) {
				numBytes = nullInputStream.read(chunk, 0, chunk.length);
			} else {
				numBytes = IOUtils.EOF;
			}
			nullInputStream.close();
			return numBytes;
		}
	}

	/**
	 * Create temporary file that will be deleted before the program terminates.
	 * This file will be located inside systems Temp folder.
	 * 
	 * @param filename - the prefix of the file that will be created
	 * @param length
	 * @return newly created temporary file
	 * @throws IOException
	 */
	public File createTempFile(String filename, long length) throws IOException {
		File tempFile = new File(filename);
		tempFile.deleteOnExit();
		return tempFile;
	}

	@Deprecated
	/**
	 * Read piece from file by the piece index.
	 * 
	 * @param file
	 * @param piece
	 * @param index
	 * @return
	 * @throws IOException
	 */
	public int getPiece(File file, byte[] piece, long index) throws IOException {
		return readChunk(file.getAbsolutePath(), piece, index * piece.length);
	}

	@Deprecated
	/**
	 * Read bytes from file at the offset position.
	 * 
	 * @param filename
	 * @param position
	 * @param size
	 * @return
	 * @throws IOException
	 */
	public byte[] readFromFile(String filename, int position, int size) throws IOException {
		RandomAccessFile randomAccessFile = new RandomAccessFile(filename, "r");
		randomAccessFile.seek(position);
		byte[] read = new byte[size];
		randomAccessFile.read(read);
		randomAccessFile.close();
		return read;
	}

	@Deprecated
	/**
	 * Concatenate list of file into single file.
	 * 
	 * @param filename
	 * @param files
	 * @return
	 * @throws IOException
	 */
	public File concatenateFiles(String filename, ArrayList<File> files) throws IOException {
		File concatFile = new File(filename);
		FileOutputStream fileOutputStream = new FileOutputStream(concatFile);
		for (File file : files) {
			FileInputStream fileInputStream = new FileInputStream(file);
			// fileOutputStream.write(fileInputStream.readAllBytes()); // Not supported in JDK 1.8
			fileInputStream.close();
		}
		fileOutputStream.close();
		return concatFile;
	}
}
