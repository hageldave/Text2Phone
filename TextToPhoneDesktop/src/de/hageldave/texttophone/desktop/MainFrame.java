package de.hageldave.texttophone.desktop;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * MainFrame der Anwendung, Benutzer kann damit den Server auf einem 
 * gewuenschten Port starten, und stoppen.
 */
public class MainFrame extends JFrame {
	
	/** hier wird der gewuenschte Port eingetragen */
	JTextField textfld_port = new JTextField();
	
	/** label "port to use:" */
	JLabel label_port = new JLabel("port to use: ");
	
	/** button um server zu starten / zu stoppen */
	JButton button_start_stop;
	
	/** label fuer serveradresse (zeigt fehler wenn unmoeglicher port) */
	JLabel label_adress = new JLabel("Serveradress");
	
	/** Server mit dem sich die Telefone verbinden koennen */
	MultiThreadedMessageServer server;
	
	
	/** Konstruktor, erstellt das gui */
	public MainFrame() {
		// titel und icon des fensters
		setTitle(" Text2Phone");
		setIconImage(new ImageIcon("res/text2phone_icon.png").getImage());
		// Komponenten hinzufuegen
		buildStartStopButton();
		getContentPane().add(button_start_stop);
		getContentPane().add(label_port);
		getContentPane().add(textfld_port);
		getContentPane().add(label_adress);
		// fenster setup
		this.setLayout(null);
		this.setResizable(false);
		this.setPreferredSize(new Dimension(300, 200));
		this.setLocationByPlatform(true);
		this.pack();
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setVisible(true);
		// Komponenten Groessen
		button_start_stop.setSize(120, 30);
		label_port.setSize(120, 30);
		textfld_port.setSize(120, 30);
		label_adress.setSize(160, 30);
		// Komponenten Anordnung
		label_port.setLocation(90, 15);
		textfld_port.setLocation(90, 50);
		button_start_stop.setLocation(90, 90);
		label_adress.setLocation(70, 125);
		// label texte zentrieren
		label_adress.setHorizontalAlignment(JLabel.CENTER);
		label_port.setHorizontalAlignment(JLabel.CENTER);
		
		System.out.println("started program");
	}
	
	/** setup fuer den start/stop button */
	private void buildStartStopButton(){
		this.button_start_stop = new JButton();
		button_start_stop.setAction(new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				startstopServer();
			}
		});
		button_start_stop.setText("Start Server");
	}
	
	/** 
	 * wird beim druecken des buttons ausgefuehrt <br>
	 * startet bzw stoppt den server und aendert dem entsprechend
	 * die Komponenten des fensters.
	 */
	private void startstopServer(){
		if(this.server == null){
			Integer port = getPort();
			if(port != null){
				try {
					server = new MultiThreadedMessageServer(port, this);
					this.button_start_stop.setText("Stop Server");
					this.textfld_port.setEnabled(false);
					showServeradress(true);
					System.out.println("started server");
				} catch (IllegalArgumentException e) {
					// fail, benutzer informieren dass port nicht geht
					showBadPort();
					System.out.println("not good port1");
					return;
				}
			} else {
				// fail, benutzer informieren dass port nicht geht
				showBadPort();
				System.out.println("not good port2");
				return;
			}
		} else {
			this.server.interrupt();
			this.server = null;
			this.button_start_stop.setText("Start Server");
			this.textfld_port.setEnabled(true);
			showServeradress(false);
		}
	}
	
	/**
	 * zeigt serveradresse auf {@link #label_adress} an, bzw "Serveradress"
	 * wenn der Server nicht laeuft.
	 * @param show boolean, wenn true, wird adresse angezeigt
	 */
	private void showServeradress(boolean show){
		// normale farbe fuer die schrift
		this.label_adress.setForeground(getForeground());
		if(show){
			this.label_adress.setText(getlocalIP());
		} else {
			this.label_adress.setText("Serveradress");
		}
	}
	
	private String getlocalIP(){
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
			NetworkInterface nI;
			while(interfaces.hasMoreElements()){
				nI = interfaces.nextElement();
				Enumeration<InetAddress> adresses= nI.getInetAddresses();
				InetAddress iA;
				while(adresses.hasMoreElements()){
					iA = adresses.nextElement();
					if(!iA.isLoopbackAddress() && iA.isSiteLocalAddress()){
						return iA.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
		}
		return null;
	}
	
	/**
	 * zeigt "Bad port!" auf {@link #label_adress} an. <br>
	 * wird verwendet wenn bei serverstart herauskommt dass der
	 * gewaehlte port unmoeglich ist.
	 */
	private void showBadPort(){
		// rote farbe fuer die schrift
		this.label_adress.setForeground(Color.red);
		this.label_adress.setText("Bad port!");
	}
	
	
	@Override
	public void dispose() {
		// beende server beim schliessen des fensters
		if(server != null){
			server.interrupt();
		}
		super.dispose();
		System.exit(0);
	}
	
	/**
	 * @return gibt port nummer aus {@link #textfld_port} zurueck, bzw
	 * 		null falls dort keine zahl steht.
	 */
	private Integer getPort() {
		String txt = textfld_port.getText();
		System.out.println(txt);
		try {
			 return Integer.decode(txt);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	
	// ->> static methoden <<-
	
	
	/** main methode startet das MainFrame */
	public static void main(String[] args) {
		setGTKLookAndFeel();
		new MainFrame();
	}

	/** stellt lookandfeel der anwendung auf gtk */
	private static void setGTKLookAndFeel(){
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
		} catch (ClassNotFoundException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (UnsupportedLookAndFeelException e) {
		}
	}
	
}
