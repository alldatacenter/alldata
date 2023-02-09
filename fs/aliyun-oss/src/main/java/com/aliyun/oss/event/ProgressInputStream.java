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

package com.aliyun.oss.event;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.aliyun.oss.model.WebServiceRequest;

public abstract class ProgressInputStream extends FilterInputStream {

    public static InputStream inputStreamForRequest(InputStream is, WebServiceRequest req) {
        return req == null ? is : new RequestProgressInputStream(is, req.getProgressListener());
    }

    public static InputStream inputStreamForResponse(InputStream is, WebServiceRequest req) {
        return req == null ? is : new ResponseProgressInputStream(is, req.getProgressListener());
    }

    private static final int DEFAULT_NOTIFICATION_THRESHOLD = 8 * 1024;

    private final ProgressListener listener;
    private final int notifyThresHold;
    private int unnotifiedByteCount;
    private boolean hasBeenRead;
    private boolean doneEOF;
    private long notifiedByteCount;

    public ProgressInputStream(InputStream is, ProgressListener listener) {
        this(is, listener, DEFAULT_NOTIFICATION_THRESHOLD);
    }

    public ProgressInputStream(InputStream is, ProgressListener listener, int notifyThresHold) {
        super(is);
        if (is == null || listener == null) {
            throw new IllegalArgumentException();
        }
        this.listener = listener;
        this.notifyThresHold = notifyThresHold;
    }

    protected void onFirstRead() {
    }

    protected void onEOF() {
    }

    protected void onClose() {
        eof();
    }

    protected void onReset() {
    }

    protected void onNotifyBytesRead() {
    }

    private void onBytesRead(int bytesRead) {
        unnotifiedByteCount += bytesRead;
        if (unnotifiedByteCount >= notifyThresHold) {
            onNotifyBytesRead();
            notifiedByteCount += unnotifiedByteCount;
            unnotifiedByteCount = 0;
        }
    }

    @Override
    public int read() throws IOException {
        if (!hasBeenRead) {
            onFirstRead();
            hasBeenRead = true;
        }
        int ch = super.read();
        if (ch == -1)
            eof();
        else
            onBytesRead(1);
        return ch;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        onReset();
        unnotifiedByteCount = 0;
        notifiedByteCount = 0;
    }

    @Override
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!hasBeenRead) {
            onFirstRead();
            hasBeenRead = true;
        }
        int bytesRead = super.read(b, off, len);
        if (bytesRead == -1)
            eof();
        else
            onBytesRead(bytesRead);
        return bytesRead;
    }

    private void eof() {
        if (doneEOF)
            return;
        onEOF();
        unnotifiedByteCount = 0;
        doneEOF = true;
    }

    public final InputStream getWrappedInputStream() {
        return in;
    }

    protected final int getUnnotifiedByteCount() {
        return unnotifiedByteCount;
    }

    protected final long getNotifiedByteCount() {
        return notifiedByteCount;
    }

    @Override
    public void close() throws IOException {
        onClose();
        super.close();
    }

    public final ProgressListener getListener() {
        return listener;
    }
}
