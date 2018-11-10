package main;

import java.io.DataOutputStream;
import java.io.IOException;

public class Writer implements Runnable {

	private ConnectionState state;
	private DataOutputStream output = null;
	private String mesg = "";
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
					mesg = message;
					// if queue is empty then don't send message
					if (message != null && message.length() > 0) {
						writeOut(message);
						System.out.println("Writer message: " + message);
					}
				}
				Thread.sleep(1);
			}
			this.output.close();
			System.out.println("Writer Terminated Gracefully!");
		} catch (IOException | InterruptedException e) {
			System.err.println("Writer Terminated Error Unexpectedly! " + mesg);
			e.printStackTrace();
//			state.setKill(true);
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
