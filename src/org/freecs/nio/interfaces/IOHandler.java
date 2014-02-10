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

package org.freecs.nio.interfaces;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface IOHandler {
    /**
     * Triggered if a connection is waiting to be accepted 
     * and this io-handler has been registered to react to this event.
     * @param sc the SocketChannel already accepted by the ServerSocketChannel
     */
    void accept(SocketChannel sc);

    /**
     * Triggered if a SocketChannel is ready for connecting to it's peer  
     * and this io-handler has beenregistered to react to this event.
     * @param sk the SelectionKey of the SocketChannel ready for connecting
     */
    void connect();

    /**
     * Triggered if a SocketChannel has data ready for reading  
     * and this io-handler has beenregistered to react to this event.
     * @param buf the ByteBuffer containing data read from the socket
     */
    void read();

    /**
     * Triggered if a SocketChannel is ready for writing  
     * and this io-handler has beenregistered to react to this event.
     * @param sc the SocketChannel ready for writing
     */
    void write();

    /**
     * Triggered if a selection-key has been invalidated for some reason.
     * (e.g. connection closed by remote host)
     * @param sk the SelectionKey which is not valid anymore
     */
    void cleanup();
    
    /**
     * Sets the SelectionKey associated with this handler
     */
    void setSelectionKey (SelectionKey sk);
    
    /**
     * Returns the interstOps of this handler
     */
    int getInterestSet();
}
