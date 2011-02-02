/*
 *   Copyright (c) 2010 STRUMSOFT (http://www.strumsoft.com)
 *  
 *   Permission is hereby granted, free of charge, to any person
 *   obtaining a copy of this software and associated documentation
 *   files (the "Software"), to deal in the Software without
 *   restriction, including without limitation the rights to use,
 *   copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the
 *   Software is furnished to do so, subject to the following
 *   conditions:
 *  
 *   The above copyright notice and this permission notice shall be
 *   included in all copies or substantial portions of the Software.
 *  
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *   OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *   HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *   WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *   FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *   OTHER DEALINGS IN THE SOFTWARE.
 *  
 */
package com.strumsoft.websocket;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import com.strumsoft.websocket.phonegap.WebSocket;

/**
 * Originally written by <a href="https://github.com/TooTallNate"/>Nathan Rajlich</a>
 * 
 * <tt>WebSocketListener</tt> is an abstract class that expects a valid
 * "ws://" URI to connect to, and after establishing the connection, it recieves and
 * subsequently calls methods related to the life of the connection. A subclass must 
 * implement <var>onOpen</var>, <var>onClose</var>, and <var>onMessage</var> to be
 * useful. 
 * 
 * @author Animesh Kumar (animesh@strumsoft.com)
 */
public abstract class WebSocketListener implements Runnable {

	// INSTANCE PROPERTIES /////////////////////////////////////////////////////
	/**
	 * The URI this client is supposed to connect to.
	 */
	private URI uri ;
	/**
	 * The WebSocket instance this client object wraps.
	 */
	private WebSocketProtocol conn;
	/**
	 * The SocketChannel instance this client uses.
	 */
	private SocketChannel client;
	/**
	 * The 'Selector' used to get event keys from the underlying socket.
	 */
	private Selector selector;
	/**
	 * Keeps track of whether or not the client thread should continue running.
	 */
	private boolean running;
	
	/** Should try to reconnect?. */
	private int attempts;
	
	/** How long should it wait before it should reconnect?. */
	private int reConnectWaitDuration = 5000; // 5 seconds
	/**
	 * The Draft of the WebSocket protocol the Client is adhering to.
	 */
	private WebSocketProtocol.Draft draft;
	
	/** Number 1 used in handshake. */
	private int number1 = 0;
	
	/** Number 2 used in handshake. */
	private int number2 = 0;
	
	/** Key3 used in handshake. */
	private byte[] key3 = null;

	/**
	 * Instantiates a new web socket listener.
	 *
	 * @param uri the uri
	 */
	public WebSocketListener(URI uri) {
		this(uri, WebSocketProtocol.Draft.DRAFT75, 2);
	}

	/**
	 * Constructs a WebSocketClient instance and sets it to the connect to the
	 * specified URI. The client does not attampt to connect automatically. You
	 * must call <var>connect</var> first to initiate the socket connection.
	 *
	 * @param uri the uri
	 * @param draft the draft
	 * @param attempts the attempts
	 */
	public WebSocketListener(URI uri, WebSocketProtocol.Draft draft, int attempts) {
		this.uri = uri;

		if (draft == WebSocketProtocol.Draft.AUTO) {
			throw new IllegalArgumentException(draft
					+ " is meant for `WebSocketServer` only!");
		}

		this.draft = draft;
		this.attempts = attempts;
	}

	// PUBLIC INSTANCE METHODS /////////////////////////////////////////////////
	/**
	 * Gets the URI that this WebSocketClient is connected to.
	 *
	 * @return The  for this WebSocketClient.
	 */
	public URI getURI() {
		return uri;
	}

	/**
	 * Gets the draft.
	 *
	 * @return the draft
	 */
	public WebSocketProtocol.Draft getDraft() {
		return this.draft;
	}

	/**
	 * Starts a background thread that attempts and maintains a WebSocket
	 * connection to the URI specified in the constructor or via
	 * <var>setURI</var>. <var>setURI</var>.
	 */
	public void connect() {
		this.running = true;
		(new Thread(this)).start();
	}

	/**
	 * Calls <var>close</var> on the underlying SocketChannel, which in turn
	 * closes the socket connection, and ends the client socket thread.
	 * 
	 * @throws IOException
	 *             When socket related I/O errors occur.
	 */
	public void close() throws IOException {
		this.running = false;
		selector.wakeup();
		conn.close();
	}

	/**
	 * Sends <var>text</var> to the connected WebSocket server.
	 * 
	 * @param text
	 *            The String to send to the WebSocket server.
	 * @throws IOException
	 *             When socket related I/O errors occur.
	 */
	public void send(String text) throws IOException {
		conn.send(text);
	}

	// Runnable IMPLEMENTATION /////////////////////////////////////////////////
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		int count = 0;
		do {
			try {
				_connect();
			} catch (IOException e) {
				e.printStackTrace();

				// check for re-attempts
				if (++count >= attempts) {
					return;
				}

				// put to sleep for some time
				try {
					Thread.sleep(reConnectWaitDuration);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		} while (true);
	}

	/**
	 * _connect.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void _connect() throws IOException {
		int port = uri.getPort();
		if (port == -1) {
			port = WebSocketProtocol.DEFAULT_PORT;
		}

		// The WebSocket constructor expects a SocketChannel that is
		// non-blocking, and has a Selector attached to it.

		client = SocketChannel.open();
		client.configureBlocking(false);
		client.connect(new InetSocketAddress(uri.getHost(), port));
		
		// More info: http://groups.google.com/group/android-developers/browse_thread/thread/45a8b53e9bf60d82
		// http://stackoverflow.com/questions/2879455/android-2-2-and-bad-address-family-on-socket-connect
		
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.net.preferIPv6Addresses", "false");
		
		selector = Selector.open();

		this.conn = new WebSocketProtocol(client,
				new LinkedBlockingQueue<ByteBuffer>(), this);
		
		// At first, we're only interested in the 'CONNECT' keys.
		client.register(selector, SelectionKey.OP_CONNECT);

		// Continuous loop that is only supposed to end when "close" is called.
		while (this.running) {
			selector.select();
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> i = keys.iterator();

			while (i.hasNext()) {
				SelectionKey key = i.next();
				i.remove();

				// When 'conn' has connected to the host
				if (key.isConnectable()) {

					// Ensure connection is finished
					if (client.isConnectionPending()) {
						client.finishConnect();
					}

					// Now that we're connected, re-register for only 'READ'
					// keys.
					client.register(selector, SelectionKey.OP_READ);

					// Now send the WebSocket client-side handshake
					String path = uri.getPath();
					if (path.indexOf("/") != 0) {
						path = "/" + path;
					}
					String host = uri.getHost()
							+ (port != WebSocketProtocol.DEFAULT_PORT ? ":" + port : "");
					String origin = "*"; // TODO: Make 'origin' configurable
					String request = "GET " + path + " HTTP/1.1\r\n"
							+ "Upgrade: WebSocket\r\n"
							+ "Connection: Upgrade\r\n" + "Host: " + host
							+ "\r\n" + "Origin: " + origin + "\r\n";
					if (this.draft == WebSocketProtocol.Draft.DRAFT76) {
						request += "Sec-WebSocket-Key1: " + this.generateKey()
								+ "\r\n";
						request += "Sec-WebSocket-Key2: " + this.generateKey()
								+ "\r\n";
						this.key3 = new byte[8];
						(new Random()).nextBytes(this.key3);
					}
					// extraHeaders.toString() +
					request += "\r\n";
					conn.socketChannel().write(
							ByteBuffer.wrap(request
									.getBytes(WebSocketProtocol.UTF8_CHARSET)));
					if (this.key3 != null) {
						conn.socketChannel().write(ByteBuffer.wrap(this.key3));
					}
				}
				//Utils.printString("key.isReadable() : "+key.isReadable(), Utils.PRINT_IMPORTANT);
				// When 'conn' has recieved some data
				if (key.isReadable()) {
					try {
						//****************
						conn.handleRead();
						//************************
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Generate key.
	 *
	 * @return the string
	 */
	private String generateKey() {
		Random r = new Random();
		long maxNumber = 4294967295L;
		long spaces = r.nextInt(12) + 1;
		int max = new Long(maxNumber / spaces).intValue();
		max = Math.abs(max);
		int number = r.nextInt(max) + 1;
		if (this.number1 == 0) {
			this.number1 = number;
		} else {
			this.number2 = number;
		}
		long product = number * spaces;
		String key = Long.toString(product);
		int numChars = r.nextInt(12);
		for (int i = 0; i < numChars; i++) {
			int position = r.nextInt(key.length());
			position = Math.abs(position);
			char randChar = (char) (r.nextInt(95) + 33);
			// exclude numbers here
			if (randChar >= 48 && randChar <= 57) {
				randChar -= 15;
			}
			key = new StringBuilder(key).insert(position, randChar).toString();
		}
		for (int i = 0; i < spaces; i++) {
			int position = r.nextInt(key.length() - 1) + 1;
			position = Math.abs(position);
			key = new StringBuilder(key).insert(position, "\u0020").toString();
		}
		return key;
	}

	// WebSocketListener IMPLEMENTATION ////////////////////////////////////////
	/**
	 * Parses the server's handshake to verify that it's a valid WebSocket
	 * handshake.
	 *
	 * @param conn The {@link WebSocket} instance who's handshake has been
	 * recieved. In the case of <tt>WebSocketClient</tt>, this.conn
	 * == conn.
	 * @param handshake The entire UTF-8 decoded handshake from the connection.
	 * @param reply the reply
	 * @return  is a valid WebSocket
	 * server handshake,  otherwise.
	 * @throws IOException When socket related I/O errors occur.
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 */
	public boolean onHandshakeRecieved(WebSocketProtocol conn, String handshake,
			byte[] reply) throws IOException, NoSuchAlgorithmException {
		// TODO: Do some parsing of the returned handshake, and close connection
		// (return false) if we recieved anything unexpected.
		if (this.draft == WebSocketProtocol.Draft.DRAFT76) {
			if (reply == null) {
				return false;
			}
			byte[] challenge = new byte[] { (byte) (this.number1 >> 24),
					(byte) ((this.number1 << 8) >> 24),
					(byte) ((this.number1 << 16) >> 24),
					(byte) ((this.number1 << 24) >> 24),
					(byte) (this.number2 >> 24),
					(byte) ((this.number2 << 8) >> 24),
					(byte) ((this.number2 << 16) >> 24),
					(byte) ((this.number2 << 24) >> 24), this.key3[0],
					this.key3[1], this.key3[2], this.key3[3], this.key3[4],
					this.key3[5], this.key3[6], this.key3[7] };
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] expected = md5.digest(challenge);
			for (int i = 0; i < reply.length; i++) {
				if (expected[i] != reply[i]) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Calls subclass' implementation of <var>onMessage</var>.
	 *
	 * @param conn the conn
	 * @param message the message
	 */
	public void onMessage(WebSocketProtocol conn, String message) {
		onMessage(message);
	}

	/**
	 * Calls subclass' implementation of <var>onOpen</var>.
	 *
	 * @param conn the conn
	 */
	public void onOpen(WebSocketProtocol conn) {
		onOpen();
	}

	/**
	 * Calls subclass' implementation of <var>onClose</var>.
	 *
	 * @param conn the conn
	 */
	public void onClose(WebSocketProtocol conn) {
		onClose();
	}

	/**
	 * On reconnect.
	 *
	 * @param conn the conn
	 */
	public void onReconnect(WebSocketProtocol conn) {
		onReconnect();
	}
	// ABTRACT METHODS /////////////////////////////////////////////////////////
	/**
	 * On message.
	 *
	 * @param message the message
	 */
	public abstract void onMessage(String message);

	/**
	 * On open.
	 */
	public abstract void onOpen();

	/**
	 * On close.
	 */
	public abstract void onClose();
	
	/**
	 * On reconnect.
	 */
	public abstract void onReconnect();
}
