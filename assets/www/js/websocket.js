/*
 * Copyright (c) 2010 Animesh Kumar  (https://github.com/anismiles)
 * Copyright (c) 2010 Strumsoft  (https://strumsoft.com)
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 */

(function() {

	// window object
	var global = window;

	if (typeof global.WebSocket === 'function') {
		return;
	}

	// WebSocket Object. All listener methods are cleaned up!
	var WebSocket = global.WebSocket = function(url) {
		// get a new websocket object from factory (check com.strumsoft.websocket.WebSocketFactory.java)
		this.socket = WebSocketFactory.getInstance(url);
		// store in registry
		if(this.socket) {
			WebSocket.store[this.socket.getId()] = this;
		} else {
			throw new Error('Websocket instantiation failed! Address might be wrong.');
		}
	};

	Object.defineProperty(
		WebSocket.prototype,
		'readyState',
		{
			get: function () { return this.socket.getReadyState(); }
		}
	);

	if (typeof window.atob === 'function') {
		WebSocket._decode = function (data) {
			return window.atob(data)
		};
	} else {
		WebSocket._keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

		// Decode string from base64
		WebSocket._decode = function (input) {
			var output = "";
			var chr1, chr2, chr3;
			var enc1, enc2, enc3, enc4;
			var i = 0;

			input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

			while (i < input.length) {

				enc1 = this._keyStr.indexOf(input.charAt(i++));
				enc2 = this._keyStr.indexOf(input.charAt(i++));
				enc3 = this._keyStr.indexOf(input.charAt(i++));
				enc4 = this._keyStr.indexOf(input.charAt(i++));

				chr1 = (enc1 << 2) | (enc2 >> 4);
				chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
				chr3 = ((enc3 & 3) << 6) | enc4;

				output = output + String.fromCharCode(chr1);

				if (enc3 != 64) {
					output = output + String.fromCharCode(chr2);
				}
				if (enc4 != 64) {
					output = output + String.fromCharCode(chr3);
				}

			}

			output = this._utf8_decode(output);

			return output;
		};

		// private method for UTF-8 decoding
		WebSocket._utf8_decode = function (utftext) {
			var string = "";
			var i = 0;
			var c = c1 = c2 = 0;

			while ( i < utftext.length ) {

				c = utftext.charCodeAt(i);

				if (c < 128) {
					string += String.fromCharCode(c);
					i++;
				}
				else if((c > 191) && (c < 224)) {
					c2 = utftext.charCodeAt(i+1);
					string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
					i += 2;
				}
				else {
					c2 = utftext.charCodeAt(i+1);
					c3 = utftext.charCodeAt(i+2);
					string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
					i += 3;
				}

			}
			return string;
		};
	}

	// storage to hold websocket object for later invokation of event methods
	WebSocket.store = {};

	// static event methods to call event methods on target websocket objects
	WebSocket.onmessage = function (evt) {
		var event = {
			data: this._decode(evt._data),
			type: 'message'
		};

		WebSocket.store[evt._target]['onmessage'].call(global, event);
	};

	WebSocket.onopen = function (evt) {
		WebSocket.store[evt._target]['onopen'].call(global, evt);
	};

	WebSocket.onclose = function (evt) {
		WebSocket.store[evt._target]['onclose'].call(global, evt);
	};

	WebSocket.onerror = function (evt) {
		WebSocket.store[evt._target]['onerror'].call(global, evt);
	};

	// instance event methods
	WebSocket.prototype.send = function(data) {
		this.socket.send(data);
	};

	WebSocket.prototype.close = function() {
		this.socket.close();
	};

	WebSocket.prototype.getReadyState = function() {
		return this.socket.getReadyState();
	};
	///////////// Must be overloaded
	WebSocket.prototype.onopen = function(){
		throw new Error('onopen not implemented.');
  };

  // alerts message pushed from server
  WebSocket.prototype.onmessage = function(msg){
    throw new Error('onmessage not implemented.');
  };

  // alerts message pushed from server
  WebSocket.prototype.onerror = function(msg){
    throw new Error('onerror not implemented.');
  };

  // alert close event
  WebSocket.prototype.onclose = function(){
    throw new Error('onclose not implemented.');
	};
})();