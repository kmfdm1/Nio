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
 * KeepAliveState is a state-object helping HttpKeepAliveTracker to detect
 * and close down idle keep-alive-connections
 */
package org.freecs.nio.httpServer;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class KeepAliveState {
    private long lastRequest;
    private boolean onKeepAliveWatchlist=false;
    private final SelectionKey sk;

    public KeepAliveState(SelectionKey sk) {
        this.sk = sk;
    }

    /**
     * Returns the timestamp of the last request
     * @return the timestamp of the last request
     */
    public long getLastRequestTime() {
        return lastRequest;
    }

    /**
     * Updates the timestamp of the last request if it is listed as keep-alive-connection
     */
    public void updateIfListed() {
        if (!onKeepAliveWatchlist)
            return;
        lastRequest = System.currentTimeMillis();
    }

    /**
     * Marks this KeepAliveState as registered with HttpKeepAliveTracker
     * @param val true if listed, false if not listed
     */
    public void listed(boolean val) {
        this.onKeepAliveWatchlist = val;
    }

    /**
     * Returns true if this KeepAliveState is registered with HttpKeepAliveTracker
     * @return true if this KeepAliveState is registered with HttpKeepAliveTracker, false if not
     */
    public boolean listed() {
        return this.onKeepAliveWatchlist;
    }

    /**
     * Closes down this KeepAliveState's channel and cancels it's SelctionKey
     * @throws IOException
     */
    public void closeConnection() throws IOException {
        this.sk.cancel();
        this.sk.channel().close();
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !(obj instanceof KeepAliveState))
            return false;
        return this.sk.equals(((KeepAliveState) obj).sk);
    }

    public int hashCode() {
        return this.sk.hashCode();
    }
}
