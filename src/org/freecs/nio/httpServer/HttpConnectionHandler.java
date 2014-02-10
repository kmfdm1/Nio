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
 * HttpConnectionHandler handles a connected client's reads/writes/disconnects and registeres
 * with HttpKeepAliveTracker if it is a keep-alive-connection.
 */
package org.freecs.nio.httpServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import org.freecs.nio.interfaces.IOHandler;

public class HttpConnectionHandler implements IOHandler {
    private final ByteBuffer buff;
    private final HttpRequestParser hrp;
    private LinkedList<HttpResponse> responseQueue = new LinkedList<HttpResponse>();
    private SelectionKey sk = null;

    private KeepAliveState kas = null;
    
    @SuppressWarnings("unused")
    private HttpConnectionHandler() { buff=null; hrp=null; }
    
    HttpConnectionHandler (int buffSize) {
        buff = ByteBuffer.allocateDirect(buffSize);
        hrp = new HttpRequestParser(this, buff);
    }

    public void accept(SocketChannel sc) { return; } // nothing to accept (it's an already established connection)

    public void connect() { return; } // connect is only used by clients wanting to connect to a server

    /**
     * Read new Data from the given SelectionKey and delegate the parsing to it's HttpRequestParser and
     * adding the generated HttpResponse to it's responseQueue.
     */
    public void read() {
        if (kas==null) {
            kas = new KeepAliveState(sk);
        }
        kas.updateIfListed();
        try {
            if (((SocketChannel) sk.channel()).read(buff) == -1) {
                this.cleanup();
                return;
            }
            buff.flip();
            HttpRequest req;
            try {
                while ((req = hrp.parseNewData()) != null) {
                    this.addResponse(sk, new HttpResponse(req));
                    if (!req.isHttp11() || !req.isKeepAlive()) {
                        this.addResponse(sk, HttpResponse.CloseConnection);
                    } else if (req.isKeepAlive()) {
                        HttpKeepAliveTracker.instance.add(kas);
                    }
                }
                buff.compact();
            } catch(HttpError he) {
                this.addResponse(sk, new HttpResponse(he.responseCode));
                this.addResponse(sk, HttpResponse.CloseConnection);
            }
        } catch (IOException e) {
            this.cleanup();
        }
    }

    /**
     * Take the next HttpResponse from the stack, write it to the SelectionKey's SocketChannel
     * and remove it from the queue if it has been fully written.
     */
    public void write() {
        try {
            SocketChannel sc = (SocketChannel) sk.channel();
            while (!responseQueue.isEmpty()) {
                HttpResponse hr = responseQueue.getFirst();
                if (hr == HttpResponse.CloseConnection) {
                    this.cleanup();
                    return;
                }
                if (sc.write(hr.buff) < 1) {
                    return;
                }
                responseQueue.removeFirst();
            }
            if (responseQueue.isEmpty()) {
                sk.interestOps(sk.interestOps() - SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            this.cleanup();
        }
    }

    /**
     * Cleanup by canceling the given SelectionKey, deregistering KeepAliveState
     * from HttpKeepAliveTracker if neccesary and trying to close the channel.
     */
    public void cleanup() {
        sk.cancel();
        if (this.kas != null)
            HttpKeepAliveTracker.instance.remove(this.kas);
        try {
            sk.channel().close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Add a new response to this HttpConnectionHandler's responseQueue. If the given
     * SelectionKey doesn't have the OP_WRITE-interest-flag set it will be set.
     * @param sk The SelectionKey to write out the response to
     * @param response  The Response to write out
     */
    public void addResponse(SelectionKey sk, HttpResponse response) {
        responseQueue.add(response);
        int ops = sk.interestOps();
        if ((ops & SelectionKey.OP_WRITE) == 0)
            sk.interestOps(ops | SelectionKey.OP_WRITE);
    }

    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public void setSelectionKey(SelectionKey sk) {
        this.sk = sk;
    }

    @Override
    public int getInterestSet() {
        return SelectionKey.OP_READ;
    }
}