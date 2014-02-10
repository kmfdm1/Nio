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
 * MultithreadedPoller is a multithreaded implementation of a NIO poller
 * It just holds a list of single-threaded pollers and distributes the connections
 * evenly across them by creating one for each listening socket and registering new
 * connections with the next poller in list starting over at the end of the list.
 */
package org.freecs.nio.core;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.freecs.nio.interfaces.IOHandler;
import org.freecs.nio.interfaces.IPoller;

public class MultithreadedPoller implements IPoller {
    private Poller[] pollers;
    private List<IPoller> listeners = new ArrayList<IPoller>();
    private int nextPoller=0;

    /**
     * Construct a number of pollers according to the given argument
     * @param threads the number of pollers to create
     */
    public MultithreadedPoller (int threads) {
        pollers=new Poller[threads];
        for (int i = 0; i<threads; i++)
            try {
                pollers[i] = new Poller();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    /**
     * Add an Listening-IOHandler for the given interestOp and the given InetSocketAddress.
     * @param ioh the io-handler responsible for managing events described by interstOp
     * @param isa the InetSocketAddress of the port we are interested in
     * @throws IOException
     */
    public void addListeningHandler(IOHandler ioh, ServerSocketChannel ssc) throws IOException {
        IPoller p = new Poller();
        p.addListeningHandler(ioh, ssc);
        listeners.add(p);
    }

    /**
     * Add an IOHandler for the given interestOp and the given SocketChannel.
     * @param ioh the io-handler responsible for managing events described by interstOp
     * @param sc the SocketChannel we are interested in doing operations on
     * @throws IOException
     */
    public void addHandler(IOHandler ioh, SocketChannel sc) throws IOException {
        getNextPoller().addHandler(ioh, sc);
    }
    
    /**
     * Retrieve the next poller in line starting over at the end of the list
     * @return the noxt poller in line
     * @throws IOException
     */
    private Poller getNextPoller() throws IOException {
        int currPoller;
        synchronized(this) {
            currPoller = nextPoller;
            nextPoller++;
            if (nextPoller>=pollers.length) {
                nextPoller=0;
            }
        }
        Poller next = pollers[currPoller];
        if (next == null) {
            next = pollers[nextPoller] = new Poller();
            next.startPoller();
        }
        return next;
    }

    /**
     * Start all pollers
     */
    public void startPoller() throws Exception {
        for (int i = 0; i < pollers.length; i++)
            pollers[i].startPoller();
        Iterator<IPoller> i = listeners.iterator();
        while (i.hasNext()) {
            i.next().startPoller();
        }
    }

    /**
     * Shutdown all underlying pollers
     */
    public void shutdown() {
        for (int i = 0; i < this.pollers.length; i++) {
            this.pollers[i].shutdown();
        }
    }
}