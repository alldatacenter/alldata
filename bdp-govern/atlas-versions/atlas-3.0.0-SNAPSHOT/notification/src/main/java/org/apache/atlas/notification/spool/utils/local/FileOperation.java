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
import org.apache.atlas.notification.spool.models.IndexRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.TimeUnit;

public abstract class FileOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FileOperation.class);

    private static final int    MAX_RETRY_ATTEMPTS               = 5;
    private static final String RANDOM_ACCESS_FILE_OPEN_MODE_RWS = "rws";
    private static final String RANDOM_ACCESS_FILE_OPEN_MODE_R   = "r";

    private final String source;
    private       File   file;
    private       String id;

    public static RandomAccessFile createRandomAccessFileForRead(File file) throws FileNotFoundException {
        return new RandomAccessFile(file, RANDOM_ACCESS_FILE_OPEN_MODE_R);
    }

    public static RandomAccessFile createRandomAccessFile(File file) throws FileNotFoundException {
        return new RandomAccessFile(file, RANDOM_ACCESS_FILE_OPEN_MODE_RWS);
    }

    public static long find(RandomAccessFile raf, String id) throws IOException {
        while (true) {
            String line = raf.readLine();

            if (StringUtils.isEmpty(line)) {
                break;
            }

            if (line.contains(id)) {
                return raf.getChannel().position() - SpoolUtils.getLineSeparator().length() - IndexRecord.RECORD_SIZE;
            }
        }

        return -1;
    }

    public FileOperation(String source) {
        this(source, false);
    }

    public FileOperation(String source, boolean notifyConcurrency) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void perform(File file) {
        perform(file, StringUtils.EMPTY);
    }

    public void perform(File file, String json) {
        setFile(file);

        performWithRetry(file, json);
    }

    public void perform(File file, String id, String json) {
        this.setId(id);
        perform(file, json);
    }

    public abstract FileLock run(RandomAccessFile randomAccessFile, FileChannel channel, String json) throws IOException;


    protected File getFile() {
        return this.file;
    }

    protected String getId() {
        return this.id;
    }

    protected void close(RandomAccessFile randomAccessFile, FileChannel channel, FileLock lock) {
        try {
            if (channel != null) {
                channel.force(true);
            }

            if (lock != null) {
                lock.release();
            }

            if (channel != null) {
                channel.close();
            }

            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        } catch (IOException exception) {
            LOG.error("FileOperation(source={}).close(): failed", getSource(), exception);
        }
    }


    private void setFile(File file) {
        this.file = file;
    }

    private void performWithRetry(File file, String json) {
        for (int i = 0; i < MAX_RETRY_ATTEMPTS; i++) {
            try {
                performOperation(json);
                return;
            } catch (OverlappingFileLockException e) {
                try {
                    int timeout = 1 + (50 * RandomUtils.nextInt(10));
                    LOG.info("FileOperation.performWithRetry(source={}): {}: {}: Waiting: {} ms...", getSource(), getClass().getSimpleName(), file.getName(), timeout);

                    TimeUnit.MILLISECONDS.sleep(timeout);
                } catch (InterruptedException ex) {
                    LOG.error("FileOperation.performWithRetry(source={}): {}: Interrupted!", getSource(), file.getAbsolutePath(), ex);
                }

                LOG.info("FileOperation.performWithRetry(source={}): {}: Re-trying: {}!", getSource(), file.getAbsolutePath(), i);
            }
        }

        LOG.info("FileOperation.performWithRetry(source={}): {}: appendRecord: Could not write.", getSource(), file.getAbsolutePath());
    }

    private boolean performOperation(String json) {
        RandomAccessFile randomAccessFile = null;
        FileChannel      channel          = null;
        FileLock         lock             = null;

        try {
            randomAccessFile = new RandomAccessFile(getFile(), "rws");
            channel          = randomAccessFile.getChannel();
            lock             = run(randomAccessFile, channel, json);
        } catch (FileNotFoundException e) {
            LOG.error("FileOperation.performOperation(source={}): file={}: file not found", getSource(), getFile().getAbsolutePath(), e);
        } catch (IOException exception) {
            LOG.error("FileOperation.performOperation(source={}): file={}: failed", getSource(), getFile().getAbsolutePath());
        } finally {
            close(randomAccessFile, channel, lock);
        }

        return true;
    }
}
