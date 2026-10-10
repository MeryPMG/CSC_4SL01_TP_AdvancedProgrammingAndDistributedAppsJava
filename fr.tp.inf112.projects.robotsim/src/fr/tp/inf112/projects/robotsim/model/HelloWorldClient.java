package fr.tp.inf112.projects.robotsim.model;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HelloWorldClient {
	private static final Logger LOGGER = Logger.getLogger(HelloWorldClient.class.getName());
			
	private static final int SERVER_PORT = 55000;
	
	private static Socket socket;
	
	public static void main(String[] args) {
		try {
			InetAddress serverNetAddr = InetAddress.getByName("localhost");
			socket = new Socket(serverNetAddr, SERVER_PORT);
			
			InputStream inpStr = socket.getInputStream();
			Reader strReader = new InputStreamReader(inpStr);
            BufferedReader buffReader = new BufferedReader(strReader);
            
            OutputStream outStr = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outStr, true);
            
            String message = "Hello World!";
            writer.println(message);
            LOGGER.info("Sent: " + message);
            
            String response = buffReader.readLine();
            LOGGER.info("Server response: " + response);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Client communication error", ex);
		} finally {
			if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Error while closing the socket", ex);
                }
            }
		}
	}
}
