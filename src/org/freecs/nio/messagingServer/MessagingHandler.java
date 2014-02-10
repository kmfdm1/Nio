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
 * MessagingHandler handles a connected client's reads/writes/disconnects
 * It will add messages sent through it to it's sendQueue, write them out
 * when the selector gets notified about write-ability of this SelectionKey's
 * SocketChannel and reads data when selector gets notified about readability
 * of this SelectionKey's SocketChannel.
 */
package org.freecs.nio.messagingServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import org.freecs.nio.interfaces.IOHandler;

public class MessagingHandler implements IOHandler {
    static final Charset characterset = Charset.forName("UTF-8");
    private final ByteBuffer buff;
    private LinkedList<ByteBuffer> sendQueue = new LinkedList<ByteBuffer>();
    private final IMessageReceiver callback;
    private SelectionKey sk = null;
    private int ops;
    private static int nextHashCode=0;
    private final int hashCode;

    @SuppressWarnings("unused")
    private MessagingHandler() { buff=null; callback=null; hashCode = -1; }

    /**
     * Construct the MessagingHandler having a buffer-size of bufferSize
     * calling the callback imr for every fully arrived message and having
     * the interestOps given with ops
     * @param buffSize The buffer-size for reads over this connection
     * @param imr The callback called for every fully arrived message
     * @param ops The interestOps (either OP_CONNECT for pending connections or OP_READ. OP_WRITE will 
     * be set/unset automatically when there is work to do)
     */
    MessagingHandler (int buffSize, IMessageReceiver imr, int ops) {
        buff = ByteBuffer.allocateDirect(buffSize);
        this.callback = imr;
        this.ops = ops;
        synchronized(MessagingHandler.class) {
            hashCode = nextHashCode++;
        }
    }

    public void accept(SocketChannel sc) { return; } // nothing to accept (it's an already established connection)

    /**
     * Finish the connection-process add this IOHandler to MessageListener's recipients-list,
     * unset OP_CONNECT and switch over to OP_READ.
     * Cleanup if any errors occur.
     */
    public void connect() {
        try {
            ((SocketChannel) sk.channel()).finishConnect();
            this.ops = SelectionKey.OP_READ;
            sk.interestOps(this.ops);
            MessagingListener.addRecipient(this);
        } catch (IOException e) {
            this.cleanup();
            e.printStackTrace();
        }
        return;
    }

    /**
     * Read new Data from the given SelectionKey and parse it. If a message arrived fully
     * the callback provided on construction-time will be called with the message
     * as argument.
     */
    public void read() {
        try {
            int bytesRead = ((SocketChannel) sk.channel()).read(buff); 
            if (bytesRead == -1) {
                // reading -1 number of bytes means connection is closed
                this.cleanup();
                return;
            } else if (bytesRead == 0) {
                System.out.print(".");
                return;
            }
            buff.flip();
            while (buff.hasRemaining()) {
                int len = buff.getInt();
                if (buff.remaining() < len) {
                    buff.position(buff.position() - 4);
                    if (buff.position()>0)
                        buff.compact();
                    return;
                }
                byte[] bytes = new byte[len];
                buff.get(bytes);
                String strg = new String(bytes, characterset);
                callback.receive(strg);
            }
            buff.clear();
        } catch (IOException e) {
            this.cleanup();
        }
    }

    /**
     * Take the next message from the sendQueue and send it out.
     * Unset OP_WRITE if there is no more pending data to write out.
     */
    public void write() {
        try {
            SocketChannel sc = (SocketChannel) sk.channel();
            while (true) synchronized(sendQueue) {
                if (sendQueue.isEmpty()) {
                    sk.interestOps(sk.interestOps() - SelectionKey.OP_WRITE);
                    break;
                }
                ByteBuffer msg = sendQueue.getFirst();
                if (sc.write(msg) < 1) {
                    return;
                }
                if (!msg.hasRemaining())
                    sendQueue.removeFirst();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            this.cleanup();
        }
    }

    /**
     * Cleanup by canceling the given SelectionKey and removing this
     * IOHandler from MessagingListener's recipient-list
     */
    public void cleanup() {
        MessagingListener.removeRecipient(this);
        sk.cancel();
        try {
            sk.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a message to one individual peer. This will be called by MessageingListener.sendMessage(String)
     * SelectionKey doesn't have the OP_WRITE-interest-flag set it will get set.
     * @param sk The SelectionKey to write out the response to
     * @param response  The Response to write out
     */
    public void sendMessage(byte[] bytes) {
        if (sk == null)
            return;
        if (!sk.isValid()) {
            this.cleanup();
            return;
        }
        ByteBuffer sbuff = ByteBuffer.allocate(bytes.length + 4);
        sbuff.putInt(bytes.length).put(bytes).flip();
        synchronized(sendQueue) {
            sendQueue.add(sbuff);
        }
        int ops = sk.interestOps();
        if ((ops & SelectionKey.OP_WRITE) == 0) {
            sk.interestOps(ops | SelectionKey.OP_WRITE);
        }
    }

    public int hashCode() {
        return this.hashCode;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MessagingHandler))
            return false;
        MessagingHandler other = (MessagingHandler) obj;
        return this.hashCode == other.hashCode;
    }

    /**
     * Sets this IOHandlers SelectionKey (called by IPoller when registered with it's selector)
     */
    public void setSelectionKey(SelectionKey sk) {
        this.sk = sk;
    }

    /**
     * Return the interestOps this IOHandler is interested in.
     */
    public int getInterestSet() {
        return ops;
    }
}