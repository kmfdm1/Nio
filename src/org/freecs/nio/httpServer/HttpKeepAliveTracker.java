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
 * HttpKeepAliveTracker is responsible for monitoring keep-alive-connections and closing
 * them down if there is a period of inactivity greater than keepAliveTimeout.
 * 
 * Keep-alive connections will have to register them passing a KeepAliveState object to this
 * HttpKeepAliveTracker.instance's add method. Also they will have to deregister using remove.
 * 
 */

package org.freecs.nio.httpServer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class HttpKeepAliveTracker implements Runnable {
    /**
     * The only instance of this HttpKeepAliveTracker
     */
    static final HttpKeepAliveTracker instance = new HttpKeepAliveTracker();
    static public final Thread hkatThread = new Thread(instance, "KeepAlive-Timeout-Tracker");
    
    private HttpKeepAliveTracker() {}
    
    private final Set<KeepAliveState> keepAliveRequests = new HashSet<KeepAliveState>();

    public void run() {
        long keepAliveTimeout = 10000;
        long sleepTime = keepAliveTimeout;
        while (true) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) { 
                // nothing to do here
            }
            KeepAliveState kasArr[];
            synchronized (keepAliveRequests) {
                int size = keepAliveRequests.size();
                if (keepAliveRequests.size() < 1) {
                    sleepTime = keepAliveTimeout;
                    continue;
                }
                kasArr = keepAliveRequests.toArray(new KeepAliveState[size]);
            }
            long currentTime = System.currentTimeMillis();
            long nextCheck = System.currentTimeMillis() + keepAliveTimeout;
            for (int i = 0; i < kasArr.length; i++) {
                long hchTimeout = kasArr[i].getLastRequestTime() + keepAliveTimeout;
                if (hchTimeout < currentTime) try {
                        kasArr[i].closeConnection();
                        this.remove(kasArr[i]);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                } else if (hchTimeout < nextCheck)
                    nextCheck = hchTimeout;
            }
            sleepTime = Math.max(0, nextCheck - System.currentTimeMillis());
        }
    }

    /**
     * Add the given KeepAliveState to this Tracker's list of States to track
     * @param kas The KeepAliveState to add
     */
    public void add(KeepAliveState kas) {
        if (kas==null)
            return;
        synchronized(keepAliveRequests) {
            keepAliveRequests.add(kas);
        }
        kas.listed(true);
    }

    /**
     * Remove the given KeepAliveState from this Tracker's list of States to track
     * @param kas The KeepAliveState to remove
     */
    public void remove(KeepAliveState kas) {
        if (kas==null)
            return;
        synchronized(keepAliveRequests) {
            keepAliveRequests.remove(kas);
        }
    }
}