package de.hageldave.texttophone.desktop;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import de.hageldave.texttophone.desktop.MultiThreadedMessageServer.MessageServerThread;

/**
 * "Chatfenster" um Nachrichten an client (verbundenes Handy) zu schicken.
 */
public class MessageDialog extends JDialog {
	
	/** serverthread ueber den die Nachrichten an den client gesendet werden */
	private MessageServerThread servThread;
	
	/** textarea in der die nachricht geschreiben wird */
	private JTextArea textarea = new JTextArea();
	
	/** button um die textarea zu leeren */
	private JButton button_clear;
	
	/** button um den text aus der textarea abzusenden */
	private JButton button_send;

	
	/**
	 * Konstruktor, erstellt das "Chatfenster"
	 * @param servthread ueber den mit client kommuniziert wird
	 * @param owner Frame dem dieser Dialog gehoert
	 */
	public MessageDialog(MessageServerThread servthread, Frame owner) {
		super(owner, " " + servthread.clientName);
		this.servThread = servthread;
		// dialog setup
		setIconImage(owner.getIconImage());
		setLayout(null);
		pack();
		setPreferredSize(new Dimension(300+getInsets().left*2, 230));
		setLocationByPlatform(true);
		pack();
		setResizable(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		// Komponenten erstellen
		buildClearButton();
		buildSendButton();
		JScrollPane scrollpane = wrapTextareaInScrollPane(textarea);
		// Komponenten groesse
		scrollpane.setSize(280, 150);
		button_clear.setSize(80, 30);
		button_send.setSize(190, 30);
		// Komponenten Anordnung
		scrollpane.setLocation(10, 5);
		button_clear.setLocation(10, 165);
		button_send.setLocation(100, 165);
		// Komponenten hinzufuegen
		getContentPane().add(scrollpane);
		getContentPane().add(button_clear);
		getContentPane().add(button_send);
		
		setVisible(true);
	}
	
	/**
	 * fuegt textarea in ein scrollpane ein und gibt es zurueck.
	 * @param textarea die in ein scrollpane eingefuegt wird
	 * @return scrollpane mit textarea
	 */
	private JScrollPane wrapTextareaInScrollPane(JTextArea textarea){
		// zeilen brechen um
		textarea.setLineWrap(true);
		textarea.setWrapStyleWord(true);
		// textarea in scrollpane
		return new JScrollPane(textarea);
	}
	
	/** setup fuer {@link #button_send} */
	private void buildSendButton(){
		this.button_send = new JButton();
		button_send.setAction(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String msg = textarea.getText();
				if(msg.length() > 0){
					servThread.sendMessage(msg);
				}
			}
		});
		button_send.setText("Send");
	}
	
	/** setup fuer {@link #button_clear} */
	private void buildClearButton(){
		this.button_clear = new JButton();
		button_clear.setAction(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				textarea.setText("");
			}
		});
		button_clear.setText("clear");
	}

	
	@Override
	public void dispose() {
		// disconnect wenn fenster geschlossen wird
		servThread.closeConnection();
		super.dispose();
	}
	
}
