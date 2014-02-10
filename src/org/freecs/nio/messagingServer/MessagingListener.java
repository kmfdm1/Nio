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
 * MessagingListener accepts connections from other messaging-recipients
 * and adds them to the recipients-list registered with an MessagingHandler
 * responsible for this one connection.
 */
package org.freecs.nio.messagingServer;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

import org.freecs.nio.interfaces.IOHandler;
import org.freecs.nio.interfaces.IPoller;

public class MessagingListener implements IOHandler {
    private final IPoller poller;
    private final IMessageReceiver callback;
    private SelectionKey sk;
    private static final Set<MessagingHandler> recipients = new HashSet<MessagingHandler>();
    
    @SuppressWarnings("unused")
    private MessagingListener() { poller=null; callback=null; }
    
    public MessagingListener(IPoller poller, IMessageReceiver imr) {
        this.poller=poller;
        this.callback = imr;
    }

    /**
     * Wrap the given SocketChannel within a MessagingHandler, add this handler to the IPoller
     * and put this handler into the recipients-list
     */
    public void accept(SocketChannel sc) {
        MessagingHandler mh = new MessagingHandler(10240, callback, SelectionKey.OP_READ);
        try {
            poller.addHandler(mh, sc);
            addRecipient(mh);
            System.out.println("accepted new connection from " + sc.getRemoteAddress());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * adds a recipient to the recipient-list
     * @param mh the recipient to add to the recipient-list
     */
    public static void addRecipient(MessagingHandler mh) {
        System.out.println("adding recipient");
        synchronized (recipients) {
            recipients.add(mh);
        }
    }

    /**
     * Remove a recipient from the recipient-list
     * @param mh the recipient to remove fromt the recipient-list
     */
    public static void removeRecipient(MessagingHandler mh) {
        System.out.println("removing recipient");
        synchronized (recipients) {
            recipients.remove(mh);
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
     * Sets this IOHandlers SelectionKey (called by IPoller when registered with it's selector)
     */
    public void setSelectionKey(SelectionKey sk) {
        this.sk = sk;
    }

    /**
     * Send a message to all known recipients at the time
     * @param strg the message to sent to the recipients on the recipient-list
     */
    public static void sendMessage(String strg) {
        byte[] bytes = strg.getBytes(MessagingHandler.characterset);
        MessagingHandler[] handlers = new MessagingHandler[0];
        synchronized(recipients) {
            handlers = (MessagingHandler[]) recipients.toArray(handlers);
        }
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].sendMessage(bytes);
        }
    }

    /**
     * Returns this IOHandlers interestOps (OP_ACCEPT for the listener)
     */
    public int getInterestSet() {
        return SelectionKey.OP_ACCEPT;
    }
}
