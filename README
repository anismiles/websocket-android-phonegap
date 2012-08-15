Note: this plugin will be added to Phonegap-android core very soon. :) Here is the forked project: https://github.com/anismiles/phonegap-android


Websocket Android Plugin with Phonegap integration
--------------------------------------------------

This is a Java library that implements Websockt API (Draft-75/76) for Android platform. Library uses java.nio.* packages for
efficient non-blocking evented behavior. It easily gets integrated with Phonegap framework too.

Usage (native Android)
----------------------

1. Copy Java source into your source folder.
2. Create a class, say WebSocketListener extending WebSocket (refer: com.strumsoft.websocket.WebSocket)
3. Implement following methods
	1. onOpen
	2. onClose
	3. onMessage
	4. onReconnect (WebSocketListener will try to reconnect to the server in case of connection failure)
	 
Usage (Phonegap)
----------------

1. Copy Java source into your source folder.
2. Copy websocket.js in your assets/www/js folder
3. Attach com.strumsoft.websocket.phonegap.WebSocketFactory to WebView, like this:

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.loadUrl(" file:///android_asset/www/index.html ");

		// attach websocket factory
		appView.addJavascriptInterface(new WebSocketFactory(appView), "WebSocketFactory");
	}

4. In your page, create a new WebSocket, and overload its method 'onmessage', 'onopen', 'onclose', like this:

	// new socket
	var socket = new WebSocket('ws://192.168.1.153:8081');
	 
	// push a message after the connection is established.
	socket.onopen = function() {
	 socket.send('{ "type": "join", "game_id": "game/6"}')
	};

	// alerts message pushed from server
	socket.onmessage = function(msg) {
	 alert(JSON.stringify(msg.data));
	};

	// alert close event
	socket.onclose = function() {
	 alert('closed');
	};


ps: It doesn't support 'onerror' event, and various states as defined by WebSocket APIs yet. I am working on it. By the way, if you like the project, join the force. 