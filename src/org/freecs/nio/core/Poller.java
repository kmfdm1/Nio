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
 * Poller opens a selector and delegates the ready SelectionKey's to the 
 * IOHandler registered with the corresponding SelectionKey
 */
package org.freecs.nio.core;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.freecs.nio.interfaces.IOHandler;
import org.freecs.nio.interfaces.IPoller;

public class Poller implements Runnable, IPoller {
    private boolean running;
    private final Selector sel;
    private Thread runner;
    private int priority = 5;
    private List<IOHandler> regListIoh = new ArrayList<IOHandler>();
    private List<SocketChannel> regListSc = new ArrayList<SocketChannel>(); 
    
    public Poller () throws IOException {
        sel = SelectorProvider.provider().openSelector();
    }

    /**
     * Add an Listening-IOHandler for the given interestOp and the given InetSocketAddress.
     * @param ioh the io-handler responsible for managing events described by interstOp
     * @param isa the InetSocketAddress of the port we are interested in
     * @throws IOException
     */
    public void addListeningHandler(IOHandler ioh, ServerSocketChannel ssc) throws IOException {
        ssc.register(sel, SelectionKey.OP_ACCEPT, ioh);
        priority = 8;
    }
    
    /**
     * Add an IOHandler for the given interestOp and the given SocketChannel.
     * @param ioh the io-handler responsible for managing events described by interstOp
     * @param sc the SocketChannel we are interested in doing operations on
     * @throws IOException
     */
    public void addHandler(IOHandler ioh, SocketChannel sc) throws IOException {
        sc.configureBlocking(false);
        synchronized(regListIoh) {
            regListIoh.add(ioh);
            regListSc.add(sc);
        }
    }

    public static final IOHandler[] ioArr = new IOHandler[0];
    public static final SocketChannel[] scArr = new SocketChannel[0];

    public void run() {
        startup();
        while (this.isRunning()) {
            if (regListIoh.size()>0) {
                IOHandler[] iohs;
                SocketChannel[] scs;
                synchronized(regListIoh) {
                    iohs = (IOHandler[]) regListIoh.toArray(ioArr);
                    scs = (SocketChannel[]) regListSc.toArray(scArr);
                    regListIoh.clear();
                    regListSc.clear();
                }
                for (int i = 0; i < iohs.length; i++) {
                    try {
                        SelectionKey sk = scs[i].register(sel, iohs[i].getInterestSet(), iohs[i]);
                        iohs[i].setSelectionKey(sk);
                        if ((iohs[i].getInterestSet() & SelectionKey.OP_CONNECT) != 0 && scs[i].isConnected()) {
                            iohs[i].connect();
                        }
                    } catch (ClosedChannelException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                if (sel.select(33)<1) {
                    continue;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                this.shutdown();
                break;
            }
            for (Iterator<SelectionKey> i = sel.selectedKeys().iterator(); i.hasNext(); ) { 
                SelectionKey sk = i.next();
                IOHandler ioh = (IOHandler) sk.attachment();
                try {
                    if (!sk.isValid()) {
                        ioh.cleanup();
                        continue;
                    }
                    if (sk.isAcceptable()) {
                        ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
                        SocketChannel sc = ssc.accept();
                        if (sc!=null) {
                            sc.configureBlocking(false);
                            ioh.accept(sc);
                        }
                        continue;
                    }
                    if (sk.isConnectable()) {
                        ioh.connect();
                        continue;
                    }
                    if (sk.isReadable()) {
                        ioh.read();
                    }
                    if (sk.isWritable()) {
                        ioh.write();
                    }
                } catch (IOException ioe) {
                    ioh.cleanup();
                } catch (CancelledKeyException cke) {
                    // happens... remotely or locally closed connection for example
                    ioh.cleanup();
                } finally {
                    i.remove();
                }
            }
        }
        for (Iterator<SelectionKey> i = sel.keys().iterator(); i.hasNext(); ) {
            SelectionKey sk = i.next();
            try {
                sk.channel().close();
            } catch (IOException e) {
                // ignore.. we are shutting down anyways
            }
        }
        try {
            sel.close();
        } catch (IOException e) {
            // ignore.. we are shutting down anyways
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void shutdown() {
        running = false;
    }
    
    public void startup() {
        running = true;
    }

    /**
     * Start up this poller
     */
    public void startPoller() {
        if (runner!=null && runner.isAlive())
            return;
        runner = new Thread(this);
        runner.setPriority(priority);
        runner.setName("PollerRunner " + priority + " / " + this.toString());
        runner.start();
    }
}