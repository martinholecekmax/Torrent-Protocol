package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

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
	
	public Server(ServerSocket serverSocket, FileManager fileManager) {
		this.serverSocket = serverSocket;
		this.fileManager = fileManager;
		this.connections = new ArrayList<>();
		jobUpdater = new JobUpdater(fileManager);
	}

	@Override
	public void run() {
		try {
			Thread jobUpdaterThread = new Thread(jobUpdater, "Job Updater Thread");
			jobUpdaterThread.start();

			LOGGER.info("Server Created ...");
			LOGGER.info("IP: " + serverSocket.getInetAddress().toString());
			LOGGER.info("Port: " + serverSocket.getLocalPort());

//			ExecutorService executorService = Executors.newFixedThreadPool(MAX_SERVER_THREADS);

			while (running) {
				Socket socket = serverSocket.accept();
				socket.setSoTimeout(SOCKET_TIMEOUT);

//				ServerHandler connection = new ServerHandler(socket, connections, fileManager);
//				executorService.submit(connection);

				ServerHandler connection = new ServerHandler(socket, connections, fileManager);
				Thread connectionThread = new Thread(connection, "Connection Manager - New Connection Thread");
				connectionThread.start();

				connections.add(socket);
			}
			serverSocket.close();
		} catch (IOException e) {
			LOGGER.error("Server Socket failed.", e);
		}
	}
}
