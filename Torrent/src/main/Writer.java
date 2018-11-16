package main;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.apache.log4j.Logger;

public class Writer implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private ConnectionState state;
	private DataOutputStream output = null;

	public Writer(ConnectionState state) {
		this.state = state;
	}

	@Override
	public void run() {
		try {
			this.output = new DataOutputStream(state.getSocket().getOutputStream());

			while (state.isAlive()) {
				while (state.hasWrite()) {
					Optional<String> message = state.dequeueWrite();
					if (message.isPresent() && message.get().length() > 0) {						
						writeOut(message.get());
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					LOGGER.error("Thread sleep has been interrupted.");
				}
			}
			this.output.close();
			LOGGER.info("Writer Terminated Gracefully!");
		} catch (IOException e) {
			LOGGER.error("Writer Terminated Error Unexpectedly!");
			state.setKill(true);
		}
	}

	public synchronized void writeOut(String msg) throws IOException {
		byte[] message = msg.getBytes();
		output.writeInt(message.length);
		output.flush();
		output.write(message);
		output.flush();
	}
}
