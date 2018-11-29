package main;

import static utils.Constants.DISCONNECT_WAIT;
import static utils.Constants.PIECE_SIZE;
import static utils.Constants.TIMER_TO_SET_PIECE_QUEUE;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import file.FileManager;
import utils.Utility;

public class DownloadTask implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private FileManager fileManager;
	private ArrayList<Peer> connectedPeers;
	private Peer peer;
	private Writer writer;
	private Reader reader;
	private ConnectionState state;
	private Job job;
	private PiecesQueue piecesQueue;
	private long timeStart;

	public DownloadTask(Socket socket, FileManager fileManager, ArrayList<Peer> connectedPeers, Peer peer, Job job) {
		state = new ConnectionState(socket);
		this.fileManager = fileManager;
		this.connectedPeers = connectedPeers;
		this.peer = peer;
		this.reader = new Reader(state, "Client Reader");
		this.writer = new Writer(state, "Client Writer");
		this.job = job;
		this.piecesQueue = new PiecesQueue();
	}

	@Override
	public void run() {
		connectedPeers.add(peer);
		LOGGER.info("Client -> Peer Connected: " + peer.getIpAddress() + ":" + peer.getPort() + " " + peer.getPeerID());
		Thread.currentThread().setName("Client");
		Thread readerThread = new Thread(reader, "Client Reader Thread");
		Thread writerThread = new Thread(writer, "Client Writer Thread");
		readerThread.start();
		writerThread.start();

		timeStart = System.currentTimeMillis() - TIMER_TO_SET_PIECE_QUEUE;
		setPieceQueue();

		while (state.isAlive()) {
			try {
				processRead();
				processWrite();
				Thread.sleep(job.getBandwidth());
			} catch (InterruptedException e) {
				LOGGER.error("Download Task Thread sleep has been interrupted.");
				state.setKill(true);
			}
		}

		try {
			LOGGER.info("Client, Reader and Writer Threads are finishing before closing.");
			readerThread.join();
			writerThread.join();
			state.terminate();
		} catch (InterruptedException e) {
			LOGGER.error("Closing threads has been interrupted.", e);
		} catch (IOException e) {
			LOGGER.error("Closing socket failed.", e);
		}
		LOGGER.info("Download Task Process Terminated ...");
		connectedPeers.remove(peer);
	}

	/**
	 * Process response from Peer.
	 * 
	 * @throws InterruptedException
	 */
	public void processRead() throws InterruptedException {
		if (state.hasRead()) {
			String message = state.dequeueRead();
			if (message.startsWith("HAVEPIECE")) {
				String[] messageSplit = message.split(" ");
				String infoHash = messageSplit[1].trim();
				int index = Integer.parseInt(messageSplit[2].trim());
				String dataUTF = messageSplit[3];

				byte[] data = new byte[PIECE_SIZE];
				data = Base64.decodeBase64(dataUTF.getBytes());
				String hash = Utility.getHahSHA1(data);
				if (!hash.isEmpty()) {
					Piece piece = new Piece(index, hash);
					piece.setData(data);
					boolean stored = fileManager.storePiece(infoHash, piece);
					if (stored) {
						LOGGER.trace("Piece: " + piece.getIndex() + " stored successfully!");
					} else {
						LOGGER.trace("Invalid Piece: " + index + " storing unsuccessfull!");
					}
				} else {
					LOGGER.error("Generating of piece hash failed!");
				}
			} else if (message.startsWith("NOPIECE")) {
				LOGGER.trace("This peer doesn't have a piece: " + message);
			} else if (message.startsWith("KEEPALIVE")) {
				LOGGER.trace("Server sended KEEPALIVE");
			} else {
				LOGGER.warn("SYNTAX ERROR " + message);
			}
		}
	}

	/**
	 * Ask Peer for Piece or Disconnect.
	 * 
	 * @throws InterruptedException
	 */
	public void processWrite() throws InterruptedException {
		if (job.isDone() && state.isAlive()) {
			LOGGER.info("Client sends disconnect");
			state.clearWriteQueue();
			state.clearReadQueue();
			state.enqueueWrite("DISCONNECT");
			Thread.sleep(DISCONNECT_WAIT);
			state.setKill(true);
		} else if (state.isAlive()) {
			Piece piece = piecesQueue.dequeuePiece();
			if (piece != null) {
				String infoHash = job.getTorrentInfoHash();
				state.enqueueWrite("PIECEEXISTS " + infoHash + " " + piece.getIndex());
				LOGGER.trace("Piece: " + piece.getIndex() + " SENDING!");
			} else {
				setPieceQueue();
			}
		}
	}

	private void setPieceQueue() {
		long timeEnd = System.currentTimeMillis();
		long timeDelta = timeEnd - timeStart;
		if (timeDelta > TIMER_TO_SET_PIECE_QUEUE) {
			LOGGER.trace("Enqueue PIECES");
			for (Piece piece : job.getPieces()) {
				if (!piece.isStored()) {
					piecesQueue.enqueuePiece(piece);
				}
			}
			if (!piecesQueue.hasPiece()) {
				LOGGER.trace("ENQUEUE NO MORE PIECES");
				job.setDone(true);
			}
			timeStart = System.currentTimeMillis();
		}
	}
}
