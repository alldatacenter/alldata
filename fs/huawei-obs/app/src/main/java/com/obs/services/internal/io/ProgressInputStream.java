/**
 * 
 * Copyright 2013-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
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

package com.obs.services.internal.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.obs.services.exception.ObsException;
import com.obs.services.internal.ProgressManager;

public class ProgressInputStream extends FilterInputStream {

    private boolean readFlag;
    private ProgressManager progressManager;
    private boolean endFlag;

    public ProgressInputStream(InputStream in, ProgressManager progressManager) {
        this(in, progressManager, true);
    }

    public ProgressInputStream(InputStream in, ProgressManager progressManager, boolean endFlag) {
        super(in);
        this.progressManager = progressManager;
        this.endFlag = endFlag;
    }

    @Override
    public final boolean markSupported() {
        return false;
    }

    protected final void abortWhileThreadIsInterrupted() {
        if (Thread.interrupted()) {
            throw new ObsException("Abort io due to thread interrupted");
        }
    }

    @Override
    public synchronized void mark(int a) {
        abortWhileThreadIsInterrupted();
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new UnrecoverableIOException("UnRepeatable");
    }

    @Override
    public long skip(long n) throws IOException {
        abortWhileThreadIsInterrupted();
        return super.skip(n);
    }

    @Override
    public int available() throws IOException {
        abortWhileThreadIsInterrupted();
        return super.available();
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
            abortWhileThreadIsInterrupted();
        } finally {
            if (endFlag) {
                this.progressManager.progressEnd();
            }
        }
    }

    @Override
    public int read() throws IOException {
        abortWhileThreadIsInterrupted();
        return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        abortWhileThreadIsInterrupted();
        if (!this.readFlag) {
            this.readFlag = true;
            this.progressManager.progressStart();
        }
        int bytes = super.read(b, off, len);
        this.progressManager.progressChanged(bytes);
        return bytes;
    }

}
