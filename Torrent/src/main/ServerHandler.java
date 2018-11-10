package main;

import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;

import file.FileManager;

public class ServerHandler implements Runnable {
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
//			processCommands();
			try {
				test();
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			readerThread.join();
			writerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Server Process Terminated ...");
		connections.remove(state.getSocket());
		state.terminate();
	}
	
	public void test() throws InterruptedException {
		if (state.hasRead()) {
			String message = state.dequeueRead();
			if (message.startsWith("PIECEEXISTS")) {
				state.enqueueWrite("HAVEPIECE");
			} else if (message.startsWith("DISCONNECT")) {
				System.out.println("Server Disconnects ...");
				state.enqueueWrite("DISCONNECTED");
				Thread.sleep(100);
				state.setKill(true);
			} else {
				System.out.println("Syntax Error");
			}
		}
	}

	public void processCommands() {
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
					
//					CSVFileHandler.writeTime("Send", index);
					
					state.enqueueWrite("HAVEPIECE " + infoHash + " " + index + " " + dataEncoded);
				} else {
					// FILE MANAGER DOESN'T HAVE FILE
					state.enqueueWrite("NOPIECE " + infoHash + " " + index);
				}

			} else if (message.startsWith("KEEPALIVE")) {
				System.out.println("KEEPALIVE from client");
			} else if (message.startsWith("DISCONNECT")) {
				System.out.println("Server disconnects ...");
//				state.enqueueWrite("DISCONNECTED");
				state.setKill(true);
			} else {
				System.out.println("SYNTAX ERROR " + message);
			}
		}
	}
}
