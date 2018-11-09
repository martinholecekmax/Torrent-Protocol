package main;

import java.io.DataOutputStream;
import java.io.IOException;

import file.CSVFileHandler;

public class Writer implements Runnable {

	private ConnectionState state;
	private DataOutputStream output = null;

	public Writer(ConnectionState state) {
		this.state = state;
	}

	@Override
	public void run() {
		try {
			this.output = new DataOutputStream(state.getSocket().getOutputStream());
			
			while (state.isAlive() || state.hasWrite()) {
				while (state.hasWrite()) {
					String message = state.dequeueWrite();
					// if queue is empty then don't send message
					if (message != null && message.length() > 0) {
						writeOut(message);
					}
				}
				Thread.sleep(1);
			}
			this.output.close();
			System.out.println("Writer Terminated Gracefully!");
		} catch (IOException | InterruptedException e) {
			System.out.println("Writer Terminated Unexpectedly!");
			e.printStackTrace();
			state.setKill(true);
		}
	}

	public synchronized void writeOut(String msg) throws IOException {
		byte[] message = getAsciiBytes(msg);
		output.writeInt(message.length);
		output.flush();
		output.write(message);
		output.flush();
	}

	public static byte[] getAsciiBytes(String input) {
		char[] character = input.toCharArray();
		byte[] ascii = new byte[character.length];
		for (int asciiValue = 0; asciiValue < character.length; asciiValue++) {
			ascii[asciiValue] = (byte) (character[asciiValue] & 0x007F);
		}
		return ascii;
	}
}
