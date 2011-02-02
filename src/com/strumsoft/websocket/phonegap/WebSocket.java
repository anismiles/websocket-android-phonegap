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
package com.strumsoft.websocket.phonegap;

import java.net.URI;
import java.net.URISyntaxException;

import com.strumsoft.util.Logger;
import com.strumsoft.websocket.WebSocketListener;

import android.webkit.WebView;

/**
 * The Class WebSocket.
 *
 * @author Animesh Kumar
 */
public class WebSocket extends WebSocketListener {

	/** The app view. */
	private WebView appView;

	/** The web socket id. */
	private String webSocketId;

	/**
	 * Deafualt method which invokes super class method.
	 * 
	 * Note: constructor is protected because WebSocketFactory is supposed 
	 * to generate instances.
	 *
	 * @param appView the app view
	 * @param uri the uri
	 * @throws URISyntaxException the uRI syntax exception
	 */
	protected WebSocket(WebView appView, URI uri) throws URISyntaxException {
		super(uri);
		this.appView = appView;
		this.webSocketId = getClass().getSimpleName() + "." + hashCode();
	}

	/* (non-Javadoc)
	 * @see com.strumsoft.websocket.WebSocketListener#onClose()
	 */
	@Override
	public void onClose() {
		appView.loadUrl(buildLoadData("close", ""));
	}

	/* (non-Javadoc)
	 * @see com.strumsoft.websocket.WebSocketListener#onReconnect()
	 */
	@Override
	public void onReconnect() {
		// TODO: Phase-II
		//appView.loadUrl(buildLoadData("reconnect", ""));
	}

	/* (non-Javadoc)
	 * @see com.strumsoft.websocket.WebSocketListener#onMessage(java.lang.String)
	 */
	@Override
	public void onMessage(String message) {
		appView.loadUrl(buildLoadData("message", message));
	}

	/* (non-Javadoc)
	 * @see com.strumsoft.websocket.WebSocketListener#onOpen()
	 */
	@Override
	public void onOpen() {
		appView.loadUrl(buildLoadData("open", ""));
	}

	/**
	 * builds data to be pushed to attached WebView so as to be
	 * passed to Javascript.
	 *
	 * @param _event event type (open, close, message)
	 * @param _data 	data
	 * @return the string
	 */
	private String buildLoadData(String _event, String _data) {
		String _d =  "javascript:WebSocket.on" + _event + "(" + 
				"{"
				+ "\"_target\":\"" + webSocketId + "\"," + 
				"\"_data\":'" + _data + "'" + 
				"}" + 
				")";
		Logger.log(_d);
		return _d;
	}

	/**
	 * Gets the web socket id.
	 *
	 * @return the web socket id
	 */
	public String getWebSocketId() {
		return this.webSocketId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appView == null) ? 0 : appView.hashCode());
		return result;
	}
}
