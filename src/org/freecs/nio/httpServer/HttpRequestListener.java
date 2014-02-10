/*
 * Copyright 2014 Manfred Andres
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * HttpRequestListener is responsible for registering SocketChannels by wrapping them inside of a 
 * HttpConnctionHandler and registering it with a IPoller
 */
package org.freecs.nio.httpServer;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.freecs.nio.interfaces.IOHandler;
import org.freecs.nio.interfaces.IPoller;

public class HttpRequestListener implements IOHandler {
    private final IPoller poller;
    private SelectionKey sk = null;
    
    @SuppressWarnings("unused")
    private HttpRequestListener() { poller=null; }
    
    public HttpRequestListener(IPoller poller) {
        this.poller=poller;
    }

    /**
     * Wrap the given SocketChannel within a HttpConnectionHandler and
     * add this handler to the IPoller
     */
    public void accept(SocketChannel sc) {
        HttpConnectionHandler hch = new HttpConnectionHandler(10240);
        try {
            poller.addHandler(hch, sc);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void connect() { return; } // connect is only used by clients wanting to connect to a server
    public void read() { return; } // listeners don't read
    public void write() { return; } // listeners don't write

    /**
     * Cancel the SelecitonKey, and close the Channel
     */
    public void cleanup() {
        sk.cancel();
        try {
            sk.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     * Sets the selectionkey associated with this IOHandler
     */
    public void setSelectionKey(SelectionKey sk) {
        this.sk = sk;
    }

    /**
     * Returns the interestOps this IOHandler is interested in
     */
    public int getInterestSet() {
        return SelectionKey.OP_ACCEPT;
    }
}
