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

import static com.aliyun.oss.common.utils.LogUtils.getLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

import com.aliyun.oss.ClientException;

public class RepeatableFileInputStream extends InputStream {

    private File file = null;
    private FileInputStream fis = null;
    private FileChannel fileChannel = null;
    private long markPos = 0;

    public RepeatableFileInputStream(File file) throws IOException {
        this(new FileInputStream(file), file);
    }

    public RepeatableFileInputStream(FileInputStream fis) throws IOException {
        this(fis, null);
    }

    public RepeatableFileInputStream(FileInputStream fis, File file) throws IOException {
        this.file = file;
        this.fis = fis;
        this.fileChannel = fis.getChannel();
        this.markPos = fileChannel.position();
    }

    public void reset() throws IOException {
        fileChannel.position(markPos);
        getLog().trace("Reset to position " + markPos);
    }

    public boolean markSupported() {
        return true;
    }

    public void mark(int readlimit) {
        try {
            markPos = fileChannel.position();
        } catch (IOException e) {
            throw new ClientException("Failed to mark file position", e);
        }
        getLog().trace("File input stream marked at position " + markPos);
    }

    public int available() throws IOException {
        return fis.available();
    }

    public void close() throws IOException {
        fis.close();
    }

    public int read() throws IOException {
        return fis.read();
    }

    @Override
    public long skip(long n) throws IOException {
        return fis.skip(n);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return fis.read(b, off, len);
    }

    public InputStream getWrappedInputStream() {
        return this.fis;
    }

    public File getFile() {
        return this.file;
    }
}
