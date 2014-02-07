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
 * HttpRequestParser is doing the actual parsing of the incomming requests.
 */
package org.freecs.nio.httpServer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class HttpRequestParser {
    private static final byte   H = 72, P = 80, T = 84, SPC=32,
                                SLASH = 47, N1=49, COLON=58,
                                PERIOD = 46, CR = 13, LF = 10;
    
    private static final int    HTTP11 = H+T+T+P+SLASH+N1+PERIOD+N1;

    private static final Charset ascii = Charset.forName("US-ASCII");
    private final ByteBuffer asciiBuffer = ByteBuffer.allocateDirect(2048);
    private final ByteBuffer buff;
    private HttpRequest currentRequest = new HttpRequest();
    private byte currentStep = 0;
    private int methodChecksum = 0, versionChecksum = 0;
    private String key;

    @SuppressWarnings("unused")
    private HttpRequestParser() { buff=null; }

    public HttpRequestParser(HttpConnectionHandler hch, ByteBuffer buff) {
        this.buff = buff;
    }

    /**
     * Get's called if a request finishes to clean out all remainders
     */
    private void cleanup() {
        currentRequest = new HttpRequest();
        currentStep = 0;
        methodChecksum = versionChecksum = 0;
        asciiBuffer.clear();
        key = null;
    }

    /**
     * Parse newly added data and return true if the request has fully arrived
     * @return boolean true if the request is complete, false otherwise
     * @throws HttpError 
     */
    public HttpRequest parseNewData() throws HttpError {
        while (buff.hasRemaining()) {
            switch(currentStep) {
            case 0:
                if (!determineMethod())
                    continue;
                currentStep++;
            case 1:
                if (!extractUrl())
                    continue;
                currentStep++;
            case 2:
                if (!determineProtocolVersion())
                    continue;
                currentStep++;
            case 3:
                if (!parseHeaders())
                    continue;
                currentStep++;
            case 4:
                if (decodeContent()) {
                    HttpRequest result = currentRequest;
                    cleanup();
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * The rest stored within the buffer is post-data
     * @return true if request complete, false if not
     */
    private boolean decodeContent() {
        if (buff.get(buff.position()) == LF) {
            buff.position(buff.position()+1); // get ridd of an LF which may remain from the header
        }
        if (this.currentRequest.getContentLength() < 1)
            return true;
        if (buff.remaining() < this.currentRequest.getContentLength())
            return false;
        byte[] data = new byte[this.currentRequest.getContentLength()];
        buff.get(data);
        this.currentRequest.setData(data);
        return true;
    }

    /**
     * Get the bytes from start to end and convert them to a string
     * @param start start position within buff
     * @param end end position within buff
     * @return the string composed of the bytes stored in buff betweene start and end
     */
    private String getString(int len) {
        byte[] bytes = new byte[len];
        buff.get(bytes);
        return new String(bytes, ascii);
    }

    /**
     * parses header-fields and stores them within the current HttpRequest-object
     * @return true if the header has reached it's end, false if not
     */
    private boolean parseHeaders() {
        if (!buff.hasRemaining())
            return false;
        for (int i = buff.position(); i < buff.limit(); i++) {
            byte b = buff.get(i);
            if (b == LF)
                continue;
            if (b == COLON && i < buff.limit()) {
                key = getString(i - buff.position());
                buff.position(i+1);
                continue;
            }
            if (b == CR) {
                if (i+3 > buff.limit())
                    return false;
                this.currentRequest.addHeader(key, getString(i - buff.position()).trim());
                if (buff.get(i+2) == CR) {
                    buff.position(i+3);
                    return true;
                }
                buff.position(i+2);
                continue;
            }
        }
        return false;
    }

    /**
     * parses the protocol-version-part of the first line of the header
     * @return true if first line is finished
     */
    private boolean determineProtocolVersion() {
        if (!buff.hasRemaining())
            return false;
        byte b = buff.get();
        while (b != CR && b != LF) {
            versionChecksum += b;
            if (!buff.hasRemaining())
                return false;
            b = buff.get();
        }
        if (!buff.hasRemaining())
            return false;
        if (b == CR) {
            buff.position(buff.position()+1); // step over \n
        }
        if (b == CR || b == LF) {
            if (versionChecksum == HTTP11) {
                currentRequest.setHttp11(true);
            } else {
                currentRequest.setHttp11(false);
            }
            return true;
        }
        return false;
    }

    /**
     * parses the url-part of the first line of the header
     * @return true if url-part is finished (marked by a space)
     * @throws HttpError Throws HttpError 414 request url too long if there is not enough space within buff
     */
    private boolean extractUrl() throws HttpError {
        if (!buff.hasRemaining())
            return false;
        for (int i = buff.position(); i < buff.limit(); i++) {
            if (i == buff.capacity()) {
                throw new HttpError(414);
            }
            byte b = buff.get(i);
            if (b == SPC && i < buff.limit()) {
                if (buff.position() == i) {
                    // we have an empty request
                    this.currentRequest.setUrl("");
                } else {
                    this.currentRequest.setUrl(getString(i - 1 - buff.position()));
                }
                buff.position(i+1);
                return true;
            }
        }
        return false;
    }

    /**
     * parses the method used by this request
     * @return true if method-part of first line has reached it's end (marked by a space)
     */
    private boolean determineMethod() {
        if (!buff.hasRemaining())
            return false;
        byte b = buff.get();
        while (b != SPC) {
            methodChecksum += b;
            if (!buff.hasRemaining())
                return false;
            b = buff.get();
        }
        if (b == SPC && buff.hasRemaining()) {
            this.currentRequest.method(methodChecksum);
            buff.position(buff.position()+1);
            return true;
        }
        return false;
    }
}
