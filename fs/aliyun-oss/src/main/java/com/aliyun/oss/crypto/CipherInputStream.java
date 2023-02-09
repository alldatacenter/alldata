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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class CipherInputStream extends SdkFilterInputStream {
    private static final int MAX_RETRY = 1000;
    private static final int DEFAULT_IN_BUFFER_SIZE = 512;
    private CryptoCipher cryptoCipher;
    private boolean eof;
    private byte[] bufin;
    private byte[] bufout;
    private int curr_pos;
    private int max_pos;

    public CipherInputStream(InputStream is, CryptoCipher cryptoCipher) {
        this(is, cryptoCipher, DEFAULT_IN_BUFFER_SIZE);
    }

    public CipherInputStream(InputStream is, CryptoCipher c, int buffsize) {
        super(is);
        this.cryptoCipher = c;
        if (buffsize <= 0 || (buffsize % DEFAULT_IN_BUFFER_SIZE) != 0) {
            throw new IllegalArgumentException(
                    "buffsize (" + buffsize + ") must be a positive multiple of " + DEFAULT_IN_BUFFER_SIZE);
        }
        this.bufin = new byte[buffsize];
    }

    @Override
    public int read() throws IOException {
        if (curr_pos >= max_pos) {
            if (eof)
                return -1;
            int count = 0;
            int len;
            do {
                if (count > MAX_RETRY)
                    throw new IOException("exceeded maximum number of attempts to read next chunk of data");
                len = nextChunk();
                count++;
            } while (len == 0);

            if (len == -1)
                return -1;
        }
        return ((int) bufout[curr_pos++] & 0xFF);
    };

    @Override
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte buf[], int off, int target_len) throws IOException {
        if (curr_pos >= max_pos) {
            if (eof)
                return -1;
            int count = 0;
            int len;
            do {
                if (count > MAX_RETRY)
                    throw new IOException("exceeded maximum number of attempts to read next chunk of data");
                len = nextChunk();
                count++;
            } while (len == 0);

            if (len == -1)
                return -1;
        }
        if (target_len <= 0)
            return 0;
        int len = max_pos - curr_pos;
        if (target_len < len)
            len = target_len;
        System.arraycopy(bufout, curr_pos, buf, off, len);
        curr_pos += len;
        return len;
    }

    /**
     * Note: This implementation will only skip up to the end of the buffered data,
     * potentially skipping 0 bytes.
     */
    @Override
    public long skip(long n) throws IOException {
        abortIfNeeded();
        int available = max_pos - curr_pos;
        if (n > available)
            n = available;
        if (n < 0)
            return 0;
        curr_pos += n;
        return n;
    }

    @Override
    public int available() {
        abortIfNeeded();
        return max_pos - curr_pos;
    }

    @Override
    public void close() throws IOException {
        in.close();
        try {
            cryptoCipher.doFinal();
        } catch (BadPaddingException ex) {
        } catch (IllegalBlockSizeException ex) {
        }
        curr_pos = max_pos = 0;
        abortIfNeeded();
    }

    @Override
    public boolean markSupported() {
        abortIfNeeded();
        return false;

    }

    @Override
    public void mark(int readlimit) {
        abortIfNeeded();
    }

    @Override
    public void reset() throws IOException {
        throw new IllegalStateException("mark/reset not supported.");
    }

    final void resetInternal() {
        curr_pos = max_pos = 0;
        eof = false;
    }

    /**
     * Reads and process the next chunk of data into memory.
     * 
     * @return the length of the data chunk read and processed, or -1 if end of
     *         stream.
     * @throws IOException
     *             if there is an IO exception from the underlying input stream
     * 
     * @throws SecurityException
     *             if there is authentication failure
     */
    private int nextChunk() throws IOException {
        abortIfNeeded();
        if (eof)
            return -1;
        bufout = null;
        int len = in.read(bufin);
        if (len == -1) {
            eof = true;
            try {
                bufout = cryptoCipher.doFinal();
                if (bufout == null) {
                    return -1;
                }
                curr_pos = 0;
                return max_pos = bufout.length;
            } catch (IllegalBlockSizeException e) {
            } catch (BadPaddingException e) {
                throw new SecurityException(e);
            }
            return -1;
        }
        bufout = cryptoCipher.update(bufin, 0, len);
        curr_pos = 0;
        return max_pos = (bufout == null ? 0 : bufout.length);
    }

    void renewCryptoCipher() {
        cryptoCipher = cryptoCipher.recreate();
    }
}
