/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.util;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Buffered {@link FileWriter} that supports atomic operations.
 * <p>
 * The write operation is atomic when used for overwriting;
 * it either leaves the original file intact, or it completely rewrites it with new contents.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class AtomicFileWriter extends Writer {

    private final Writer core;

    private final File tmpFile;

    private final File destFile;

    /**
     * Writes with UTF-8 encoding.
     */
    public AtomicFileWriter(File f) throws IOException {
        this(f, "UTF-8");
    }

    /**
     * @param encoding File encoding to write. If null, platform default encoding is chosen.
     */
    public AtomicFileWriter(File f, String encoding) throws IOException {
        FileUtils.forceMkdirParent(f);
        File dir = f.getParentFile();
        try {
            tmpFile = File.createTempFile("atomic", null, dir);
        } catch (IOException e) {
            throw new IOException("Failed to create a temporary file in " + dir, e);
        }
        destFile = f;
        if (encoding == null) {
            encoding = Charset.defaultCharset().name();
        }
        core = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), encoding));
    }

    @Override
    public void write(int c) throws IOException {
        core.write(c);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        core.write(str, off, len);
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        core.write(cbuf, off, len);
    }

    public void flush() throws IOException {
        core.flush();
    }

    public void close() throws IOException {
        core.close();
    }

    /**
     * When the write operation failed, call this method to
     * leave the original file intact and remove the temporary file.
     * This method can be safely invoked from the "finally" block, even after
     * the {@link #commit()} is called, to simplify coding.
     */
    public void abort() throws IOException {
        close();
        tmpFile.delete();
    }

    public void commit() throws IOException {
        close();
        if (destFile.exists()) {
            try {
                FileUtils.forceDelete(destFile);
            } catch (IOException x) {
                tmpFile.delete();
                throw x;
            }
        }
        tmpFile.renameTo(destFile);
    }

    @Override
    protected void finalize() throws Throwable {
        // one way or the other, temporary file should be deleted.
        close();
        tmpFile.delete();
    }

    /**
     * Until the data is committed, this file captures
     * the written content.
     */
    public File getTemporaryFile() {
        return tmpFile;
    }
}
