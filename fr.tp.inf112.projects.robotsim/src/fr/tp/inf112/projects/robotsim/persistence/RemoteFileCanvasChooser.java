package fr.tp.inf112.projects.robotsim.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;

public class RemoteFileCanvasChooser extends FileCanvasChooser {
	private static final Logger LOGGER = Logger.getLogger(RemoteFileCanvasChooser.class.getName());
	private static final int SERVER_PORT = 55000;
	
	public RemoteFileCanvasChooser(final String fileExtension,
								   final String documentTypeLabel) {
		super(null, fileExtension, documentTypeLabel);
	}
	
	@Override
	protected String browseCanvases(final boolean open) throws IOException {
		if (open) {
            String[] fileNames = getRemoteFileNames();

            if (fileNames.length == 0) {
            	JOptionPane.showMessageDialog(null, "No factory models are available on the server.");
                return null;
            }

            return (String) JOptionPane.showInputDialog(
					                    null,
					                    "Select a factory model:",
					                    "Open Factory Model",
					                    JOptionPane.PLAIN_MESSAGE,
					                    null,
					                    fileNames,
					                    fileNames[0]);
            
        }
		return JOptionPane.showInputDialog("Enter the factory model file name:");
	}
	
	// Asks the persistence server for a list of existing model file names on its file 
	// system
	private String[] getRemoteFileNames() throws IOException {
		InetAddress serverNetAddr = InetAddress.getByName("localhost");

		try (
				Socket socket = new Socket(serverNetAddr, SERVER_PORT)
		) {
			OutputStream outStr = socket.getOutputStream();
			OutputStream bufOutStream = new BufferedOutputStream(outStr);
			ObjectOutputStream objOutStream = new ObjectOutputStream(bufOutStream);

			objOutStream.writeObject(PersistenceRequest.LIST_FILES);
			objOutStream.flush();

			InputStream inpStr = socket.getInputStream();
			InputStream bufInStream = new BufferedInputStream(inpStr);
			ObjectInputStream objInStream = new ObjectInputStream(bufInStream);

			try {
				String[] receivedFilenames = (String[]) objInStream.readObject();
				
				LOGGER.config("Received existing model file names from the persistence server");
				
				return receivedFilenames;
			} catch (ClassNotFoundException ex) {
				LOGGER.log(Level.SEVERE, "Error receiving model file names from the persistence server");
				throw new IOException(ex);
			}
		}
	}
}
