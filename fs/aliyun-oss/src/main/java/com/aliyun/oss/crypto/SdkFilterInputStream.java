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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import com.aliyun.oss.ClientErrorCode;
import com.aliyun.oss.ClientException;

/**
 * Base class for OSS Java SDK specific {@link FilterInputStream}.
 */
public class SdkFilterInputStream extends FilterInputStream {
    private volatile boolean aborted = false;

    public SdkFilterInputStream(InputStream in) {
        super(in);
    }

    /**
     * @return The wrapped stream.
     */
    public InputStream getDelegateStream() {
        return in;
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

    public void abort() {
        if (in instanceof SdkFilterInputStream) {
            ((SdkFilterInputStream) in).abort();
        }
        aborted = true;
    }

    public boolean isAborted() {
        return aborted;
    }

    @Override
    public int read() throws IOException {
        abortIfNeeded();
        return in.read();
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        abortIfNeeded();
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        abortIfNeeded();
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        abortIfNeeded();
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
        abortIfNeeded();
    }

    @Override
    public synchronized void mark(int readlimit) {
        abortIfNeeded();
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        abortIfNeeded();
        in.reset();
    }

    @Override
    public boolean markSupported() {
        abortIfNeeded();
        return in.markSupported();
    }

    public void release() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ex) {
                // Ignore exception.
            }
        }
    }
}
