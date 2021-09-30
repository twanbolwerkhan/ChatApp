package main.java.client;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import main.java.client.component.*;
import main.java.client.component.Authentication.ClientPasswordAuthenticator;
import main.java.client.component.Authentication.IAuthenticationInput;
import main.java.client.component.Authentication.IClientAuthenticator;
import main.java.client.component.Authentication.PasswordInput;
import main.java.spl.Logger;

import java.net.*;

public class Client implements Runnable {
	// Connect status constants
	public final static int NULL = 0;
	public final static int DISCONNECTED = 1;
	public final static int DISCONNECTING = 2;
	public final static int BEGIN_CONNECT = 3;
	public final static int CONNECTED = 4;

	// Other constants
	public final static String statusMessages[] = { " Error! Could not connect!", " Disconnected", " Disconnecting...",
			" Begin Connecting...", " Connected" };
	public static Client tcpObj;
	public final static String END_CHAT_SESSION = "QUIT"; // Indicates the end of a session

	// Connection state info
	public static String hostIP = "localhost";
	public static int port = 1234;
	public static int connectionStatus = DISCONNECTED;
	public static String statusString = statusMessages[connectionStatus];
	public static StringBuffer toAppend = new StringBuffer("");
	public static StringBuffer toSend = null;
	public static String serverSecret = "";

	// Various GUI components and info
	public static JPanel optionsPane = null;
	public static JFrame mainFrame = null;
	public static JPanel statusBar = null;
	public static JLabel statusField = null;
	public static JTextField statusColor = null;
	public static JTextField ipField = null;
	public static JTextField portField = null;
	
	public static JButton connectButton = null;
	public static JButton disconnectButton = null;

	// TCP Components
	public static ServerSocket hostServer = null;
	public static Socket socket = null;
	public static BufferedReader in = null;
	public static PrintWriter out = null;

	private static Encrypter encrypter = new Encrypter();
	
	private static IChat chatUI = null;

	// Constructor /////////////////////////////////////////////////////
	
	private static IAuthenticationInput authenticationInput;
	IClientAuthenticator clientAuthenticator;
	private static IColor colorSelection;
	IEncrypter encryptor;
	ILogger logger; 
	
	public Client(
			IAuthenticationInput authenticationInput, 
			IClientAuthenticator clientAuthenticator, 
			IColor colorSelection,
			IEncrypter encryptor, 
			ILogger logger, 
			IChat chat
		) {
		Client.authenticationInput = authenticationInput;
		this.clientAuthenticator = clientAuthenticator;
		Client.colorSelection = colorSelection;
		this.encryptor = encryptor;
		this.logger = logger;
		Client.chatUI = chat;
		tcpObj = this;
	}
	/////////////////////////////////////////////////////////////////

	private static JPanel initOptionsPane() {
		JPanel pane = null;
		ActionAdapter buttonListener = null;
		int gridSize = 6;

		// Create an options pane
		optionsPane = new JPanel(new GridLayout(gridSize, 1));

		// IP address input
		pane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pane.add(new JLabel("Host IP:"));
		ipField = new JTextField(10);
		ipField.setText(hostIP);
		ipField.setEnabled(false);
		ipField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				ipField.selectAll();
				// Should be editable only when disconnected
				if (connectionStatus != DISCONNECTED) {
					changeStatusNTS(NULL, true);
				} else {
					hostIP = ipField.getText();
				}
			}
		});
		pane.add(ipField);
		optionsPane.add(pane);

		// Port input
		pane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pane.add(new JLabel("Port:"));
		portField = new JTextField(10);
		portField.setEditable(true);
		portField.setText((new Integer(port)).toString());
		portField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				// should be editable only when disconnected
				if (connectionStatus != DISCONNECTED) {
					changeStatusNTS(NULL, true);
				} else {
					int temp;
					try {
						temp = Integer.parseInt(portField.getText());
						port = temp;
					} catch (NumberFormatException nfe) {
						portField.setText((new Integer(port)).toString());
						mainFrame.repaint();
					}
				}
			}
		});
		pane.add(portField);
		optionsPane.add(pane);

		// Color input
		pane = colorSelection.createGuiComponent(tcpObj);
		optionsPane.add(pane);

		// Password input
		pane = authenticationInput.createGuiComponent(tcpObj);
		optionsPane.add(pane);

		// Host/guest option
		buttonListener = new ActionAdapter() {
			public void actionPerformed(ActionEvent e) {
				if (connectionStatus != DISCONNECTED) {
					changeStatusNTS(NULL, true);
				} else {
					ipField.setEnabled(true);
				}
			}
		};

		// Connect/disconnect buttons
		JPanel buttonPane = new JPanel(new GridLayout(1, 2));
		buttonListener = new ActionAdapter() {
			public void actionPerformed(ActionEvent e) {
				// Request a connection initiation
				if (e.getActionCommand().equals("connect")) {
					changeStatusNTS(BEGIN_CONNECT, true);
				}
				// Disconnect
				else {
					changeStatusNTS(DISCONNECTING, true);
				}
			}
		};
		connectButton = new JButton("Connect");
		connectButton.setMnemonic(KeyEvent.VK_C);
		connectButton.setActionCommand("connect");
		connectButton.addActionListener(buttonListener);
		connectButton.setEnabled(true);
		disconnectButton = new JButton("Disconnect");
		disconnectButton.setMnemonic(KeyEvent.VK_D);
		disconnectButton.setActionCommand("disconnect");
		disconnectButton.addActionListener(buttonListener);
		disconnectButton.setEnabled(false);
		buttonPane.add(connectButton);
		buttonPane.add(disconnectButton);
		optionsPane.add(buttonPane);

		return optionsPane;
	}

	/////////////////////////////////////////////////////////////////

	// Initialize all the GUI components and display the frame
	private static void initGUI() {
		// Set up the status bar
		statusField = new JLabel();
		statusField.setText(statusMessages[DISCONNECTED]);
		statusColor = new JTextField(1);
		statusColor.setBackground(Color.red);
		statusColor.setEditable(false);
		statusBar = new JPanel(new BorderLayout());
		statusBar.add(statusColor, BorderLayout.WEST);
		statusBar.add(statusField, BorderLayout.CENTER);

		// Set up the options pane
		JPanel optionsPane = initOptionsPane();

		// Set up the main pane
		JPanel mainPane = new JPanel(new BorderLayout());
		mainPane.add(statusBar, BorderLayout.SOUTH);
		mainPane.add(optionsPane, BorderLayout.WEST);
		
		// Set up the chat pane
		JPanel chatPane = chatUI.createGuiComponent(tcpObj);
		toSend = chatUI.getSendBuffer();
		mainPane.add(chatPane, BorderLayout.CENTER);
		
		// Set up the main frame
		mainFrame = new JFrame("Simple TCP Chat");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setContentPane(mainPane);
		mainFrame.setSize(mainFrame.getPreferredSize());
		mainFrame.setLocation(200, 200);
		mainFrame.pack();
		mainFrame.setVisible(true);
	}

	/////////////////////////////////////////////////////////////////

	// The thread-safe way to change the GUI components while
	// changing state
	private static void changeStatusTS(int newConnectStatus, boolean noError) {
		// Change state if valid state
		if (newConnectStatus != NULL) {
			connectionStatus = newConnectStatus;
		}

		// If there is no error, display the appropriate status message
		if (noError) {
			statusString = statusMessages[connectionStatus];
		}
		// Otherwise, display error message
		else {
			statusString = statusMessages[NULL];
		}

		// Call the run() routine (Runnable interface) on the
		// error-handling and GUI-update thread
		SwingUtilities.invokeLater(tcpObj);
	}

	/////////////////////////////////////////////////////////////////

	// The non-thread-safe way to change the GUI components while
	// changing state
	private static void changeStatusNTS(int newConnectStatus, boolean noError) {
		// Change state if valid state
		if (newConnectStatus != NULL) {
			connectionStatus = newConnectStatus;
		}

		// If there is no error, display the appropriate status message
		if (noError) {
			statusString = statusMessages[connectionStatus];
		}
		// Otherwise, display error message
		else {
			statusString = statusMessages[NULL];
		}

		// Call the run() routine (Runnable interface) on the
		// current thread
		tcpObj.run();
	}

	/////////////////////////////////////////////////////////////////

	// Thread-safe way to append to the chat box
	private static void appendToChatBox(String s) {
		synchronized (toAppend) {
			toAppend.append(s);
		}
	}

	/////////////////////////////////////////////////////////////////

	// Add text to send-buffer
	public static void sendString(String s) {
		synchronized (toSend) {
			toSend.append(s + "\n");
		}
	}

	/////////////////////////////////////////////////////////////////

	// Cleanup for disconnect
	private static void cleanUp() {
		try {
			if (hostServer != null) {
				hostServer.close();
				hostServer = null;
			}
		} catch (IOException e) {
			hostServer = null;
		}

		try {
			if (socket != null) {
				socket.close();
				socket = null;
			}
		} catch (IOException e) {
			socket = null;
		}

		try {
			if (in != null) {
				in.close();
				in = null;
			}
		} catch (IOException e) {
			in = null;
		}

		if (out != null) {
			out.close();
			out = null;
		}
	}

	/////////////////////////////////////////////////////////////////

	// Checks the current state and sets the enables/disables
	// accordingly
	public void run() {
		switch (connectionStatus) {
		case DISCONNECTED:
			connectButton.setEnabled(true);
			disconnectButton.setEnabled(false);
			authenticationInput.setEnabled(true);
			ipField.setEnabled(true);
			portField.setEnabled(true);
			chatUI.onDisconnected();
			statusColor.setBackground(Color.red);
			break;
		case DISCONNECTING:
			connectButton.setEnabled(false);
			disconnectButton.setEnabled(false);
			authenticationInput.setEnabled(false);
			ipField.setEnabled(false);
			portField.setEnabled(false);
			chatUI.onDisconnecting();
			statusColor.setBackground(Color.orange);
			break;
		case CONNECTED:
			connectButton.setEnabled(false);
			disconnectButton.setEnabled(true);
			authenticationInput.setEnabled(true);
			ipField.setEnabled(false);
			portField.setEnabled(false);
			chatUI.onConnected();
			statusColor.setBackground(Color.green);
			break;
		case BEGIN_CONNECT:
			connectButton.setEnabled(false);
			disconnectButton.setEnabled(false);
			authenticationInput.setEnabled(false);
			ipField.setEnabled(false);
			portField.setEnabled(false);
			chatUI.onConnecting();
			statusColor.setBackground(Color.orange);
			break;
		}

		// Make sure that the button/text field states are consistent
		// with the internal states
		ipField.setText(hostIP);
		portField.setText((new Integer(port)).toString());
		statusField.setText(statusString);
		chatUI.append(toAppend.toString());
		toAppend.setLength(0);

		mainFrame.repaint();
	}

	/////////////////////////////////////////////////////////////////

	// The main procedure
	public void start() {
		String s;

		initGUI();

		while (true) {
			try { // Poll every ~10 ms
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}

			switch (connectionStatus) {
			case BEGIN_CONNECT:
				try {
					// Try to connect to the server
					socket = new Socket(hostIP, port);
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					out = new PrintWriter(socket.getOutputStream(), true);
					String password = authenticationInput.getPassword();
					if (clientAuthenticator.authenticate(out, in, password)) {
						changeStatusNTS(CONNECTED, true);
					} else {
						cleanUp();
						changeStatusTS(DISCONNECTED, false);
					}
				}
				// If error, clean up and output an error message
				catch (Exception e) {
					cleanUp();
					changeStatusTS(DISCONNECTED, false);
				}
				break;
			case CONNECTED:
				try {
					// Send data
					if (toSend.length() != 0) {
						out.print(toSend);
						out.flush();
						toSend.setLength(0);
						changeStatusTS(NULL, true);
					}

					// Receive data
					if (in.ready()) {
						s = in.readLine();
						if ((s != null) && (s.length() != 0)) {
							// Check if it is the end of a transmission
							if (s.equals(END_CHAT_SESSION)) {
								changeStatusTS(DISCONNECTING, true);
							}

							// Otherwise, receive what text
							else {
								logger.log("client_log.txt", s);
								String content = s;
								content = encrypter.encrypt(s);
								appendToChatBox(content + "\n");
								changeStatusTS(NULL, true);
							}
						}
					}
				} catch (IOException e) {
					cleanUp();
					changeStatusTS(DISCONNECTED, false);
				}
				break;

			case DISCONNECTING:
				// Tell other chatter to disconnect as well
				out.print(END_CHAT_SESSION);
				out.flush();

				// Clean up (close all streams/sockets)
				cleanUp();
				changeStatusTS(DISCONNECTED, true);
				break;

			default:
				break; // do nothing
			}
		}
	}
}
