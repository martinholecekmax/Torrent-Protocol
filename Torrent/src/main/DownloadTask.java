package main;

import static utils.Constants.PIECE_SIZE;

import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;

import file.FileManager;
import utils.Utility;

public class DownloadTask implements Runnable {
	private FileManager fileManager;
	private ArrayList<Peer> connectedPeers;
	private Peer peer;
	private Writer writer;
	private Reader reader;
	private ConnectionState state;
	private Job job;

	public DownloadTask(Socket socket, FileManager fileManager, ArrayList<Peer> connectedPeers, Peer peer, Job job) {
		state = new ConnectionState(socket);
		this.fileManager = fileManager;
		this.connectedPeers = connectedPeers;
		this.peer = peer;
		this.reader = new Reader(state);
		this.writer = new Writer(state);
		this.job = job;
	}

	@Override
	public void run() {
		connectedPeers.add(peer);

		Thread readerThread = new Thread(reader, "Client Reader Thread");
		Thread writerThread = new Thread(writer, "Client Writer Thread");
		readerThread.start();
		writerThread.start();

		while (state.isAlive()) {
			processRead();
			processWrite();
//			try {
//				Thread.sleep(job.getBandwidth());
//			} catch (InterruptedException e) {
//				state.setKill(true);
//			}
		}

		try {
			readerThread.join();
			writerThread.join();
		} catch (InterruptedException e) {
			System.out.println("Error, Thread joining interrupted!");
		}
		connectedPeers.remove(peer);
		System.out.println("Process Terminated ...");
	}

	/**
	 * Process response from Peer.
	 */
	public void processRead() {
		if (state.hasRead()) {
			String message = state.dequeueRead();

			// HAVEPIECE FILE_ID_HASH PIECE_ID <PIECE DATA>
			if (message.startsWith("HAVEPIECE")) {
				String[] messageSplit = message.split(" ");
				String infoHash = messageSplit[1].trim();
				int index = Integer.parseInt(messageSplit[2].trim());
				String dataUTF = messageSplit[3];

				byte[] data = new byte[PIECE_SIZE];
				data = Base64.decodeBase64(dataUTF.getBytes());
				Piece piece = new Piece(index, Utility.getHahSHA1(data));
				piece.setData(data);

				boolean stored = fileManager.storePiece(infoHash, piece);
				if (stored) {
					System.out.println("Piece stored successfully");
				} else {
					job.enquePiece(piece);
					System.out.println("Piece not stored");
				}

//				CSVFileHandler.writeTime("Reieved", index);
			} else if (message.startsWith("NOPIECE")) {
				// NOTIFY FILE MANAGER THAT PEER DOESN'T HAVE PIECE
				String[] messageSplit = message.split(" ");
				String infoHash = messageSplit[1].trim();
				int index = Integer.parseInt(messageSplit[2].trim());
				job.enquePiece(job.getPiece(index));
				System.out.println(message);
			} else if (message.startsWith("DISCONNECT")) {
				System.out.println(message);
				state.setKill(true);
			} else if (message.startsWith("DISCONNECT")) {
				System.out.println("CLIENT RECEIVES DISCONNECT");
//				state.setKill(true);
			} else {
				System.out.println("SYNTAX ERROR " + message);
			}
		}
	}

	/**
	 * Ask Peer for Piece.
	 */
	boolean run = true;

	public void processWrite() {
		// ASK FILE MANAGER IF NEEDS PIECE
		if (job.hasPiece()) {
			state.enqueueWrite("PIECEEXISTS " + job.getTorrentInfoHash() + " " + job.dequePiece().getIndex());
		} else if (run) {
			System.out.println("CLIENT SENDS DISCONNECT");
			state.enqueueWrite("DISCONNECT");
//			run = false;
			state.setKill(true);
		}
//		if (!job.isDone()) {
//			Piece piece = job.findLessSeenPiece();
//			if (piece != null) {				
//				state.enqueueWrite("PIECEEXISTS " + job.getTorrentInfoHash() + " " + piece.getIndex());
//			}
//		} else if(run) {
//			System.out.println("CLIENT SENDS DISCONNECT");
//			state.enqueueWrite("DISCONNECT");
//			run = false;
////			state.setKill(true);
//		}		
//		state.enqueueWrite("KEEPALIVE");
	}
}
