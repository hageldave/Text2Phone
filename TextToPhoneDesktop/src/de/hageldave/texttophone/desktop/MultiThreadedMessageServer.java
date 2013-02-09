package de.hageldave.texttophone.desktop;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.swing.JFrame;


/** 
 * Multithreaded Server, der jedem client einen {@link MessageServerThread} 
 * zuordnet. 
 */
public class MultiThreadedMessageServer extends Thread {
	
	/** nimmt eingehende verbindungsanfragen an */
	private ServerSocket serverSocket;
	
	/** Frame das den server kontrolliert */
	private final JFrame ownerframe;
	
	/**
	 * Konstruktor
	 * @param port port über den der server laeuft
	 * @param ownerframe frame das den server startet und beendet
	 * @throws IllegalArgumentException wenn port nicht gueltig
	 */
	public MultiThreadedMessageServer(int port, JFrame ownerframe) throws IllegalArgumentException {
		this.ownerframe = ownerframe;
		try {
			// server socket der ueber prort 80 laeuft
			this.serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// sollte eig nicht passieren
			e.printStackTrace();
		}
		// startet den server
		this.start();
	}
	
	
	@Override
	public void run() {
		System.out.println("Server is running...");
		// loop solange nicht unterbrochen
		while(!isInterrupted()){
			// socket mit dem verbindung zum client hergestllt wird
			Socket newConnection;
			
			try {
				/* server socket bekommt verbindungsanfrage und gibt
				 * einen socket zurueck der die verbindung zum client haelt */
				newConnection = this.serverSocket.accept();
				
				// erstelle neuen serverthread der mit dem client kommuniziert
				System.out.println("try to resolve new connection");
				MessageServerThread messageServerThread = new MessageServerThread(newConnection, ownerframe);
				
				System.out.println("new connection: " + messageServerThread.connectionToClient.getInetAddress());
			} catch (IOException e) {
				// TODO: behandeln
			}
		}
		System.out.println("Server stopped");
	}
	
	
	@Override
	public void interrupt() {
		super.interrupt();
		// schliesse Socket
		try {
			this.serverSocket.close();
		} catch (IOException e) {
		}
	}
	
	
	
	// ->> innere Klasse <<-
	
	
	/**
	 * MessageServerThread der Verbindung zum client haelt und
	 * Textnachrichten an diesen sendet. Dazu oeffnet sich bei
	 * erfolgreicher Verbindung mit dem Client ein "Chatfenster".
	 */
	public static class MessageServerThread extends Thread {
		
		/** socket der mit client verbunden ist */
		Socket connectionToClient;
		
		/** von client kommender Stream */
		ObjectInputStream inputStream;
		
		/** zu client gehender Stream */
		ObjectOutputStream outputStream;
		
		/** gibt an ob immernoch eine verbindung zum client besteht */
		boolean isConnected;
		
		/** 
		 * name des Clients (wird nach verbindungsaufbau vom client mitgeteilt)
		 */
		String clientName = "";
		
		/** 
		 * {@link MessageDialog} mit dem Nachrichten an client gesendet werden
		 */
		MessageDialog messageDialog;
		
		
		/**
		 * Konstruktor
		 * @param commectionToClient socket der mit client verbunden ist
		 * @param ownerframe Frame das den Server kontrolliert und auch parent
		 * 		des {@link #messageDialog} wird
		 */
		public MessageServerThread(Socket connectionToClient, JFrame ownerframe) {
			this.connectionToClient = connectionToClient;
			// versuche streams zu oeffnen
			boolean streamsAreOpen = openStreams();
			if(streamsAreOpen){
				// starte den serverthread
				this.isConnected = true;
				this.messageDialog = new MessageDialog(this, ownerframe);
				start();
			}
		}
		
		/**
		 * oeffnet in- und outputstream
		 * @return true wenn erfolgreich, sonst false
		 */
		private boolean openStreams(){
			try {
				// zuerst input oeffnen weil client zuerst output oeffnet
				// andersrum warten beide bis der andere den entsprechenden stream geoeffnet hat
				System.out.println("opening instream");
				this.inputStream = new ObjectInputStream(
						connectionToClient.getInputStream());
				System.out.println("opening outstream");
				this.outputStream = new ObjectOutputStream(
						connectionToClient.getOutputStream());
			} catch (IOException e) {
				// wenn oeffnen der streams nicht geklappt hat
				e.printStackTrace();
				return false;
			}
			// oeffnen der streams erfolgreich
			receiveClientName();
			return true;
		}
		
		
		/**
		 * erste Nachricht von Client wird als {@link #clientName} verwendet. 
		 * <br>
		 * (Client sollte bei erfolgreicher verbindung einen Namen senden)
		 */
		private void receiveClientName(){
			Object input = null;
			try {
				input = inputStream.readObject();
			} catch (ClassNotFoundException e1) {
				closeConnection();
			} catch (IOException e1) {
				closeConnection();
			}
			if(input instanceof String){
				this.clientName = (String) input;
			}
		}
		
		
		/** 
		 * <b>(!)</b> in der gegebenen Anwendung schickt der client im Regelfall
		 * keine Nachrichten sondern empfängt nur. <br>
		 * Die Routine ist dennoch nuetzlich um einen Verbindungsabbruch
		 * zu bemerken.
		 */
		@Override
		public void run() {
			// loop solange mit client verbunden
			while(this.isConnected){
				try {
					// warte darauf dass client etwas schickt
					Object input = this.inputStream.readObject();
					// schreibe auf konsole was empfangen wurde
					System.out.println("recieved: " + input.toString());
				} catch (ClassNotFoundException e) {
					// wenn client unbekanntes objekt geschickt hat
					System.out.println("recieved: unknown object");
					sendUnknownObjectReceived();
				} catch (SocketException e) {
					System.out.println("lost connection to client " + connectionToClient.getInetAddress());
					closeConnection();
				} catch (IOException e) {
					System.out.println("lost connection to client " + connectionToClient.getInetAddress());
					closeConnection();
				} 
			}
			closeConnection();
			// schliesse chatfenster wenn verbindung abgebrochen
			messageDialog.dispose();
		}
		
		
		/**
		 * sendet nachricht an client dass ein unbekannstes objekt 
		 * empfangen wurde
		 */
		private synchronized void sendUnknownObjectReceived(){
			try {
				this.outputStream.writeObject("SERVER: recieved unknown object");
			} catch (SocketException e) {
				System.out.println("lost connection to client " + connectionToClient.getInetAddress());
				closeConnection();
			} catch (IOException e) {
				System.out.println("lost connection to client " + connectionToClient.getInetAddress());
				closeConnection();
			} 
		}
		
		/**
		 * schliesst die verbindung zum client
		 */
		public void closeConnection(){
			try {
				// schliesse streams und socket
				this.inputStream.close();
				this.outputStream.close();
				this.connectionToClient.close();
			} catch (SocketException e) {
				// socket schon geschlossen
			} catch (IOException e) {
				// streams schon geschlossen
			}
			this.isConnected = false;
		}
		
		
		/** 
		 * Schnittstelle um Nachrichten zu senden.
		 * @param msg Nachricht an client
		 */
		public synchronized void sendMessage(String msg){
			try {
				this.outputStream.writeObject(msg);
			} catch (IOException e) {
				System.out.println("IO-Exception occurred, disconnecting now.");
				closeConnection();
			}
		}
		
	}
	
	
}
