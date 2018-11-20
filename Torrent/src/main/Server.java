package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
			LOGGER.info("IP: " + serverSocket.getInetAddress().toString());
			LOGGER.info("Port: " + serverSocket.getLocalPort());
			
			executorService = Executors.newFixedThreadPool(MAX_SERVER_THREADS);

			while (running) {
				Socket socket = serverSocket.accept();				

				connections.add(socket);
				ServerHandler connection = new ServerHandler(socket, connections, fileManager);
				executorService.submit(connection);
			}
		} catch (IOException e) {
			LOGGER.error("Server Socket failed. Accept method interrupted." + e.getMessage());
		}
	}
	
	public void close() {
		running = false;
		if (!jobUpdaterService.isShutdown()) {			
			jobUpdaterService.shutdownNow();
		}
		if (!executorService.isShutdown()) {
			executorService.shutdownNow();			
		}
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
}
