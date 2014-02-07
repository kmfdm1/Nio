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
 * HttpRequest will hold all data received from the client
 */
package org.freecs.nio.httpServer;

import java.util.Properties;
import java.util.Set;

public class HttpRequest extends Properties {

    private static final long serialVersionUID = 3437886775889314680L;

    static final int    METHOD_GET = 224,
                        METHOD_POST = 326,
                        METHOD_PUT = 249,
                        METHOD_HEAD = 274,
                        METHOD_TRACE = 367,
                        METHOD_DELETE = 435,
                        METHOD_OPTIONS = 556,
                        METHOD_CONNECT = 522;

    private final Properties headers = new Properties();
    private byte[] data;
    private int method;
    private String urlString;
    private boolean isHttp11;
    private int contentLength=0;

    private boolean keepAlive;

    public HttpRequest() {
    }

    /**
     * Mark as keep-alive or none-keep-alive connection
     * @param keepAlive boolean true if this is a keep-alive request
     */
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive=keepAlive;
    }
    
    /**
     * Returns true if this request is a keep-alive request
     * @return true if this request is a keep-alive request, false if not.
     */
    public boolean isKeepAlive() {
        return this.keepAlive;
    }

    /**
     * HttpRequestParser will store all headers within this HttpRequest.
     * It also will parse content-length into a int
     * @param key The header-name which will be converted to lower case
     * @param val The header-value untouched
     */
    public void addHeader(String key, String val) {
        String lowerKey = key.toLowerCase();
        if ("content-length".equals(lowerKey)) {
            this.contentLength = Integer.parseInt(val);
        } else if ("connection".equals(lowerKey)) {
            this.setKeepAlive("keep-alive".equalsIgnoreCase(val));
        }
        headers.setProperty(lowerKey, val);
    }

    /**
     * Returns this HttpRequests's content-length of the post-body
     * @return this HttpRequests's content-length of the post-body
     */
    public int getContentLength() {
        return this.contentLength;
    }

    /**
     * Return the value of the header having the given key (name)
     * @param key The name of the header-field to retrieve the value for
     * @return The value of the header having the given key (name)
     */
    public String getHeader(String key) {
        return headers.getProperty(key.toLowerCase());
    }

    /**
     * Returns a set of Strings containing all header-names
     * @return a set of Strings containing all header-names
     */
    public Set<String> headers() {
        return headers.stringPropertyNames();
    }

    /**
     * Store the given post-body-data raw with this request
     * @param val the byte array containing all data of the post-body of this request
     */
    public void setData(byte[] val) {
        this.data = val;
    }

    /**
     * Returns the byte-array containing all data of the post-body of this request 
     * @return the byte-array containing all data of the post-body of this request
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * Set this HttpRequest's request method
     * @param method The checksum of this HttpRequest's request method
     */
    public void method(int method) {
        this.method = method;
    }

    /**
     * Returns this HttpRequest's request method as String
     * @return a String representation of this HttpRequest's request method
     */
    public String method() {
        switch(method) {
        case METHOD_OPTIONS:
            return "OPTIONS";
        case METHOD_GET:
            return "GET";
        case METHOD_HEAD:
            return "HEAD";
        case METHOD_POST:
            return "POST";
        case METHOD_PUT:
            return "PUT";
        case METHOD_DELETE:
            return "DELETE";
        case METHOD_TRACE:
            return "TRACE";
        case METHOD_CONNECT:
            return "CONNECT";
        default:
            return "unknown";
        }
    }

    /**
     * Tell this HttpRequest if it is a http/1.1 request or not.
     * @param isHttp11 true if this HttpRequest is a http/1.1 request, false if it isn't
     */
    public void setHttp11(boolean isHttp11) {
        this.isHttp11 = isHttp11;
    }

    /**
     * Returns true if this HttpRequest is a http/1.1 request
     * @return true if this HttpRequest is a http/1.1 request, false if it isn't
     */
    public boolean isHttp11() {
        return this.isHttp11;
    }

    /**
     * Store the url of the request within this HttpRequest
     * @param url the requested url
     */
    public void setUrl(String url) {
        this.urlString = url;
    }
    /**
     * Returns the url of this HttpRequest's request
     * @return the url of this HttpRequest's request
     */
    public String url() {
        return this.urlString;
    }
}
