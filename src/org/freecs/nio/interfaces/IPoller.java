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
 * IPoller interface
 * 
 * The IPoller will have to do the actual work of selecting ready keys from it's own
 * Selector and distributing the work of reading to the registered IOHandler(s)
 */
package org.freecs.nio.interfaces;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public interface IPoller {
    /**
     * Starts the loop of the implementing Poller continously polling it's selector for ready keys
     * and distributing the work to the registered IOHandler(s)
     * @throws Exception
     */
    public void startPoller() throws Exception;

    /**
     * Add a listening IOHandler for the given ServerSocketChannel. This IOHandler's accept method
     * will be called everytime a connection is arriving for the given ServerSocketChannel.
     * @param ioh IOHandler accepting connections and registering the new connections with this or another IPoller
     * @param ssc ServerSocketChannel used to listen for new connections
     * @throws IOException
     */
    public void addListeningHandler(IOHandler ioh, ServerSocketChannel ssc) throws IOException;
    
    /**
     * Add an IOHandler for the given SocketChannel handling read/write operations as well as disconnects.
     * The given IOHandler will be responsible for cleaning up when the connection gets closed or an operation
     * raises an exception.
     * @param ioh IOHandler handling read/write/disconnect for the given SocketChannel
     * @param sc SocketChannel connected to some client
     * @throws IOException
     */
    public void addHandler(IOHandler ioh, SocketChannel sc) throws IOException;
}