package fr.tp.inf112.projects.robotsim.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestProcessor implements Runnable {
	private static final Logger LOGGER = Logger.getLogger(RequestProcessor.class.getName());
	
	private Socket socket;
	
	public RequestProcessor(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {
			InputStream inpStr = socket.getInputStream();
			Reader strReader = new InputStreamReader(inpStr);
            BufferedReader buffReader = new BufferedReader(strReader);
            
            OutputStream outStr = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outStr, true);
            
            String response = buffReader.readLine();
            LOGGER.info("Client message: " + response);
            
            String message = "I received " + response + "!";
            writer.println(message);
            LOGGER.info("Sent: " + message);
		} catch(IOException ex) {
			LOGGER.log(Level.SEVERE, "Server thread communication error", ex);
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
