package main;

import static utils.Constants.*;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import file.CSVFileHandler;
import file.FileManager;

public class ServerHandler implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private Reader reader;
	private Writer writer;
	private ConnectionState state;
	private ArrayList<Socket> connections;
	private FileManager fileManager;
	private long timeStart;

	public ServerHandler(Socket socket, ArrayList<Socket> connections, FileManager fileManager) {
		LOGGER.info("Server -> Peer Connected: " + socket.getRemoteSocketAddress());		
		state = new ConnectionState(socket);		
		reader = new Reader(state);
		writer = new Writer(state);
		this.connections = connections;
		this.fileManager = fileManager;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("Server");
		timeStart = System.currentTimeMillis();
		Thread readerThread = new Thread(reader, "Server Reader Thread");
		Thread writerThread = new Thread(writer, "Server Writer Thread");
		readerThread.start();
		writerThread.start();
				
		while (state.isAlive()) {
			try {
				processCommands();
				Thread.sleep(DEFAULT_BANDWIDTH);
				keepAlive();
			} catch (InterruptedException e) {
				LOGGER.error("Thread sleep has been interrupted.", e);
			}
		}

		try {
			LOGGER.info("Server, Reader and Writer Threads are finishing before closing.");
			readerThread.join();
			writerThread.join();
			state.terminate();
		} catch (InterruptedException e) {
			LOGGER.error("Closing threads has been interrupted.", e);
		} catch (IOException e) {
			LOGGER.error("Closing socket failed.", e);
		}
		LOGGER.info("Server Process Terminated ...");
		connections.remove(state.getSocket());
	}
	
	private void keepAlive() {
		long timeEnd = System.currentTimeMillis();
		long timeDelta = timeEnd - timeStart;
		if (timeDelta > KEEP_ALIVE_TIMER) {
			state.enqueueWrite("KEEPALIVE");
			timeStart = System.currentTimeMillis();
		}
	}

	public void processCommands() throws InterruptedException {
		if (state.hasRead()) {
			String message = state.dequeueRead();

			// PIECEEXISTS FILE_ID_HASH PIECE_INDEX
			if (message.startsWith("PIECEEXISTS")) {
				String[] messageSplit = message.split(" ");
				String infoHash = messageSplit[1].trim();
				int index = Integer.parseInt(messageSplit[2].trim());
				Optional<Piece> piece = fileManager.getPiece(infoHash, index);
				if (!piece.isPresent()) {
					state.enqueueWrite("NOPIECE " + infoHash + " " + index);					
				}
				else if (piece.isPresent()) {
					Optional<byte[]> data = piece.get().getData();
					if (data.isPresent()) {
						String dataEncoded = new String(Base64.encodeBase64(data.get()));
						
						// TEST time of receiving
						CSVFileHandler.writeTime("Send", index);
						state.enqueueWrite("HAVEPIECE " + infoHash + " " + index + " " + dataEncoded);						
					} else {
						state.enqueueWrite("NOPIECE " + infoHash + " " + index);
						LOGGER.error("Data not present.");
					}
				} else {
					state.enqueueWrite("NOPIECE " + infoHash + " " + index);
					LOGGER.trace("Piece not present.");
				}
			} else if (message.startsWith("DISCONNECT")) {
				LOGGER.info("Server disconnects ...");
				Thread.sleep(100);
				state.setKill(true);
			} else {
				LOGGER.warn("SYNTAX ERROR " + message);
			}
		} 
	}
}
