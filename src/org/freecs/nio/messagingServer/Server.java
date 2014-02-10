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
 * A implementation to test the messaging-service
 */
package org.freecs.nio.messagingServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;

import org.freecs.nio.core.Poller;
import org.freecs.nio.interfaces.IPoller;

public class Server {

    static InetSocketAddress connectTo = null;
    static int port = 1976;
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Arguments:");
            System.out.println("-connectTo=ip:port (an optional address to connect to");
            System.out.println("-port=port (the port to listen on)");
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-connectTo=")) {
                String[] parts = args[i].substring(11).split(":");
                connectTo = new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
            } else if (args[i].startsWith("-port=")) {
                port = Integer.parseInt(args[i].substring(6));
            } else {
                System.out.println("Unknown argument " + args[i]);
                System.exit(1);
            }
        }
        try {
            IPoller p = new Poller();
            IMessageReceiver mr = new TestCallback();
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements(); ) {
                NetworkInterface ni = e.nextElement();
                for (Enumeration<InetAddress> ee = ni.getInetAddresses(); ee.hasMoreElements(); ) {
                    ServerSocketChannel ssc = ServerSocketChannel.open();
                    ssc.configureBlocking(false);
                    ssc.socket().bind(new InetSocketAddress(ee.nextElement(), port));
                    p.addListeningHandler(new MessagingListener(p, mr), ssc);
                }
            }
            p.startPoller();
            if (connectTo != null) {
                SocketChannel sc = SocketChannel.open();
                sc.configureBlocking(false);
                MessagingHandler mh = new MessagingHandler(10240, mr, SelectionKey.OP_CONNECT);
                p.addHandler(mh, sc);
                sc.connect(connectTo);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        while(true) {
            MessagingListener.sendMessage("Random message " + Math.random());
            try {
                Thread.sleep(1000 + ((long) Math.random() * 1000));
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
