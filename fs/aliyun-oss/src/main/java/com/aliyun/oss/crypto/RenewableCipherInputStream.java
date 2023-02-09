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

import java.io.IOException;
import java.io.InputStream;

/**
 * A specific kind of {@link CipherInputStream} that supports partial
 * mark-and-reset in the sense that, if the underlying input stream supports
 * mark-and-reset, this input stream can then be marked at and get reset back to
 * the very beginning of the stream (but not anywhere else).
 */
public final class RenewableCipherInputStream extends CipherInputStream {
    private boolean hasBeenAccessed;

    public RenewableCipherInputStream(InputStream is, CryptoCipher cryptoCipher) {
        super(is, cryptoCipher);
    }

    public RenewableCipherInputStream(InputStream is, CryptoCipher c, int buffsize) {
        super(is, c, buffsize);
    }

    /**
     * Mark and reset is currently only partially supported, in the sense that, if
     * the underlying input stream supports mark-and-reset, this input stream can
     * then be marked at and get reset back to the very beginning of the stream (but
     * not anywhere else).
     */
    @Override
    public boolean markSupported() {
        abortIfNeeded();
        return in.markSupported();
    }

    /**
     * Mark and reset is currently only partially supported, in the sense that, if
     * the underlying input stream supports mark-and-reset, this input stream can
     * then be marked at and get reset back to the very beginning of the stream (but
     * not anywhere else).
     * 
     * @throws UnsupportedOperationException
     *             if mark is called after this stream has been accessed.
     */
    @Override
    public void mark(final int readlimit) {
        abortIfNeeded();
        if (hasBeenAccessed) {
            throw new UnsupportedOperationException(
                    "Marking is only supported before your first call to " + "read or skip.");
        }
        in.mark(readlimit);
    }

    /**
     * Resets back to the very beginning of the stream.
     * <p>
     * Mark and reset is currently only partially supported, in the sense that, if
     * the underlying input stream supports mark-and-reset, this input stream can
     * then be marked at and get reset back to the very beginning of the stream (but
     * not anywhere else).
     */
    @Override
    public void reset() throws IOException {
        abortIfNeeded();
        in.reset();
        renewCryptoCipher();
        resetInternal();
        hasBeenAccessed = false;
    }

    @Override
    public int read() throws IOException {
        hasBeenAccessed = true;
        return super.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        hasBeenAccessed = true;
        return super.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        hasBeenAccessed = true;
        return super.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        hasBeenAccessed = true;
        return super.skip(n);
    }
}
