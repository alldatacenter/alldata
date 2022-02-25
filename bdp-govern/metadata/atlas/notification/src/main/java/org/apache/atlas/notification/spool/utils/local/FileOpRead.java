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

import org.apache.atlas.notification.spool.SpoolUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class FileOpRead extends FileOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FileOpRead.class);

    private String[] items;

    public FileOpRead(String source) {
        super(source);
    }

    @Override
    public FileLock run(RandomAccessFile randomAccessFile, FileChannel channel, String json) throws IOException {
        items = null;

        byte[] bytes = new byte[(int) randomAccessFile.length()];

        randomAccessFile.readFully(bytes);

        int    rawRecords = 0;
        String allRecords = new String(bytes);

        if (StringUtils.isNotEmpty(allRecords)) {
            items = StringUtils.split(allRecords, SpoolUtils.getLineSeparator());

            if (items != null) {
                rawRecords = items.length;
            }
        }

        LOG.info("FileOpRead.run(source={}): loaded file {}, raw records={}", this.getSource(), this.getFile().getAbsolutePath(), rawRecords);

        return null;
    }

    public String[] getItems() {
        return items;
    }
}
