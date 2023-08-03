/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.notification.spool.utils.local;

import org.apache.commons.lang.StringUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class FileLockedReadWrite extends FileOperation {
    private RandomAccessFile raf;
    private FileChannel      channel;
    private FileLock         lock;

    public FileLockedReadWrite(String source) {
        super(source);
    }

    @Override
    public FileLock run(RandomAccessFile randomAccessFile, FileChannel channel, String json) throws IOException {
        this.raf     = randomAccessFile;
        this.channel = channel;
        this.lock    = channel.tryLock();

        return lock;
    }

    public DataInput getInput(File file) throws IOException {
        return getRaf(file);
    }

    public DataOutput getOutput(File file) throws IOException {
        return getRaf(file);
    }

    public void flush() throws IOException {
        if (channel != null) {
            channel.force(true);
        }
    }

    public void close() {
        super.close(this.raf, this.channel, this.lock);
    }

    private RandomAccessFile getRaf(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rws");

        run(raf, raf.getChannel(), StringUtils.EMPTY);

        return raf;
    }
}
