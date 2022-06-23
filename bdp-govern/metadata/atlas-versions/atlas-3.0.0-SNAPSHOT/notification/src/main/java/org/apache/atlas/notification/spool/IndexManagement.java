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
package org.apache.atlas.notification.spool;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.AtlasException;
import org.apache.atlas.notification.spool.models.IndexRecord;
import org.apache.atlas.notification.spool.models.IndexRecords;
import org.apache.atlas.notification.spool.utils.local.FileLockedReadWrite;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class IndexManagement {
    private static final Logger LOG = LoggerFactory.getLogger(IndexManagement.class);

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final SpoolConfiguration config;
    private       IndexFileManager   indexFileManager;
    private       IndexReader        indexReader;
    private       IndexWriter        indexWriter;

    public IndexManagement(SpoolConfiguration config) {
        this.config = config;
    }

    public void init() throws IOException, AtlasException {
        String sourceName = config.getSourceName();

        File spoolDir = SpoolUtils.getCreateDirectoryWithPermissionCheck(config.getSpoolDir(), config.getUser());
        if (spoolDir == null) {
            throw new AtlasException(String.format("%s: %s not found or inaccessible!", sourceName, spoolDir.getAbsolutePath()));
        }

        config.setSpoolDir(spoolDir.getAbsolutePath());

        File archiveDir = SpoolUtils.getCreateDirectory(config.getArchiveDir());
        if (archiveDir == null) {
            throw new AtlasException(String.format("%s: %s not found or inaccessible!", sourceName, archiveDir.getAbsolutePath()));
        }

        File indexFile = SpoolUtils.getCreateFile(config.getIndexFile(), sourceName);

        if (indexFile == null) {
            throw new AtlasException(String.format("%s: %s not found or inaccessible!", sourceName, indexFile.getAbsolutePath()));
        }

        File indexDoneFile = SpoolUtils.getCreateFile(config.getIndexDoneFile(), sourceName);

        if (indexDoneFile == null) {
            throw new AtlasException(String.format("%s: %s not found or inaccessible!", sourceName, indexDoneFile.getAbsolutePath()));
        }

        performInit(indexFile.getAbsolutePath(), sourceName);
    }

    @VisibleForTesting
    void performInit(String indexFilePath, String source) {
        try {
            File spoolDir      = config.getSpoolDir();
            File archiveDir    = config.getArchiveDir();
            File indexFile     = config.getIndexFile();
            File indexDoneFile = config.getIndexDoneFile();

            indexFileManager = new IndexFileManager(source, indexFile, indexDoneFile, archiveDir, config.getMaxArchiveFiles());
            indexReader      = new IndexReader(source, indexFileManager, config.getRetryDestinationMS());
            indexWriter      = new IndexWriter(source, config, indexFileManager, indexReader, spoolDir, archiveDir, config.getFileRolloverSec());
        } catch (Exception e) {
            LOG.error("{}: init: Failed! Error loading records from index file: {}", config.getSourceName(), indexFilePath);
        }
    }

    public boolean isPending() {
        return !indexReader.isEmpty()
                || (indexWriter.getCurrent() != null && indexWriter.getCurrent().isStatusWriteInProgress())
                || (indexReader.currentIndexRecord != null && indexReader.currentIndexRecord.getStatus() == IndexRecord.STATUS_READ_IN_PROGRESS);
    }

    public synchronized DataOutput getSpoolWriter() throws IOException {
        return indexWriter.getCreateWriter();
    }

    public void setSpoolWriteInProgress() {
        this.indexWriter.setFileWriteInProgress(true);
    }

    public void resetSpoolWriteInProgress() {
        this.indexWriter.setFileWriteInProgress(false);
    }

    public void updateFailedAttempt() {
        this.indexReader.updateFailedAttempt();
    }

    public IndexRecord next() throws InterruptedException {
        return indexReader.next();
    }

    public int getQueueSize() {
        return indexReader.size();
    }

    public void removeAsDone(IndexRecord indexRecord) {
        this.indexReader.removeAsDone(indexRecord);
        this.indexWriter.rolloverIfNeeded();
    }

    public void stop() {
        indexWriter.stop();
    }

    public void rolloverSpoolFileIfNeeded() {
        this.indexWriter.rolloverIfNeeded();
    }

    @VisibleForTesting
    IndexFileManager getIndexFileManager() {
        return this.indexFileManager;
    }

    public void update(IndexRecord record) {
        this.indexFileManager.updateIndex(record);

        LOG.info("this.indexFileManager.updateIndex: {}", record.getLine());
    }

    public void flushSpoolWriter() throws IOException {
        this.indexWriter.flushCurrent();
    }

    static class IndexWriter {
        private final String              source;
        private final SpoolConfiguration  config;
        private final File                spoolFolder;
        private final File                archiveFolder;
        private final int                 rollOverTimeout;
        private final IndexFileManager    indexFileManager;
        private final IndexReader         indexReader;
        private final FileLockedReadWrite fileLockedReadWrite;
        private       IndexRecord         currentIndexRecord;
        private       DataOutput          currentWriter;
        private       boolean             fileWriteInProgress;


        public IndexWriter(String source, SpoolConfiguration config, IndexFileManager indexFileManager,
                           IndexReader indexReader,
                           File spoolFolder, File archiveFolder, int rollOverTimeout) {
            this.source              = source;
            this.config              = config;
            this.indexFileManager    = indexFileManager;
            this.indexReader         = indexReader;
            this.spoolFolder         = spoolFolder;
            this.archiveFolder       = archiveFolder;
            this.rollOverTimeout     = rollOverTimeout;
            this.fileLockedReadWrite = new FileLockedReadWrite(source);

            setCurrent(indexFileManager.getFirstWriteInProgressRecord());
        }

        public void setCurrent(IndexRecord indexRecord) {
            this.currentIndexRecord = indexRecord;
        }

        public IndexRecord getCurrent() {
            return this.currentIndexRecord;
        }

        private void setCurrentWriter(File file) throws IOException {
            this.currentWriter = fileLockedReadWrite.getOutput(file);
        }

        public synchronized DataOutput getWriter() {
            return this.currentWriter;
        }

        public synchronized DataOutput getCreateWriter() throws IOException {
            rolloverIfNeeded();

            if (getCurrent() == null) {
                IndexRecord record   = new IndexRecord(StringUtils.EMPTY);
                String      filePath = SpoolUtils.getSpoolFilePath(config, spoolFolder.toString(), archiveFolder.toString(), record.getId());

                record.setPath(filePath);

                indexFileManager.appendToIndexFile(record);

                setCurrent(record);

                LOG.info("IndexWriter.getCreateWriter(source={}): Creating new spool file. File: {}", this.source, filePath);

                setCurrentWriter(new File(filePath));
            } else {
                if (this.currentWriter == null) {
                    LOG.info("IndexWriter.getCreateWriter(source={}): Opening existing file for append: File: {}", this.source, currentIndexRecord.getPath());

                    setCurrentWriter(new File(currentIndexRecord.getPath()));
                }
            }

            return currentWriter;
        }

        public synchronized void rolloverIfNeeded() {
            if (currentWriter != null && shouldRolloverSpoolFile()) {
                LOG.info("IndexWriter.rolloverIfNeeded(source={}): Rolling over. Closing File: {}", this.config.getSourceName(), currentIndexRecord.getPath());

                fileLockedReadWrite.close();

                currentWriter = null;

                currentIndexRecord.setStatusPending();

                indexFileManager.updateIndex(currentIndexRecord);

                LOG.info("IndexWriter.rolloverIfNeeded(source={}): Adding file to queue. File: {}", this.config.getSourceName(), currentIndexRecord.getPath());

                indexReader.addToPublishQueue(currentIndexRecord);

                currentIndexRecord = null;
            }
        }

        private boolean shouldRolloverSpoolFile() {
            return currentIndexRecord != null &&
                    (System.currentTimeMillis() - currentIndexRecord.getCreated() > this.rollOverTimeout);
        }

        void flushCurrent() throws IOException {
            DataOutput pw = getWriter();

            if (pw != null) {
                fileLockedReadWrite.flush();
            }
        }

        public void setFileWriteInProgress(boolean val) {
            this.fileWriteInProgress = val;
        }

        public boolean isWriteInProgress() {
            return this.fileWriteInProgress;
        }

        public void stop() {
            LOG.info("==> IndexWriter.stop(source={})", this.config.getSourceName());

            try {
                DataOutput out = getWriter();

                if (out != null) {
                    flushCurrent();

                    for (int i = 0; i < MAX_RETRY_ATTEMPTS; i++) {
                        if (isWriteInProgress()) {
                            try {
                                TimeUnit.SECONDS.sleep(i);
                            } catch (InterruptedException e) {
                                LOG.error("IndexWriter.stop(source={}): Interrupted!", this.config.getSourceName(), e);

                                break;
                            }

                            continue;
                        }

                        LOG.info("IndexWriter.stop(source={}): Closing open file.", this.config.getSourceName());

                        fileLockedReadWrite.close();
                        currentIndexRecord.setStatusPending();
                        indexFileManager.updateIndex(currentIndexRecord);

                        break;
                    }
                }
            } catch (FileNotFoundException e) {
                LOG.error("IndexWriter.stop(source={}): File not found! {}", this.config.getSourceName(), getCurrent().getPath(), e);
            } catch (IOException exception) {
                LOG.error("IndexWriter.stop(source={}): Error accessing file: {}", this.config.getSourceName(), getCurrent().getPath(), exception);
            } catch (Exception exception) {
                LOG.error("IndexWriter.stop(source={}): Error closing spool file.", this.config.getSourceName(), exception);
            }

            LOG.info("<== IndexWriter.stop(source={})", this.config.getSourceName());
        }
    }

    static class IndexReader {
        private final String                     source;
        private final BlockingQueue<IndexRecord> blockingQueue;
        private final IndexFileManager           indexFileManager;
        private final long                       retryDestinationMS;
        private       IndexRecord                currentIndexRecord;

        public IndexReader(String source, IndexFileManager indexFileManager, long retryDestinationMS) {
            this.source             = source;
            this.blockingQueue      = new LinkedBlockingQueue<>();
            this.retryDestinationMS = retryDestinationMS;
            this.indexFileManager   = indexFileManager;

            List<IndexRecord> records = indexFileManager.getRecords();

            records.stream().forEach(x -> addIfStatus(x, IndexRecord.STATUS_READ_IN_PROGRESS));
            records.stream().forEach(x -> addIfStatus(x, IndexRecord.STATUS_PENDING));
        }

        private void addIfStatus(IndexRecord record, String status) {
            if (record != null && record.getStatus().equals(status)) {
                if (!SpoolUtils.fileExists(record)) {
                    LOG.error("IndexReader.addIfStatus(source={}): file {} not found!", this.source, record.getPath());
                } else {
                    addToPublishQueue(record);
                }
            }
        }

        public void addToPublishQueue(IndexRecord record) {
            try {
                if (!blockingQueue.contains(record)) {
                    blockingQueue.add(record);
                }
            } catch (OverlappingFileLockException lockException) {
                LOG.warn("{}: {}: Someone else has locked the file.", source, record.getPath());
            }
        }

        public IndexRecord next() throws InterruptedException {
            this.currentIndexRecord = blockingQueue.poll(retryDestinationMS, TimeUnit.MILLISECONDS);
            if (this.currentIndexRecord != null) {
                this.currentIndexRecord.setStatus(IndexRecord.STATUS_READ_IN_PROGRESS);
            }

            return this.currentIndexRecord;
        }

        public int size() {
            return blockingQueue.size();
        }

        public boolean isEmpty() {
            return blockingQueue.isEmpty();
        }

        public void updateFailedAttempt() {
            if (currentIndexRecord != null) {
                currentIndexRecord.updateFailedAttempt();

                indexFileManager.updateIndex(currentIndexRecord);
            }
        }

        public void removeAsDone(IndexRecord indexRecord) {
            indexRecord.setDone();

            indexFileManager.remove(indexRecord);
        }
    }

    static class IndexFileManager {
        private final String         source;
        private final File           indexDoneFile;
        private final File           indexFile;
        private final Archiver       archiver;
        private final FileOperations fileOperations;

        public IndexFileManager(String source, File indexFile, File indexDoneFile, File archiveFolder, int maxArchiveFiles) {
            this.source         = source;
            this.indexFile      = indexFile;
            this.indexDoneFile  = indexDoneFile;
            this.archiver       = new Archiver(source, indexDoneFile, archiveFolder, maxArchiveFiles);
            this.fileOperations = new FileOperations(SpoolUtils.getEmptyRecordForWriting(), source);
        }

        public List<IndexRecord> getRecords() {
            return new ArrayList<>(loadRecords(indexFile).getRecords().values());
        }

        public synchronized void delete(File file, String id) {
            fileOperations.delete(file, id);
        }

        public synchronized IndexRecord getFirstWriteInProgressRecord() {
            IndexRecord  ret     = null;
            IndexRecords records = loadRecords(indexFile);

            if (records != null) {
                for (IndexRecord record : records.getRecords().values()) {
                    if (record.isStatusWriteInProgress()) {
                        LOG.info("IndexFileManager.getFirstWriteInProgressRecord(source={}): current file={}", this.source, record.getPath());

                        ret = record;

                        break;
                    }
                }
            }

            return ret;
        }

        public synchronized void remove(IndexRecord record) {
            delete(indexFile, record.getId());

            appendToDoneFile(record);

            IndexRecords records = loadRecords(indexFile);

            if (records.size() == 0) {
                LOG.info("IndexFileManager.remove(source={}): All done!", this.source);

                compactFile(indexFile);
            }
        }

        public void appendToIndexFile(IndexRecord record) {
            fileOperations.append(indexFile, SpoolUtils.getRecordForWriting(record));
        }

        public void updateIndex(IndexRecord record) {
            fileOperations.update(indexFile, record.getId(), SpoolUtils.getRecordForWriting(record));
        }

        private void compactFile(File file) {
            LOG.info("IndexFileManager.compactFile(source={}): compacting file {}", source, file.getAbsolutePath());

            try {
                fileOperations.compact(file);
            } finally {
                LOG.info("IndexFileManager.compactFile(source={}): done compacting file {}", source, file.getAbsolutePath());
            }
        }

        private void appendToDoneFile(IndexRecord indexRecord) {
            String json = SpoolUtils.getRecordForWriting(indexRecord);

            fileOperations.append(indexDoneFile, json);

            archiver.archive(indexRecord);
        }

        @VisibleForTesting
        IndexRecords loadRecords(File file) {
            String[] items = fileOperations.load(file);

            return SpoolUtils.createRecords(items);
        }

        @VisibleForTesting
        File getDoneFile() {
            return this.indexDoneFile;
        }

        @VisibleForTesting
        File getIndexFile() {
            return this.indexFile;
        }

        @VisibleForTesting
        IndexRecord add(String path) {
            IndexRecord record = new IndexRecord(path);

            appendToIndexFile(record);

            return record;
        }
    }
}
