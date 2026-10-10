package fr.tp.inf112.projects.robotsim.model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HelloWorldServer {
	private static final Logger LOGGER = Logger.getLogger(HelloWorldServer.class.getName());
	
	private static final int SERVER_PORT = 55000;
	
	public static void main(String[] args) {
		try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
			do {
				Socket socket = serverSocket.accept();
				Runnable reqProcessor = new RequestProcessor(socket);
				new Thread(reqProcessor).start();
			} while (true);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Server communication error", ex);
		}
	}
}
