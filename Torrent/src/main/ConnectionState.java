package main;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class ConnectionState {
	private volatile boolean kill = false;
	private Socket socket;
	private Queue<String> readQueue;
	private Queue<String> writeQueue;

	private Object readQueueLock = new Object();
	private Object writeQueueLock = new Object();

	public ConnectionState(Socket socket) {
		this.socket = socket;
		readQueue = new LinkedList<>();
		writeQueue = new LinkedList<>();
	}

	public void setKill(boolean kill) {
			this.kill = kill;
	}

	public boolean hasRead() {
		synchronized (readQueueLock) {
			if (readQueue.isEmpty() == false) {
				return true;
			}
			return false;
		}
	}

	public boolean hasWrite() {
		synchronized (writeQueueLock) {
			if (writeQueue.isEmpty() == false) {
				return true;
			}
			return false;
		}
	}

	public void enqueueRead(String message) {
		synchronized (readQueueLock) {
			readQueue.add(message);
		}
	}

	public void enqueueWrite(String message) {
		synchronized (writeQueueLock) {
			writeQueue.add(message);
		}
	}

	public String dequeueRead() {
		synchronized (readQueueLock) {
			return readQueue.poll();
		}
	}

	public Optional<String> dequeueWrite() {
		synchronized (writeQueueLock) {
			return Optional.of(writeQueue.poll());
		}
	}

	public boolean isAlive() {
			return !kill;			
	}

	public void clearWriteQueue() {
		synchronized (writeQueueLock) {
			writeQueue.clear();
		}		
	}
	
	public void clearReadQueue() {
		synchronized (readQueueLock) {
			readQueue.clear();
		}		
	}
	
	public void terminate() throws IOException {
		socket.close();
	}

	public Socket getSocket() {
		return socket;
	}
}
