package de.hageldave.texttophone.android;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

/** Client der Nachrichten vom Server empfaengt */
public class MessageReceivingClient extends Thread {
	
	/** socket der mit server verbunden ist */
	private Socket connectionToServer;
	
	/** von server kommender Stream */
	private ObjectInputStream inputStream;
	
	/** zu server gehender Stream */
	private ObjectOutputStream outputStream;
	
	/** gibt an ob immernoch eine verbindung zum server besteht */
	public boolean isConnected;
	
	/** blocking queue fuer empfangene Nachrichten (thread save) */
	private LinkedBlockingQueue<String> rcvdMessages = new LinkedBlockingQueue<String>();
	
	/** liste der {@linkplain NewMessageListener}s fuer diesen client */
	private LinkedList<NewMessageListener> newMsgListeners = new LinkedList<NewMessageListener>();
	
	/** liste der {@linkplain LostConnectionListener}s fuer diesen client */
	private LinkedList<LostConnectionListener> lostCnnctListeners = new LinkedList<LostConnectionListener>();
	
	
	/** 
	 * holt naechste Nachricht aus der Nachrichten queue. (entfernt nachricht)
	 * @return Nachricht oder null falls keine Nachrichten in queue
	 */
	public String getNextMessage(){
		if(rcvdMessages.isEmpty()){
			return null;
		} else {
			try {
				return rcvdMessages.take();
			} catch (InterruptedException e) {
				return null;
			}
		}
	}
	
	/** @return anzahl der nachrichten in der queue */
	public int countMessages(){
		return rcvdMessages.size();
	}
	
	/**
	 * versucht eine Verbindung zu gegebenem host aufzubauen.
	 * @param host name/adresse des servers
	 * @param port des servers
	 */
	public void attemptConnection(String host, int port){
		isConnected = connectToServer(host, port);
	}
	
	
	/**
	 * stellt verbindung zum server her und oeffnet streams
	 * @param host name/adresse des servers
	 * @param port port ueber den mit server kommuniziert wird
	 * @return true wenn verbindung hergestellt, sonst false
	 */
	private boolean connectToServer(String host, int port){
		try {
			// verbindungslosen socket erstellen
			this.connectionToServer = new Socket();
			connectionToServer.bind(null);
			// verbindungs aufbau mit timeout(1s) falls server nicht antwortet
			connectionToServer.connect(new InetSocketAddress(host, port), 1000);
			
			/* zuerst output oeffnen weil server thread zuerst input oeffnen 
			 * will. andersrum warten beide bis der andere den entsprechenden 
			 * stream geoeffnet hat */
			this.outputStream = 
					new ObjectOutputStream(connectionToServer.getOutputStream());
			this.inputStream = 
					new ObjectInputStream(connectionToServer.getInputStream());
			
		} catch (SecurityException e){
			System.out.println("could not connect, security exception");
			return false;
		} catch (SocketException e){
			System.out.println("could not connect, server may be down");
			return false;
		} catch (UnknownHostException e) {
			System.out.println(host + " is not a valid host");
			return false;
		} catch (IOException e) {
			System.out.println("IO Exception, could not connect");
			e.printStackTrace();
			return false;
		}
		System.out.println("connected to server " + host);
		// name zur identifizierung auf server senden
		sendMyName();
		return true;
	}
	
	
	@Override
	public void run() {
		while(isConnected){
			try {
				// auf nachricht von server warten
				awaitMessage();
			} catch (SocketException e) {
				System.out.println("lost connection to server");
				closeConnection();
			} catch (IOException e) {
				System.out.println("lost connection to server");
				closeConnection();
			} catch (ClassNotFoundException e) {
				System.out.println("received unknown object");
				sendUnknownObjectReceived();
			}
		}
		
		closeConnection();
	}
	
	/** 
	 * wartet auf Nachricht von server und steckt sie dann in die queue
	 * @throws IOException wenn inputstream eine wirft
	 * @throws ClassNotFoundException falls unbekanntes Objekt empfangen
	 */
	private void awaitMessage() throws IOException, ClassNotFoundException{
		// aus inputstream lesen
		Object input = inputStream.readObject();
		// falls ein string empfangen wurde, ab in die queue damit
		if(input instanceof String){
			String msg = (String) input;
			try {
				this.rcvdMessages.put(msg);
				notifyNewMessage();
			} catch (InterruptedException e) {
				closeConnection();
			}
		}
	}
	
	
	/**
	 * sendet nachricht an server dass ein unbekannstes objekt 
	 * empfangen wurde
	 */
	private void sendUnknownObjectReceived(){
		try {
			this.outputStream.writeObject("CLIENT: recieved unknown object");
		} catch (SocketException e) {
			System.out.println("lost connection to server");
			closeConnection();
		} catch (IOException e) {
			System.out.println("lost connection to server");
			closeConnection();
		} 
	}
	
	/**
	 * sendet eine Nachricht mit einen Namen an server. <br>
	 * der Name dient zur identifizierung des clients auf dem server. <br>
	 * es wird der name des handy models verwendet
	 */
	private void sendMyName(){
		try {
			this.outputStream.writeObject(android.os.Build.MODEL);
		} catch (SocketException e) {
			System.out.println("lost connection to server");
			closeConnection();
		} catch (IOException e) {
			System.out.println("lost connection to server");
			closeConnection();
		} 
	}
	
	
	/**
	 * schliesst die verbindung zum server <br>
	 * (socket, inputstream, outputstream)
	 */
	public void closeConnection(){
		try {
			// schliesse streams und socket
			if(inputStream != null){
				this.inputStream.close();
			}
			if(outputStream != null){
				this.outputStream.close();
			} 
			if(connectionToServer != null){
				this.connectionToServer.close();
			}
		} catch (SocketException e) {
			// socket schon geschlossen
		} catch (IOException e) {
			// streams schon geschlossen
		}
		System.out.println("closed connection");
		this.isConnected = false;
		this.interrupt();
		// benachrichtigen dass verbindung abgebrochen wurde
		notifyLostConnection();
	}
	
	
	/** fuegt einen {@link NewMessageListener} zu diesem client hinzu */
	public void addNewMessageListener(NewMessageListener listener){
		synchronized (newMsgListeners) {
			this.newMsgListeners.add(listener);
		}
	}
	
	/** benachrichtigt listeners ueber neue Nachricht */
	private void notifyNewMessage() {
		synchronized (newMsgListeners) {
			for (NewMessageListener listener : newMsgListeners) {
				listener.onMessageReceived(countMessages());
			}
		}
	}
	
	/** fuegt einen {@link LostConnectionListener} zu diesem client hinzu */
	public void addConnectionListener(LostConnectionListener listener) {
		synchronized (lostCnnctListeners) {
			this.lostCnnctListeners.add(listener);
		}
	}
	
	/** benachrichtigt listeners ueber verbindungsabbruch */
	private void notifyLostConnection() {
		synchronized (lostCnnctListeners) {
			for (LostConnectionListener listener : lostCnnctListeners) {
				listener.onLostConnection();
			}
		}
	}
	
	
	// ->> listener interfaces <<-
	
	
	/** Interface fuer einen NewMessageListener */
	public static interface NewMessageListener {
		
		/** 
		 * wird ausgefuehrt wenn client eine neue nachricht empfaengt
		 * @param numberOfMessages anzahl der Nachrichten in der queue
		 */
		public void onMessageReceived(int numberOfMessages);
	}
	
	
	/** Interface fuer einen LostConnectionListener */
	public static interface LostConnectionListener {
		
		/**
		 * wird ausgefuehrt wenn die verbindung des clients zum server abbricht
		 */
		public void onLostConnection();
	}
	
}
