package main;

import java.io.DataInputStream;
import java.io.IOException;

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
			String message = "";
			while (state.isAlive()) {
				if (input.available() > 0) {
					message = read();
					state.enqueueRead(message);
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					LOGGER.error("Thread sleep has been interrupted.", e);
				}
			}
			this.input.close();
			LOGGER.info("Reader Terminated Gracefully!");
		} catch (IOException e) {
			LOGGER.error("Reader Thread failed to read from input stream.", e);
			state.setKill(true);
		}
	}

	public synchronized String read() throws IOException {
		int length = input.readInt();
		byte[] message = null;
		String data;
		if (length > 0) {
			message = new byte[length];
			input.readFully(message, 0, message.length); // read the message
			data = new String(message, "UTF-8");
		} else {
			data = "";
		}
		return data;
	}
}
