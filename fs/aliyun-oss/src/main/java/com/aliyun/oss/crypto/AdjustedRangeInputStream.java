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

package com.aliyun.oss.crypto;

import static com.aliyun.oss.crypto.SdkRuntime.shouldAbort;

import java.io.IOException;
import java.io.InputStream;
import com.aliyun.oss.ClientErrorCode;
import com.aliyun.oss.ClientException;

/**
 * Reads only a specific range of bytes from the underlying input stream.
 */
public class AdjustedRangeInputStream extends InputStream {
    private InputStream decryptedContents;
    private long virtualAvailable;
    private boolean closed;

    /**
     * Creates a new DecryptedContentsInputStream object.
     *
     * @param objectContents
     *      The input stream containing the object contents retrieved from OSS
     * @param rangeBeginning
     *      The position of the left-most byte desired by the user
     * @param rangeEnd
     *      The position of the right-most byte desired by the user
     * @throws IOException
     *      If there are errors skipping to the left-most byte desired by the user.
     */
    public AdjustedRangeInputStream(InputStream objectContents, long rangeBeginning, long rangeEnd) throws IOException {
        this.decryptedContents = objectContents;
        this.closed = false;
        initializeForRead(rangeBeginning, rangeEnd);
    }

    /**
     * Aborts the inputstream operation if thread is interrupted.
     * interrupted status of the thread is cleared by this method.
     *
     * @throws ClientException with ClientErrorCode INPUTSTREAM_READING_ABORTED if thread aborted.
     */
    protected final void abortIfNeeded() {
        if (shouldAbort()) {
            abort();
            throw new ClientException("Thread aborted, inputStream aborted...",
                                      ClientErrorCode.INPUTSTREAM_READING_ABORTED, null);
        }
    }

    private void abort() {
    }

    /**
     * Skip to the start location of the range of bytes desired by the user.
     */
    private void initializeForRead(long rangeBeginning, long rangeEnd) throws IOException {
        int numBytesToSkip;
        if (rangeBeginning < CryptoScheme.BLOCK_SIZE) {
            numBytesToSkip = (int) rangeBeginning;
        } else {
            int offsetIntoBlock = (int) (rangeBeginning % CryptoScheme.BLOCK_SIZE);
            numBytesToSkip = offsetIntoBlock;
        }
        if (numBytesToSkip != 0) {
            while (numBytesToSkip > 0) {
                this.decryptedContents.read();
                numBytesToSkip--;
            }
        }
        this.virtualAvailable = (rangeEnd - rangeBeginning) + 1;
    }

    @Override
    public int read() throws IOException {
        abortIfNeeded();
        int result;

        if (this.virtualAvailable <= 0) {
            result = -1;
        } else {
            result = this.decryptedContents.read();
        }

        if (result != -1) {
            this.virtualAvailable--;
        } else {
            this.virtualAvailable = 0;
            close();
        }

        return result;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        abortIfNeeded();
        int numBytesRead;
        if (this.virtualAvailable <= 0) {
            numBytesRead = -1;
        } else {
            if (length > this.virtualAvailable) {
                length = (this.virtualAvailable < Integer.MAX_VALUE) ? (int) this.virtualAvailable : Integer.MAX_VALUE;
            }
            numBytesRead = this.decryptedContents.read(buffer, offset, length);
        }

        if (numBytesRead != -1) {
            this.virtualAvailable -= numBytesRead;
        } else {
            this.virtualAvailable = 0;
            close();
        }
        return numBytesRead;
    }

    @Override
    public int available() throws IOException {
        abortIfNeeded();
        int available = this.decryptedContents.available();
        if (available < this.virtualAvailable) {
            return available;
        } else {
            return (int) this.virtualAvailable;
        }
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            this.closed = true;
            if (this.virtualAvailable == 0) {
                drainInputStream(decryptedContents);
            }
            this.decryptedContents.close();
        }
        abortIfNeeded();
    }

    private static void drainInputStream(InputStream in) {
        try {
            while (in.read() != -1) {
            }
        } catch (IOException ignored) {
        }
    }

    public InputStream getWrappedInputStream() {
        return decryptedContents;
    }
}
