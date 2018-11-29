package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import file.FileManager;
import static utils.Constants.*;

public class Server implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);
	private ServerSocket serverSocket = null;
	private boolean running = true;
	private FileManager fileManager;
	private ArrayList<Socket> connections;
	private JobUpdater jobUpdater;
	private ExecutorService executorService;
	private ExecutorService jobUpdaterService;	
	
	public Server(ServerSocket serverSocket, FileManager fileManager) {
		this.serverSocket = serverSocket;
		this.fileManager = fileManager;
		this.connections = new ArrayList<>();
		jobUpdater = new JobUpdater(fileManager);
	}

	@Override
	public void run() {
		try {
			jobUpdaterService = Executors.newSingleThreadExecutor();
			jobUpdaterService.submit(jobUpdater);			
						
			LOGGER.info("Server Created ...");
			
			executorService = Executors.newFixedThreadPool(MAX_SERVER_THREADS);

			while (running) {
				Socket socket = serverSocket.accept();				

				connections.add(socket);
				ServerHandler connection = new ServerHandler(socket, connections, fileManager);
				executorService.submit(connection);
			}
		} catch (IOException e) {
			if (running) {
				LOGGER.error("Server Socket failed. Accept method interrupted." + e.getMessage());
			} else {
				LOGGER.info("Server Closed Gracefully.");
			}			
		}
	}
	
	public void close() {
		running = false;
		jobUpdater.close();		
		jobUpdaterService.shutdownNow();
		closeServerExecutor();				
		closeServerSocket();		
	}

	private void closeServerSocket() {
		if (serverSocket != null) {
			try {
				if (!serverSocket.isClosed()) {
					serverSocket.close();					
				}
			} catch (IOException e) {
				LOGGER.error("Something wrong with closing server socket.", e);
			}				
		}
	}

	private void closeServerExecutor() {
		try {
			executorService.shutdown();
			executorService.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Server Executor Termination interrupted!", e);
		} finally {
			if (!executorService.isTerminated()) {
				LOGGER.error("Server Executor Cancel non-finished tasks!");
			}
			executorService.shutdownNow();
		}
	}
}
