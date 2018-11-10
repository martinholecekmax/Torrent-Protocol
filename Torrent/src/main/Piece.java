package main;

import java.io.Serializable;

import utils.Utility;

public class Piece implements Comparable<Piece>, Serializable {
	private static final long serialVersionUID = 6510434884363403458L;
	private int index;
	private String hash;
	private int seen;
	private boolean stored;
	private boolean valid;
	private byte[] data;

	public Piece(int index, String hash) {
		this.index = index;
		this.hash = hash;
		this.setSeen(0);
		this.stored = false;
		this.valid = false;
		this.data = null;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public int getIndex() {
		return index;
	}

	public String getHash() {
		return hash;
	}

	public int getSeen() {
		return seen;
	}

	public void setSeen(int seen) {
		this.seen = seen;
	}

	public void addSeen() {
		this.seen++;
	}

	public boolean isStored() {
		return stored;
	}

	public void setStored(boolean stored) {
		this.stored = stored;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	/**
	 * Check if piece's data are valid.
	 * 
	 * @param piece
	 * @return True if the input piece's hash value matches to piece from metadata
	 *         file. Otherwise, return false.
	 */
	public boolean validate(Piece piece) {
		String hash = Utility.getHahSHA1(piece.getData());
		if (hash.equals(this.hash)) {
			this.valid = true;
		} else {
			this.valid = false;
		}
		return this.isValid();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
		result = prime * result + index;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Piece other = (Piece) obj;
		if (hash == null) {
			if (other.hash != null)
				return false;
		} else if (!hash.equals(other.hash))
			return false;
		if (index != other.index)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("<index %d hash %s>", this.getIndex(), this.getHash());
	}

	@Override
	public int compareTo(Piece o) {
		int compareIndex = ((Piece) o).getIndex();

		// Ascending order
		return this.index - compareIndex;

		// Descending order
//		return compareIndex - this.index;		
	}
}