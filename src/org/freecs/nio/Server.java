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
 * This is just a example implementation of a real basic http-server only
 * responding with the request's header fields.
 * The only purpose is to show how the Pollers may be put to use.
 */
package org.freecs.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.channels.ServerSocketChannel;
import java.util.Enumeration;

import org.freecs.nio.core.MultithreadedPoller;
import org.freecs.nio.httpServer.HttpKeepAliveTracker;
import org.freecs.nio.httpServer.HttpRequestListener;
import org.freecs.nio.interfaces.IPoller;

public class Server {
    public static void main(String args[]) {
        try {
            IPoller p;
            if (args.length > 0)
                p = new MultithreadedPoller(Integer.parseInt(args[0]));
            else
                p = new MultithreadedPoller(8);
            // IPoller p = new Poller();
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements(); ) {
                NetworkInterface ni = e.nextElement();
                for (Enumeration<InetAddress> ee = ni.getInetAddresses(); ee.hasMoreElements(); ) {
                    ServerSocketChannel ssc = ServerSocketChannel.open();
                    ssc.configureBlocking(false);
                    ssc.socket().bind(new InetSocketAddress(ee.nextElement(), 80));
                    p.addListeningHandler(new HttpRequestListener(p), ssc);
                }
            }
            HttpKeepAliveTracker.hkatThread.start();
            p.startPoller();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
