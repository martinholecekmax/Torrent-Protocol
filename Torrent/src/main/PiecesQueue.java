package main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class PiecesQueue implements Serializable{
	private static final long serialVersionUID = 6206081680138041080L;

	private Queue<Piece> pieceQueue;

	transient private Object writeQueueLock = new Object();

	public PiecesQueue() {
		pieceQueue = new LinkedList<>();
	}
	
	public void enqueAllPieces(ArrayList<Piece> pieces) {
		for (Piece piece : pieces) {
			enqueuePiece(piece);
		}
	}
	
	public boolean hasPiece() {
		synchronized (writeQueueLock) {
			if (pieceQueue.isEmpty() == false) {
				return true;
			}
			return false;
		}
	}
	
	public void enqueuePiece(Piece message) {
		synchronized (writeQueueLock) {
			pieceQueue.add(message);
		}
	}
	
	public Piece dequeuePiece() {
		synchronized (writeQueueLock) {
			return pieceQueue.poll();
		}
	}
}
