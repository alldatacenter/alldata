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

public class FixedLengthInputStream extends InputStream {

    private InputStream wrappedInputStream = null;
    private long length = 0;

    public FixedLengthInputStream(InputStream instream, long length) {
        if (instream == null || length < 0) {
            throw new IllegalArgumentException("Illegal input stream or length");
        }

        this.wrappedInputStream = instream;
        this.length = length;
    }

    public void reset() throws IOException {
        wrappedInputStream.reset();
    }

    public boolean markSupported() {
        return wrappedInputStream.markSupported();
    }

    public synchronized void mark(int readlimit) {
        wrappedInputStream.mark(readlimit);
    }

    public int available() throws IOException {
        return wrappedInputStream.available();
    }

    @Override
    public long skip(long n) throws IOException {
        return wrappedInputStream.skip(n);
    }

    public InputStream getWrappedInputStream() {
        return wrappedInputStream;
    }

    public void setWrappedInputStream(InputStream instream) {
        this.wrappedInputStream = instream;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    @Override
    public int read() throws IOException {
        return wrappedInputStream.read();
    }
}
