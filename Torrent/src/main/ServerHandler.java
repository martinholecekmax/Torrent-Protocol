package main;

import static utils.Constants.*;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

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

	public ServerHandler(Socket socket, ArrayList<Socket> connections, FileManager fileManager) {
		state = new ConnectionState(socket);
		reader = new Reader(state);
		writer = new Writer(state);
		this.connections = connections;
		this.fileManager = fileManager;
	}

	@Override
	public void run() {
		Thread readerThread = new Thread(reader, "Server Reader Thread");
		Thread writerThread = new Thread(writer, "Server Writer Thread");

		readerThread.start();
		writerThread.start();

		while (state.isAlive()) {
			try {
				processCommands();
				Thread.sleep(DEFAULT_BANDWIDTH);
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

	public void processCommands() throws InterruptedException {
		if (state.hasRead()) {
			String message = state.dequeueRead();

			// PIECEEXISTS FILE_ID_HASH PIECE_INDEX
			if (message.startsWith("PIECEEXISTS")) {
				String[] messageSplit = message.split(" ");
				String infoHash = messageSplit[1].trim();
				int index = Integer.parseInt(messageSplit[2].trim());
				Piece piece = fileManager.getPiece(infoHash, index);
				byte[] data = piece.getData();
				if (data != null) {
					String dataEncoded = new String(Base64.encodeBase64(data));
					
					// TEST time of receiving
					CSVFileHandler.writeTime("Send", index);
					state.enqueueWrite("HAVEPIECE " + infoHash + " " + index + " " + dataEncoded);
				} else {
					// FILE MANAGER DOESN'T HAVE FILE
					state.enqueueWrite("NOPIECE " + infoHash + " " + index);
				}

			} else if (message.startsWith("KEEPALIVE")) {
				LOGGER.info("KEEPALIVE from client");
			} else if (message.startsWith("DISCONNECT")) {
				LOGGER.info("Server disconnects ...");
//				state.enqueueWrite("DISCONNECTED");
				Thread.sleep(100);
				state.clearReadQueue();
				state.clearWriteQueue();
				state.setKill(true);
			} else {
				LOGGER.warn("SYNTAX ERROR " + message);
			}
		}
	}
}
