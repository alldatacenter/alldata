/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corerpc.netty;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import org.apache.inlong.tubemq.corerpc.RpcConstants;

/**
 * Utility to collect data written to an {@link java.io.OutputStream} in {@link java.nio.ByteBuffer}s.
 * Copied from <a href="http://avro.apache.org">Apache Avro Project</a>
 */
public class ByteBufferOutputStream extends OutputStream {

    private List<ByteBuffer> buffers;

    public ByteBufferOutputStream() {
        reset();
    }

    /**
     * Returns all data written and resets the stream to be empty.
     */
    public List<ByteBuffer> getBufferList() {
        List<ByteBuffer> result = buffers;
        reset();
        for (ByteBuffer buffer : result) {
            buffer.flip();
        }
        return result;
    }

    /**
     * Prepend a list of ByteBuffers to this stream.
     *
     * @param lists    need to prepended content
     */
    public void prepend(List<ByteBuffer> lists) {
        for (ByteBuffer buffer : lists) {
            buffer.position(buffer.limit());
        }
        buffers.addAll(0, lists);
    }

    /**
     * Append a list of ByteBuffers to this stream.
     *
     * @param lists    need to appended content
     */
    public void append(List<ByteBuffer> lists) {
        for (ByteBuffer buffer : lists) {
            buffer.position(buffer.limit());
        }
        buffers.addAll(lists);
    }

    public void reset() {
        buffers = new LinkedList<>();
        buffers.add(ByteBuffer.allocate(RpcConstants.RPC_MAX_BUFFER_SIZE));
    }

    public void write(ByteBuffer buffer) {
        buffers.add(buffer);
    }

    @Override
    public void write(int b) {
        ByteBuffer buffer = buffers.get(buffers.size() - 1);
        if (buffer.remaining() < 1) {
            buffer = ByteBuffer.allocate(RpcConstants.RPC_MAX_BUFFER_SIZE);
            buffers.add(buffer);
        }
        buffer.put((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        ByteBuffer buffer = buffers.get(buffers.size() - 1);
        int remaining = buffer.remaining();
        while (len > remaining) {
            buffer.put(b, off, remaining);
            len -= remaining;
            off += remaining;
            buffer = ByteBuffer.allocate(RpcConstants.RPC_MAX_BUFFER_SIZE);
            buffers.add(buffer);
            remaining = buffer.remaining();
        }
        buffer.put(b, off, len);
    }

    /**
     * Add a buffer to the output without copying, if possible.
     *
     * @param buffer   the content need to written
     */
    public void writeBuffer(ByteBuffer buffer) throws IOException {
        if (buffer.remaining() < RpcConstants.RPC_MAX_BUFFER_SIZE) {
            write(buffer.array(), buffer.position(), buffer.remaining());
        } else { // append w/o copying bytes
            ByteBuffer dup = buffer.duplicate();
            dup.position(buffer.limit()); // ready for flip
            buffers.add(dup);
        }
    }
}
