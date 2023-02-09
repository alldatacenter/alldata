/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model.select;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.CRC32;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class SelectInputStream extends InputStream {
    private static final int DEFAULT_MESSAGE_BUFFER_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int CHUNK_SIZE = 256;
    private static final String OCTET_STREAM_TYPE = "application/octet-stream";
    private static final String XML_STREAM_TYPE = "text/xml";

    private InputStream input;
    private byte[] inputChunk;
    private ByteBuffer messageBuffer;
    private SelectEventVisitor visitor;
    private ByteBuffer dataBuffer;
    private boolean done;
    private boolean isAborting;
    private int totalLength;
    private int headersLength;
    private int next;

    /**
     * Creates an iterator with an event visitor over a binary input stream
     * 
     * @param input
     *      Binary input strean
     * 
     * @param visitor
     *      Event visitor
     */
    SelectInputStream(InputStream input, SelectEventVisitor visitor) {
        this.input = input;
        this.visitor = visitor;
        inputChunk = new byte[CHUNK_SIZE];
        messageBuffer = ByteBuffer.allocate(DEFAULT_MESSAGE_BUFFER_SIZE);
        done = false;
        isAborting = false;
        totalLength = 0;
        next = 0;
    }

    /**
     * Creates an iterator over a binary input stream
     * 
     * @param input
     *      Binary input strean
     * 
     * @param visitor
     *      Event visitor
     */
    SelectInputStream(InputStream input) {
        this(input, null);
    }

    /**
     * Informs that the serialization process must be aborted.
     * 
     * It does not close immediately, just marks it to do it in the next synchronous call
     */
    public void abort() {
        isAborting = true;
    }
    
    /**
     * Returns the next character in the input stream
     * 
     * @return The next character, or -1 when the input stream is closed or already consumed
     */
    @Override
    public int read() throws IOException {
        if (isAborting) {
            close();
            return -1;
        }

        if (available() == 0) {
            return -1;
        }

        int c = dataBuffer.getInt(next);
        consume(1);
        return c;
    }

    /**
     * Returns an array of characters from the input stream
     * 
     * @param b
     *      Output byte array
     * 
     * @param off
     *      Offset into the output array
     * 
     * @param len
     *      Max number of characters to read
     * 
     * @return The number of characters returned, or -1 when the input stream is closed or already consumed
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (isAborting) {
            close();
            return -1;
        }

        // check if there is more data, otherwise try to load
        int n = available();
        if (n == 0) {
            return -1;
        }

        if (n < len) {
            // maybe there was some buffered data but not enough,
            // try to load more data from the input stream
            fetch(len - n);
            n = available();
        }

        // make a copy
        n = Math.min(n, len);
        int pos = dataBuffer.position();
        dataBuffer.position(next);
        dataBuffer.get(b, off, n);
        dataBuffer.position(pos);
        consume(n);
        return n;
    }

    /**
     * Skips one or more characters from the input stream
     * 
     * @param len
     *      Number of bytes to skip
     * 
     * @return Number of bytes skipped
     */
    @Override
    public long skip(long len) throws IOException {
        if (isAborting) {
            close();
            return 0;
        }

        // first try to skip the buffered data
        long remaining = len;
        if (!done && dataBuffer != null) {
            int n = dataBuffer.position() - next;
            if (n > 0) {
                if (n > remaining) {
                    n = (int)remaining;
                }

                remaining -= n;
                consume(n);
            }
        }

        // if there is still more to consume then
        // do it directly from the input stream
        if (remaining > 0) {
            remaining -= input.skip(remaining);
        }

        return len - remaining;
    }

    /**
     * Returns the current number of bytes buffered.
     * 
     * It only reports the buffered characters but not the total remaining
     * bytes in the stream because this value is not known in advance.
     * 
     * @return Number of buffered bytes, or 0 when the stream is empty
     */
    @Override
    public int available() throws IOException {
        if (isAborting) {
            close();
            return 0;
        }

        do {
            // check if there is still some data buffered
            if (dataBuffer != null) {
                int n = dataBuffer.position() - next;
                if (n > 0) {
                    return n;
                }
            }

            // chech if it is done
            if (done) {
                return 0;
            }

            // otherwise, try to load more data
            fetch(1);
        } while (true);
    }

    /**
     * Closes the stream
     */
    @Override
    public void close() throws IOException {
        // make sure data buffer is empty and that no more data will be loaded
        dataBuffer = null;
        next = 0;
        done = true;
        isAborting = false;
        input.close();
    }

    /**
     * Marks the current position in the stream
     * 
     * NOT SUPPORTED
     */
    @Override
    public synchronized void mark(int readLimit) {
        // not supported
    }

    /**
     * Resets the stream
     * 
     * NOT SUPPORTED, it raises an exception
     */
    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("OBS Select input stream does not support mark and reset methods");
    }

    /**
     * Returns if stream marks are supported
     * 
     * Default is false because marks are not supported
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Consumes some bytes from the current buffer.
     * 
     * @param len
     *      Number of bytes to consume
     */
    private void consume(int len) {
        next += len;
        if (next == dataBuffer.position()) {
            // all data buffer has been consumed, reset it
            next = 0;
            dataBuffer.position(0);
        }
    }

    /**
     * Stores one or more bytes into the input buffer
     * 
     * It loads chunks of data and processes the events until there are enough bytes into the
     * input buffer
     * 
     * @param len
     *      Requested number of bytes to buffer
     * 
     * @throws IOException
     */
    private void fetch(int len) throws IOException {
        if (done) {
            throw new ClosedChannelException();
        }

        while (dataBuffer == null || dataBuffer.position() < len) {
            int loaded = input.read(inputChunk);
            if (loaded == -1) {
                // no more data in the input stream
                if (messageBuffer.position() > 0) {
                    throw new IOException("Service stream ended before a SELECT event could be entirely decoded.");
                }

                break;
            }

            // try to process the next messages
            if (loaded > 0) {
                int avail = messageBuffer.remaining();
                if (avail < loaded) {
                    // if the buffer has to grow, then the total length is known for sure because the prelude
                    // has been processed but the new input chunk may contain data of the next event
                    int newCapacity = totalLength + loaded;
                    ByteBuffer current = messageBuffer.duplicate();
                    current.flip();
                    messageBuffer = ByteBuffer.allocate(newCapacity);
                    messageBuffer.put(current);
                }
                messageBuffer.put(inputChunk, 0, loaded);
        
                // the prelude size + two crc is 16 bytes
                while (messageBuffer.position() >= 16) {
                    if (done) {
                        throw new IOException("There is still message data to process after End event");
                    }
        
                    // extract prelude only once for this message
                    if (totalLength == 0) {
                        extractPrelude();
                    }
        
                    // message is not yet complete, wait for more data
                    if (totalLength > messageBuffer.position()) {
                        break;
                    }
        
                    // now we have the full message, first check the whole crc
                    long crc = toLong(messageBuffer.getInt(totalLength - 4));
                    if (crc != crc32(totalLength - 4)) {
                        throw new IOException("Invalid CRC in OBS Select message");
                    }
        
                    Map<String,String> headers = extractHeaders();
                    String messageType = headers.get(":message-type");
                    if (messageType == null) {
                        throw new IOException("Missing message type in OBS Select message");
                    }
        
                    if (messageType.equals("event")) {
                        processEvent(headers);
                    } else if (messageType.equals("error")) {
                        throw new SelectObjectException(headers.get(":error-code"), headers.get(":error-message"));
                    } else {
                        throw new IOException("Unsupported message type '" + messageType + "'' in OBS Select message");
                    }
        
                    // reuse remaining input for next message
                    if (messageBuffer.position() == totalLength) {
                        messageBuffer.clear();
                    } else {
                        // this is going to be small because the message buffer is fed with small chunks
                        byte[] remaining = new byte[messageBuffer.position() - totalLength];
                        messageBuffer.position(totalLength);
                        messageBuffer.get(remaining);
                        messageBuffer.position(0);
                        messageBuffer.put(remaining);
                    }
        
                    // ready to process next prelude
                    totalLength = 0;
                }
            }
        }
    }

    /**
     * Processed the next event
     * 
     * @param headers
     *      Content of event headers
     * 
     * @throws IOException
     */
    private void processEvent(Map<String,String> headers) throws IOException {
        String contentType = headers.get(":content-type");
        if (contentType == null) {
            contentType = OCTET_STREAM_TYPE;
        }
            
        String eventType = headers.get(":event-type");
        if (eventType == null) {
            throw new IOException("Missing event type in OBS Select message");
        }

        // prepare payload
        ByteBuffer tmp = messageBuffer.duplicate();
        tmp.position(headersLength + 12);
        tmp.limit(totalLength - 4);

        ByteBuffer payload = tmp.slice();
        if (eventType.equals("Records")) {
            if (!contentType.equals(OCTET_STREAM_TYPE)) {
                throw new IOException("Stream type '" + contentType + "' for Records event in OBS Select is not supported");
            }

            // the payload is just full or partial records data
            if (payload.limit() > 0) {
                if (visitor != null) {
                    visitor.visitRecordsEvent(payload);
                }

                // append the payload to the output result
                if (dataBuffer == null) {
                    dataBuffer = ByteBuffer.allocate(payload.limit());
                } else {
                    // check if there is enough buffer
                    if (dataBuffer.remaining() < payload.limit()) {
                        // make a copy into a larger buffer
                        ByteBuffer currentBuffer = dataBuffer.duplicate();
                        currentBuffer.flip();

                        dataBuffer = ByteBuffer.allocate(currentBuffer.limit() + payload.limit());
                        dataBuffer.put(currentBuffer);
                    }
                }
                dataBuffer.put(payload);
            }
            return;
        }
        
        if (eventType.equals("Cont")) {
            if (visitor != null) {
                visitor.visitContinuationEvent();
            }
            return;
        }
        
        if (eventType.equals("Stats") || eventType.equals("Progress")) {
            if (!contentType.equals(XML_STREAM_TYPE)) {
                throw new IOException("Stream type '" + contentType + "' for " + eventType + " event in OBS Select is not supported");
            }

            if (visitor != null) {
                Stats stats = extractStats(payload, eventType);
                if (eventType.equals("Stats")) {
                    visitor.visitStatsEvent(stats.bytesScanned, stats.bytesProcessed, stats.bytesReturned);
                } else {
                    visitor.visitProgressEvent(stats.bytesScanned, stats.bytesProcessed, stats.bytesReturned);
                }
            }
            return;
        }
        
        if (eventType.equals("End")) {
            if (visitor != null) {
                visitor.visitEndEvent();
            }

            done = true;
            return;
        }

        throw new IOException("Unsupported event type '" + eventType + "'' in OBS Select message");
    }

    private class Stats {
        long bytesScanned   = -1;
        long bytesProcessed = -1;
        long bytesReturned  = -1;
    }

    /**
     * Extracts the progress or final statistics from an XML document
     *  
     * @param payload
     *      XML document
     * 
     * @param event
     *      Progress of Stats
     * 
     * @return
     *      Progress or final statistics
     * 
     * @throws IOException
     */
    private Stats extractStats(ByteBuffer payload, String event) throws IOException {
        if (payload.limit() == 0) {
            throw new IOException("XML document for " + event + " event in OBS Select is empty");
        }

        byte[] s = new byte[payload.limit()];
        payload.get(s);

        Stats stats = new Stats();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document doc = dbf.newDocumentBuilder()
                                .parse(new InputSource(new StringReader(new String(s, StandardCharsets.UTF_8))));

            Node node = doc.getDocumentElement();
            if (!event.equals(node.getNodeName())) {
                throw new IOException("Wrong root XML element in " + event + " document in OBS Select");
            }

            node = node.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String name = node.getNodeName();
                    Node value = node.getFirstChild();
                    if (value == null || value.getNodeType() != Node.TEXT_NODE) {
                        throw new IOException("Invalid value for XML element " + name + " in " + event + " document in OBS Select");
                    }

                    long stat = Long.parseLong(value.getTextContent());
                    if (name.equals("BytesScanned")) {
                        stats.bytesScanned = stat;
                    } else if (name.equals("BytesProcessed")) {
                        stats.bytesProcessed = stat;
                    } else if (name.equals("BytesReturned")) {
                        stats.bytesReturned = stat;
                    } else {
                        throw new IOException("Unknown element " + name + " in " + event + " document in OBS Select");
                    }
                }
                node = node.getNextSibling();
            }
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Wrong XML " + event + " document in OBS Select");
        }

        if (stats.bytesScanned == -1 || stats.bytesProcessed == -1 || stats.bytesReturned == -1) {
            throw new IOException("Missing elements in " + event + " document in OBS Select");
        }

        return stats;
    }

    /**
     * Extracts and checks the prelude of an event
     * 
     * @throws IOException
     */
    private void extractPrelude() throws IOException {
        ByteBuffer buf = messageBuffer.duplicate();
        buf.flip();

        totalLength = buf.getInt();
        headersLength = buf.getInt();

        long crc = toLong(buf.getInt());
        if (crc != crc32(8)) {
            throw new IOException("Invalid CRC in OBS Select message header");
        }
    }

    /**
     * Extracts the headers of an event
     * 
     * @return Pairs of header message and value
     * 
     * @throws IOException
     */
    Map<String,String> extractHeaders() throws IOException {
        HashMap<String,String> headers = new HashMap<String,String>();
        ByteBuffer buf = messageBuffer.duplicate();
        buf.position(12);
        buf.limit(headersLength + 12);
        while (buf.position() < buf.limit()) {
            byte[] str = new byte[buf.get()];
            buf.get(str);
            String headerName = new String(str, StandardCharsets.UTF_8);
            if (buf.get() != 7) {
                throw new IOException("Wrong header in OBS Select message");
            }

            str = new byte[buf.get() * 256 + buf.get()];
            buf.get(str);

            String headerValue = new String(str, StandardCharsets.UTF_8);
            headers.put(headerName, headerValue);
        }
        if (buf.position() != buf.limit()) {
            throw new IOException("Wrong headers size in OBS Select message");
        }

        return headers;
    }
    
    /**
     * Conversion to integer to unsigned long integer
     */
    private long toLong(int i) {
        return (long)i & 4294967295L;
    }

    /**
     * Computes the CRC32 of one of more bytes into the message buffer
     */
    private long crc32(int length) {
        byte[] chunk = new byte[length];
        ByteBuffer buf = messageBuffer.duplicate();
        buf.flip();
        buf.get(chunk);

        CRC32 crc = new CRC32();
        crc.update(chunk, 0, length);
        return crc.getValue();
    }
}
