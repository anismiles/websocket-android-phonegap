package com.strumsoft.websocket;

import java.net.URI;
import java.net.URISyntaxException;

import com.strumsoft.util.Logger;

/**
 * Sample WebSocket implementation.
 * 
 * WebSocket socket = new WebSocket('ws://ip:port');
 * socket.connect();
 * 
 * @author Animesh Kumar
 *
 */
public class WebSocket extends WebSocketListener {

	public WebSocket(URI uri) throws URISyntaxException {
		super(uri);
	}
	
	@Override
	public void onClose() {
		Logger.log("Connection closed!");
	}

	@Override
	public void onMessage(String message) {
		Logger.log("Message from server: " + message);
	}

	@Override
	public void onOpen() {
		Logger.log("Connection opened!");
	}

	@Override
	public void onReconnect() {
		Logger.log("Reconnecing...");
	}
}
