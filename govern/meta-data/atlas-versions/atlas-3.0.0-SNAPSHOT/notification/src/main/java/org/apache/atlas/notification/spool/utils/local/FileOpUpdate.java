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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class FileOpUpdate extends FileOperation {
    private       String       id;
    private final FileOpAppend fileOpAppend;

    public FileOpUpdate(String source, FileOpAppend fileOpAppend) {
        super(source);

        this.fileOpAppend = fileOpAppend;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public FileLock run(RandomAccessFile file, FileChannel channel, String json) throws IOException {
        final FileLock ret;
        final long     position = find(file, getId());

        if (position < 0) {
            ret = fileOpAppend.run(file, channel, json);
        } else {
            ret = channel.tryLock(position, json.length(), false);

            channel.position(position);

            file.writeBytes(json);
        }

        return ret;
    }
}
