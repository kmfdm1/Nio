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
 * HttpResponse is currently only constructing a response containing some data from the request
 * for test/example purpose
 */
package org.freecs.nio.httpServer;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class HttpResponse {
    public static final HttpResponse CloseConnection = new HttpResponse();
    public final ByteBuffer buff;

    private HttpResponse() { buff = null; }
    
    public HttpResponse(int code) {
        byte[] c = ("HTTP/1.1 " + code + " DAFUQ\r\nConnection: close\r\n\r\nAn error occured").getBytes();
        buff = ByteBuffer.wrap(c);
    }
    
    public HttpResponse(HttpRequest request) {
        StringBuffer content = new StringBuffer("<b>Hello World!</b><p>");
        content.append("method: ").append(request.method()).append("<br />");
        content.append("url: ").append(request.url()).append("<br />");
        content.append("keepAlive: ").append(request.isKeepAlive()).append("<br />");
        content.append("http11: ").append(request.isHttp11()).append("<br /></p>");
        for (Iterator<String> i = request.headers().iterator(); i.hasNext(); ) {
            String key = i.next();
            content.append(key).append(": ").append(request.getHeader(key)).append("<br />");
        }
        byte[] c = content.toString().getBytes();

        StringBuffer header = new StringBuffer("HTTP/");
        if (request.isHttp11()) {
            header.append("1.1");
        } else {
            header.append("1.0");
        }
        header.append(" 200 OK\r\n");
        if (request.isKeepAlive()) {
            header.append("connection: keep-alive\r\nContent-length: ");
            header.append(c.length).append("\r\n");
        }
        header.append("content-type: text/html\r\n");
        header.append("\r\n");
        byte[] h = header.toString().getBytes();
        
        buff = ByteBuffer.allocate(c.length + h.length);
        buff.put(h).put(c);
        buff.flip();
    }
}
