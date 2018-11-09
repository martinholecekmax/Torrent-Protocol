package main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the list of all files in torrent file
 * 
 * @author Maxim
 *
 */
public class TorrentFileInfo implements Serializable {
	private static final long serialVersionUID = 1076148465238176492L;
	private ArrayList<TorrentFile> files;
	private String name;
	private int pieceLength;
	private ArrayList<Piece> pieces;
	private long length;

	/**
	 * Constructor.
	 * 
	 * @param files
	 * @param name
	 * @param pieceLength
	 * @param pieces
	 * @param length
	 */
	public TorrentFileInfo(List<TorrentFile> files, String name, int pieceLength, List<Piece> pieces, long length) {
		this.files = new ArrayList<TorrentFile>(files);
		this.pieces = new ArrayList<Piece>(pieces);
		this.name = name;
		this.pieceLength = pieceLength;
		this.length = length;
	}

	public ArrayList<TorrentFile> getFiles() {
		return files;
	}

	public String getName() {
		return name;
	}

	public int getPieceLength() {
		return pieceLength;
	}

	public ArrayList<Piece> getPieces() {
		return pieces;
	}

	public Piece getSinglePiece(int index) {
		if (index < pieces.size() && index >= 0) {
			return pieces.get(index);
		}
		return null;
	}

	public long getLength() {
		return length;
	}

	@Override
	public String toString() {
		return "\n  files: {" + files + "\n  }\n  name=" + name + "\n  pieceLength=" + pieceLength + "\n  pieces="
				+ pieces + "\n  length=" + length;
	}
}
