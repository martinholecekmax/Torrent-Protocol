package main;

import static utils.Constants.DEFAULT_BANDWIDTH;
import static utils.Constants.PIECE_SIZE;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import file.FileHandler;
import utils.Utility;

public class Job implements Serializable {
	private static final long serialVersionUID = 3554312536086078516L;
	private ArrayList<Piece> pieces;
	private ArrayList<File> files;
	private TorrentMetadata torrentMetadata;
	private boolean done = false;
	private TorrentStatus status;
	private int bandwidth;

	transient private Object queueLock;
	transient private Object pieceListLock;
	private PiecesQueue piecesQueue;

	public Job(ArrayList<Piece> pieces, ArrayList<File> files, TorrentMetadata torrentMetadata) {
		this.pieces = pieces;
		this.files = files;
		this.queueLock = new Object();
		this.pieceListLock = new Object();
		this.torrentMetadata = torrentMetadata;
		this.status = new TorrentStatus();
		this.status.setLeft(torrentMetadata.getInfo().getLength());
		this.setBandwidth(DEFAULT_BANDWIDTH);
		this.piecesQueue = new PiecesQueue();
		piecesQueue.enqueAllPieces(pieces);
	}

	public void enquePiece(Piece piece) {
		synchronized (queueLock) {
			piecesQueue.enqueuePiece(piece);
		}
	}

	public Piece dequePiece() {
		synchronized (queueLock) {
			return piecesQueue.dequeuePiece();
		}
	}

	public boolean hasPiece() {
		synchronized (queueLock) {
			if (piecesQueue.hasPiece()) {
				return true;
			}
			return false;
		}
	}

	public int getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	public TorrentStatus getStatus() {
		return status;
	}

	public ArrayList<Piece> getPieces() {
		return pieces;
	}

	public ArrayList<File> getFiles() {
		return files;
	}

	public String getTorrentInfoHash() {
		return torrentMetadata.getInfoHash();
	}

	public TorrentMetadata getTorrentMetadata() {
		return torrentMetadata;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	/**
	 * Checks if file has all pieces.
	 * 
	 * @return True if file has all pieces, otherwise false.
	 */
	public boolean isJobDone() {
		synchronized (pieceListLock) {
			for (Piece piece : pieces) {
				if (piece.isStored() == false) {
					return false;
				}
			}
			this.done = true;
			this.status.setEvent(Event.COMPLETED);
			return true;
		}
	}

	/**
	 * Find piece that has been seen less times than others.
	 * 
	 * @return instance of Piece class.
	 */
	public Piece findLessSeenPiece() {
		Piece lessSeenPiece = null;
		for (Piece piece : pieces) {
			if (piece.isStored()) {
				continue;
			}
			if (lessSeenPiece == null) {
				lessSeenPiece = piece;
			}
			if (piece.getSeen() < lessSeenPiece.getSeen()) {
				lessSeenPiece = piece;
			}
		}
		if (lessSeenPiece != null) {
			pieces.get(pieces.indexOf(lessSeenPiece)).addSeen();
//			status.setUploaded(PIECE_SIZE); TODO
		} else {
			setDone(true);
		}
		return lessSeenPiece;
	}

	/**
	 * Validate piece data and if is valid store the piece.
	 * 
	 * @param piece - piece of data bytes from a file.
	 * @return True if piece has been stored successfully, otherwise, return false
	 */
	public boolean storePiece(Piece piece) {
		synchronized (pieceListLock) {
			for (Piece currentPiece : pieces) {
				if (piece.getIndex() == currentPiece.getIndex()) {
					if (currentPiece.isStored()) {
						return true;
					}
					if (currentPiece.validate(piece)) {
						if (storeValidPiece(piece)) {
							currentPiece.setStored(true);
							status.addDownloaded(PIECE_SIZE);
							status.decreaseLeft(PIECE_SIZE);
							return true;
						}
					}
					break;
				}
			}
			return false;
		}
	}

	/**
	 * Store piece that contains data byte array, to a file.
	 * 
	 * @param piece
	 * @return True if file has been stored, otherwise, return false.
	 */
	private boolean storeValidPiece(Piece piece) {
		try {
			FileHandler.storePiece(files, piece.getData(), piece.getIndex());
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public Piece getPieceWithoutData(int index) {
		for (Piece piece : pieces) {
			if (piece.getIndex() == index) {
				return piece;
			}
		}
		System.err.println("Error get piece " + index);
		return null;
	}

	/**
	 * Get piece of a file by piece index which is offset in the file.
	 * 
	 * @param index - offset bytes in the file
	 * @return instance of a Piece class if index valid and file can be read,
	 *         otherwise, return null instance.
	 */
	public Piece getPiece(int index) {
		try {
			if (pieces.get(index).isStored()) {
				byte[] data = new byte[PIECE_SIZE];
				data = FileHandler.getPiece(files, index);
				if (data != null) {
					Piece piece = new Piece(index, Utility.getHahSHA1(data));
					piece.setData(data);
					return piece;
				}
			}
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Get percentage of pieces that are completed. This number is rounded to two
	 * decimal places.
	 * 
	 * @return double percentage of pieces done.
	 */
	public double getPercentageDone() {
		int totalPieces = pieces.size();
		int piecesDone = calculatePiecesDone();
		return (double) Math.round((double) 100 / totalPieces * piecesDone * 100) / 100;
	}

	/**
	 * Get number of pieces done.
	 * 
	 * @return number of pieces that are completed.
	 */
	private int calculatePiecesDone() {
		int done = 0;
		for (Piece piece : pieces) {
			if (piece.isStored()) {
				done++;
			}
		}
		return done;
	}
}
