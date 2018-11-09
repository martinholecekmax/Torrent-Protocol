package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static utils.Constants.*;
import file.FileManager;

public class Server implements Runnable {

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
			
			System.out.println("Server Created ...");
			System.out.println("IP: " + serverSocket.getInetAddress().toString());
			System.out.println("Port: " + serverSocket.getLocalPort());
			
//			ExecutorService executorService = Executors.newFixedThreadPool(MAX_SERVER_THREADS);
			
			while (running) {
				Socket socket = serverSocket.accept();
				
//				ServerHandler connection = new ServerHandler(socket, connections, fileManager);
//				executorService.submit(connection);
				
				ServerHandler connection = new ServerHandler(socket, connections, fileManager);
				Thread connectionThread = new Thread(connection, "Connection Manager - New Connection Thread");
				connectionThread.start();
				
				connections.add(socket);
			}
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
