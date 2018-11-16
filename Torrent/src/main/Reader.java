package main;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Optional;

import org.apache.log4j.Logger;

public class Reader implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private ConnectionState state;
	private DataInputStream input = null;

	public Reader(ConnectionState state) {
		this.state = state;
	}

	@Override
	public void run() {
		try {
			this.input = new DataInputStream(state.getSocket().getInputStream());
			while (state.isAlive()) {
				if (input.available() > 0) {
					Optional<String> message = read();
					if (message.isPresent()) {
						state.enqueueRead(message.get());						
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					LOGGER.error("Thread sleep has been interrupted.");
				}
			}
			this.input.close();
			LOGGER.info("Reader Terminated Gracefully!");
		} catch (IOException e) {
			LOGGER.error("Reader Thread failed to read from input stream.");
			state.setKill(true);
		}
	}

	public synchronized Optional<String> read() throws IOException {
		int length = input.readInt();
		if (length > 0) {
			byte[] message = new byte[length];
			input.readFully(message, 0, message.length); // read the message
			String data;
			data = new String(message, "UTF-8");
			return Optional.of(data);
		} else {
			return Optional.empty();
		}
	}
}
