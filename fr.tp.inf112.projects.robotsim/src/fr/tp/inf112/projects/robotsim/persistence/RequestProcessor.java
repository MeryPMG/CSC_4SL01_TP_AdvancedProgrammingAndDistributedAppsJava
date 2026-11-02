package fr.tp.inf112.projects.robotsim.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.robotsim.model.Factory;

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
			InputStream bufInStream = new BufferedInputStream(inpStr);
			ObjectInputStream objInStream = new ObjectInputStream(bufInStream);
            
			Object receivedObj = objInStream.readObject();
			
			FactoryPersistenceManager factMgr = new FactoryPersistenceManager(null);
			
			if (receivedObj instanceof String) {
				String factoryId = (String) receivedObj;
				
				Canvas factory = factMgr.read(factoryId);
				
				OutputStream outStr = socket.getOutputStream();
	            OutputStream bufOutStream = new BufferedOutputStream(outStr);
	            ObjectOutputStream objOutStream = new ObjectOutputStream(bufOutStream);
	            
	            objOutStream.writeObject(factory);
	            objOutStream.flush();
	            LOGGER.info("Sent factory model to the client");
			} else if (receivedObj instanceof Factory) {
				Factory factory = (Factory) receivedObj;
				
				factMgr.persist(factory);
				LOGGER.info("Data from the client has been persisted");
			} else if (receivedObj == PersistenceRequest.LIST_FILES) {
			    File workingDirectory = new File(".");
			    File[] files = workingDirectory.listFiles();

			    List<String> fileNames = new ArrayList<String>();

			    if (files != null) {
			        for (File file : files) {
			            if (file.isFile() && file.getName().endsWith(".factory")) { // Only include files from the current directory
			                fileNames.add(file.getName());
			            }
			        }
			    }

			    String[] fileNamesArray = fileNames.toArray(new String[0]);

			    OutputStream outStr = socket.getOutputStream();
			    OutputStream bufOutStream = new BufferedOutputStream(outStr);
			    ObjectOutputStream objOutStream = new ObjectOutputStream(bufOutStream);

			    objOutStream.writeObject(fileNamesArray);
			    objOutStream.flush();

			    LOGGER.info("Sent list of existing model file names to the client");
			}
		} catch (IOException | ClassNotFoundException ex) {
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
