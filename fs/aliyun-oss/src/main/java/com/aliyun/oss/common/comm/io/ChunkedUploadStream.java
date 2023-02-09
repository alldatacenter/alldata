/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.comm.io;

import java.io.IOException;
import java.io.InputStream;

import com.aliyun.oss.ClientException;

public class ChunkedUploadStream extends InputStream {

    private static final int DEFAULT_CHUNK_SIZE = 128 * 1024;
    private static final String DEFAULT_CHARTSET_NAME = "utf-8";
    private static final String CLRF = "\r\n";

    private InputStream innerStream;
    private byte[] inputBuffer;
    private byte[] outputBuffer;
    private int outputBufferPos = -1;
    private int outputBufferDataLen = -1;

    private final int innerStreamBufferSize;
    private boolean innerStreamConsumed = false;
    private boolean isTerminatingChunk = false;

    public ChunkedUploadStream(InputStream innerStream, int innerStreamBufferSize) {
        if (innerStream == null) {
            throw new IllegalArgumentException("Source input stream should not be null");
        }

        this.innerStream = innerStream;
        this.innerStreamBufferSize = innerStreamBufferSize;
        this.inputBuffer = new byte[DEFAULT_CHUNK_SIZE];
        this.outputBuffer = new byte[CalculateChunkHeaderLength(DEFAULT_CHUNK_SIZE)];
    }

    @Override
    public int read() throws IOException {
        byte[] tmp = new byte[1];
        int count = read(tmp);
        if (count != -1) {
            return tmp[0];
        } else {
            return count;
        }
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        if (buffer == null) {
            throw new NullPointerException();
        } else if (offset < 0 || count < 0 || count > buffer.length - offset) {
            throw new IndexOutOfBoundsException(
                    String.format("buffer size: %n, offset: %n, count: %n", buffer.length, offset, count));
        } else if (count == 0) {
            return 0;
        }

        if (outputBufferPos == -1) {
            if (innerStreamConsumed && isTerminatingChunk) {
                return -1;
            }

            int bytesRead = fillInputBuffer();
            constructOutputBufferChunk(bytesRead);
            isTerminatingChunk = (innerStreamConsumed && bytesRead == 0);
        }

        int outputRemaining = outputBufferDataLen - outputBufferPos;
        int bytesToRead = count;
        if (outputRemaining < count) {
            bytesToRead = outputRemaining;
        }

        System.arraycopy(outputBuffer, outputBufferPos, buffer, 0, bytesToRead);
        outputBufferPos += bytesToRead;
        if (outputBufferPos >= outputBufferDataLen) {
            outputBufferPos = -1;
        }

        return bytesToRead;
    }

    private int fillInputBuffer() {
        if (innerStreamConsumed) {
            return 0;
        }

        int inputBufferPos = 0;
        while (inputBufferPos < inputBuffer.length && !innerStreamConsumed) {
            int chunkBufferRemaining = inputBuffer.length - inputBufferPos;
            if (chunkBufferRemaining > innerStreamBufferSize) {
                chunkBufferRemaining = innerStreamBufferSize;
            }

            int bytesRead = 0;
            try {
                bytesRead = innerStream.read(inputBuffer, inputBufferPos, chunkBufferRemaining);
                if (bytesRead == -1) {
                    innerStreamConsumed = true;
                } else {
                    inputBufferPos += bytesRead;
                }
            } catch (IOException e) {
                throw new ClientException("Unexpected IO exception, " + e.getMessage(), e);
            }
        }

        return inputBufferPos;
    }

    private void constructOutputBufferChunk(int dataLen) {
        StringBuilder chunkHeader = new StringBuilder();
        chunkHeader.append(Integer.toHexString(dataLen));
        chunkHeader.append(CLRF);

        try {
            byte[] header = chunkHeader.toString().getBytes(DEFAULT_CHARTSET_NAME);
            byte[] trailer = CLRF.getBytes(DEFAULT_CHARTSET_NAME);

            int writePos = 0;
            System.arraycopy(header, 0, outputBuffer, writePos, header.length);
            writePos += header.length;
            if (dataLen > 0) {
                System.arraycopy(inputBuffer, 0, outputBuffer, writePos, dataLen);
                writePos += dataLen;
            }
            System.arraycopy(trailer, 0, outputBuffer, writePos, trailer.length);

            outputBufferPos = 0;
            outputBufferDataLen = header.length + dataLen + trailer.length;
        } catch (Exception e) {
            throw new ClientException("Unable to sign the chunked data, " + e.getMessage(), e);
        }
    }

    private static int CalculateChunkHeaderLength(long chunkDataSize) {
        return (int) (Long.toHexString(chunkDataSize).length() + CLRF.length() + chunkDataSize + CLRF.length());
    }
}
