package main;

import java.io.DataInputStream;
import java.io.IOException;

public class Reader implements Runnable {

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
			while (state.isAlive() || state.hasRead()) {
				if (input.available() > 0) {
					message = read();
					state.enqueueRead(message);
					System.out.println("Reader Message: " + message);
				}
				Thread.sleep(1);
			}
			this.input.close();
			System.out.println("Reader Terminated Gracefully!");
		} catch (IOException | InterruptedException e) {
			System.err.println("Reader Terminated Error!");
//			e.printStackTrace();
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
