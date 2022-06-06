/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.notification.spool.utils.local;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class FileOpCompaction extends FileOperation {
    private final FileOpRead fileOpLoad;

    public FileOpCompaction(String source) {
        super(source);

        this.fileOpLoad = new FileOpRead(source);
    }

    @Override
    public FileLock run(RandomAccessFile file, FileChannel channel, String json) throws IOException {
        FileLock lock = file.getChannel().tryLock();

        fileOpLoad.perform(getFile(), StringUtils.EMPTY);

        file.getChannel().truncate(0);

        String[] rawItems = fileOpLoad.getItems();

        if (rawItems != null) {
            for (String record : rawItems) {
                if (StringUtils.isNotBlank(record)) {
                    file.writeBytes(record);
                }
            }
        }

        return lock;
    }
}
