package de.hageldave.texttophone.android;

/** Beinhaltet informationen zur verbindung mit einem Server */
public class ConnectionSetting {

	/** name / adresse des hosts */
	String hostName;
	
	/** port auf dem der server laeuft */
	int hostPort;
	
	/** 
	 * Konstruktor
	 * @param hostname {@link #hostName}
	 * @param port {@link #hostPort}
	 */
	public ConnectionSetting(String hostname, int port) {
		setHostName(hostname);
		setHostPort(port);
	}
	
	
	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getHostPort() {
		return hostPort;
	}

	public void setHostPort(int hostPort) {
		this.hostPort = hostPort;
	}
	
}
