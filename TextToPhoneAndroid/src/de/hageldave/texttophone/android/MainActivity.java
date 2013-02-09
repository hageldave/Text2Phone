package de.hageldave.texttophone.android;

import de.hageldave.texttophone.android.MessageReceivingClient.LostConnectionListener;
import de.hageldave.texttophone.android.MessageReceivingClient.NewMessageListener;
import de.hageldave.texttophone.android.R;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * MainActivity der App, zeigt GUI an und enthaelt client der
 * Nachrichten vom Server bekommt.
 */
public class MainActivity extends Activity {
	
	/** clientthread der mit dem server kommuniziert */
	private MessageReceivingClient client;
	
	/** momentan gewaehlte Verbindungseinstellungen 
	 * @see {@link ConnectionSetting}
	 */
	public ConnectionSetting currentConnectionSetting = new ConnectionSetting("192.168.178.35", 80); //TODO: in einem menue auswaehlen, bzw erstellen
	
	/** Textview der App, auf dem empfangene nachrichten angezeigt werden */
	private TextView textview;

	/** button um verbindung aufzubauen oder zu trennen */
	private ToggleButton connect_disconnect_bttn;
	
	/** button um nachste nachricht anzuzeigen */
	private Button nextmsg_bttn;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// textview aus layout holen
		this.textview = (TextView)findViewById(R.id.rcvd_message);
		// textview scrollfaehig machen
		this.textview.setMovementMethod(ScrollingMovementMethod.getInstance());
		// connectbutton aus layout holen
		this.connect_disconnect_bttn = (ToggleButton)findViewById(R.id.button_cnct_disco);
		// nextmsg button aus layout holen
		this.nextmsg_bttn = (Button)findViewById(R.id.next_msg);
		this.nextmsg_bttn.setEnabled(false);
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	
	/** Nachricht aus Textview Kopieren (zum Clippboard) */
	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void copyMessageToClipboard(View view) {
		TextView rcvdMsg = (TextView) findViewById(R.id.rcvd_message);
		CharSequence textcopy = rcvdMsg.getText();
		
		// je nach android version verschiedene clipboards
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(textcopy);
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData
					.newPlainText("Copied Text", textcopy);
			clipboard.setPrimaryClip(clip);
		}
	}
	
	
	/** 
	 * holt und zeigt die naechtse nachricht an die in der queue des
	 * clients ist.
	 * @param view button der methode aufgerufen hat. (unverwendet)
	 */
	public void showNextMessage(View view) {
		if(client != null){
			String msg = client.getNextMessage();
			if(msg != null){
				textview.setText(msg);
				// wenn keine nachrichten mehr in der queue, bttn deaktivieren
				if(client.countMessages() == 0){
					nextmsg_bttn.setEnabled(false);
				}
			}
		}
	}
	
	
	/**
	 * Stellt verbindung mit server her / trennt verbindung.
	 * Aendert Status des togglebuttons (an / aus)
	 * @param view button der methode aufgerufen hat (unverwendet)
	 */
	public void connect_disconnect(View view) {
		// falls nicht verbunden (client == null)
		if(client == null){
			client = new MessageReceivingClient();
			addListenersToClient();
			// versuche client zu verbinden
			client.attemptConnection(currentConnectionSetting.getHostName(), currentConnectionSetting.getHostPort());
			// wenn erfolgreich:
			if(client.isConnected){
				client.start();
				// button auf on setzen
				connect_disconnect_bttn.setChecked(true);
			} else {
				connect_disconnect_bttn.setChecked(false);
			}
		// falls verbunden (client != null)
		} else {
			// breche verbindung ab
			client.closeConnection();
			try {
				// warte bis clientthread beendet ist
				client.join();
			} catch (InterruptedException e) {
			}
			client = null;
		}
	}
	
	
	/** 
	 * fuegt listener zu client hinzu. <br>
	 * listener der auf verbindungsabbruch hoert <br>
	 * und listener der auf neue nachrichten hoert <p>
	 * 
	 * (wird aufgerufen wenn neuer client erstellt wird)
	 */
	private void addListenersToClient() {
		if (client != null) {
			// listener der button ausschaltet wenn verbundungsabbruch
			client.addConnectionListener(new LostConnectionListener() {
				@Override
				public void onLostConnection() {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							connect_disconnect_bttn.setChecked(false);
							nextmsg_bttn.setEnabled(false);
						}
					});
				}
			});
			// listener der den nextmsg button aktiviert bei neuer nachricht
			client.addNewMessageListener(new NewMessageListener() {
				@Override
				public void onMessageReceived(int numberOfMessages){
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							nextmsg_bttn.setEnabled(true);
						}
					});
				}
			});
		}
		
	}
	
	
	@Override
	protected void onDestroy() {
		// falls verbindung besteht, verbindung trennen
		if(client != null){
			this.client.closeConnection();
		}
		super.onDestroy();
	}
	
//TODO: einstellungen bei optionen aufrufen
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//		case R.id.menu_settings:
//			// open settings
//			break;
//		default:
//			break;
//		}
//		
//		return true;
//	}

}
