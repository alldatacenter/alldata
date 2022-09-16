/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.tools;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.broker.msgstore.disk.FileSegment;
import org.apache.inlong.tubemq.server.broker.msgstore.disk.FileSegmentList;
import org.apache.inlong.tubemq.server.broker.msgstore.disk.Segment;
import org.apache.inlong.tubemq.server.broker.msgstore.disk.SegmentList;
import org.apache.inlong.tubemq.server.broker.msgstore.disk.SegmentType;
import org.apache.inlong.tubemq.server.broker.utils.DataStoreUtils;
import org.apache.inlong.tubemq.server.common.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Topic Storage File(s) Repair Tool
 *
 */
public class StoreRepairAdmin {
    private static final Logger logger =
            LoggerFactory.getLogger(StoreRepairAdmin.class);

    /**
     * Repair topic storage files, rebuild index files by existing data files
     * @param args   Startup parameter array, including the following parts:
     *               The 1st parameter is the path of topic storage files;
     *               The 2nd parameter is the topic name(s) that needs to be repaired,
     *                   if not specified, all topics in the storage path will be repaired.
     */
    public static void main(final String[] args) throws Exception {
        if (args == null || args.length < 1) {
            System.out.println(
                    "[Data Repair] Please input 1 params : storePath [ topicA,topicB,....]");
            return;
        }
        List<String> topicList = null;
        final String storePath = args[0];
        if (args.length > 1) {
            if (args[1] != null && TStringUtils.isNotBlank(args[1])) {
                topicList = Arrays.asList(args[1].split(","));
            }
        }
        final File storeDir = new File(storePath);
        if (!storeDir.exists()) {
            throw new RuntimeException(new StringBuilder(512)
                    .append("[Data Repair] store path is not existed, path is ")
                    .append(storePath).toString());
        }
        if (!storeDir.isDirectory()) {
            throw new RuntimeException(new StringBuilder(512)
                    .append("[Data Repair]  store path is not a directory, path is ")
                    .append(storePath).toString());
        }
        final long start = System.currentTimeMillis();
        final File[] ls = storeDir.listFiles();
        if (topicList == null || topicList.isEmpty()) {
            logger.warn(new StringBuilder(512)
                    .append("[Data Repair] Begin to scan store path: ")
                    .append(storePath).toString());
        } else {
            logger.warn(new StringBuilder(512)
                    .append("[Data Repair] Begin to scan store path: ")
                    .append(storePath).append(",topicList:")
                    .append(topicList.toString()).toString());
        }
        int count = 0;
        ExecutorService executor =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        List<Callable<IndexRepairStore>> tasks = new ArrayList<>();
        if (ls != null) {
            for (final File subDir : ls) {
                if (subDir == null) {
                    continue;
                }
                if (subDir.isDirectory()) {
                    final String name = subDir.getName();
                    final int index = name.lastIndexOf('-');
                    if (index < 0) {
                        continue;
                    }
                    final String topic = name.substring(0, index);
                    if (topicList != null && !topicList.isEmpty()) {
                        if (!topicList.contains(topic)) {
                            continue;
                        }
                    }
                    final int storeId = Integer.parseInt(name.substring(index + 1));
                    tasks.add(new Callable<IndexRepairStore>() {
                        @Override
                        public IndexRepairStore call() throws Exception {
                            StringBuilder sBuilder = new StringBuilder(512);
                            logger.info(sBuilder.append("[Data Repair] Loading data directory:")
                                    .append(subDir.getAbsolutePath()).append("...").toString());
                            sBuilder.delete(0, sBuilder.length());
                            final IndexRepairStore messageStore =
                                    new IndexRepairStore(storePath, topic, storeId);
                            messageStore.reCreateIndexFiles();
                            logger.info(sBuilder.append("[Data Repair] Finished data index recreation :")
                                    .append(subDir.getAbsolutePath()).toString());
                            return messageStore;
                        }
                    });
                    count++;
                }
            }
        }
        if (count > 0) {
            CompletionService<IndexRepairStore> completionService =
                    new ExecutorCompletionService<>(executor);
            for (Callable<IndexRepairStore> task : tasks) {
                completionService.submit(task);
            }
            for (int i = 0; i < tasks.size(); i++) {
                IndexRepairStore messageStore =
                        completionService.take().get();
                if (messageStore != null) {
                    messageStore.close();
                }
            }
            tasks.clear();
        }
        executor.shutdown();
        try {
            executor.awaitTermination(30 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //
        }
        logger.warn(new StringBuilder(512)
                .append("[Data Repair] End to scan data path in ")
                .append((System.currentTimeMillis() - start) / 1000)
                .append(" secs").toString());
        System.exit(0);
    }

    private static class IndexRepairStore implements Closeable {
        private static final String DATA_SUFFIX = ".tube";
        private static final String INDEX_SUFFIX = ".index";
        private static final int ONE_M_BYTES = 10 * 1024 * 1024;
        private final String topic;
        private final int storeId;
        private final String basePath;
        private final String topicKey;
        private final String storePath;
        private final String indexPath;
        private final File topicDir;
        private final File indexDir;
        private int maxIndexSegmentSize =
                500000 * DataStoreUtils.STORE_INDEX_HEAD_LEN;
        private SegmentList segments;

        public IndexRepairStore(final String basePath,
                               final String topic,
                               final int storeId) {
            this.basePath = basePath;
            this.topic = topic;
            this.storeId = storeId;
            StringBuilder sBuilder = new StringBuilder(512);
            this.topicKey =
                    sBuilder.append(this.topic).append("-").append(this.storeId).toString();
            sBuilder.delete(0, sBuilder.length());
            this.storePath = sBuilder.append(this.basePath)
                    .append(File.separator).append(this.topicKey).toString();
            sBuilder.delete(0, sBuilder.length());
            this.indexPath = sBuilder.append(this.basePath)
                    .append(File.separator).append(this.topicKey)
                    .append(File.separator).append("index").toString();
            sBuilder.delete(0, sBuilder.length());
            this.topicDir = new File(storePath);
            this.indexDir = new File(indexPath);
        }

        public void reCreateIndexFiles() {
            final StringBuilder sBuilder = new StringBuilder(512);
            try {
                loadDataSegments(sBuilder);
                deleteIndexFiles(sBuilder);
                createIndexFiles();
            } catch (Throwable ee) {
                sBuilder.delete(0, sBuilder.length());
                logger.error(sBuilder.append("ReCreate Index File of ")
                        .append(this.topicKey).append(" error ").toString(), ee);
            }
        }

        private void loadDataSegments(final StringBuilder sBuilder) throws IOException {
            if (!topicDir.exists()) {
                throw new RuntimeException(sBuilder
                        .append("[Data Repair] Topic data path is not existed, path is ")
                        .append(storePath).toString());
            }
            if (!topicDir.isDirectory()) {
                throw new RuntimeException(sBuilder
                        .append("[Data Repair]  Topic data path is not a directory, path is ")
                        .append(storePath).toString());
            }
            loaderSegments();
        }

        private void loaderSegments() throws IOException {
            final List<Segment> accum = new ArrayList<>();
            String fileSuffix = DataStoreUtils.DATA_FILE_SUFFIX;
            final File[] ls = topicDir.listFiles();
            if (ls != null) {
                for (final File file : ls) {
                    if (file == null) {
                        continue;
                    }
                    if (file.isFile() && file.toString().endsWith(fileSuffix)) {
                        if (!file.canRead()) {
                            throw new IOException(new StringBuilder(512)
                                    .append("Could not read DATA file ").append(file).toString());
                        }
                        final String filename = file.getName();
                        final long start =
                                Long.parseLong(filename.substring(0, filename.length() - fileSuffix.length()));
                        accum.add(new FileSegment(start, file, false, SegmentType.DATA));
                    }
                }
            }
            if (accum.size() > 0) {
                // compare segment's start value, and sort order
                Collections.sort(accum, new Comparator<Segment>() {
                    @Override
                    public int compare(final Segment o1, final Segment o2) {
                        if (o1.getStart() == o2.getStart()) {
                            return 0;
                        } else if (o1.getStart() > o2.getStart()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }
                });
                validateSegments("DATA", accum);
            }
            segments = new FileSegmentList(accum.toArray(new Segment[accum.size()]));
            logger.info(new StringBuilder(512)
                    .append("[Data Repair] Loaded DATA ")
                    .append(accum.size()).append(" segments from ")
                    .append(topicDir.getAbsolutePath()).toString());
        }

        public void validateSegments(final String segTypeStr, final List<Segment> segments) {
            // valid segments continuous
            for (int i = 0; i < segments.size() - 1; i++) {
                final Segment curr = segments.get(i);
                final Segment next = segments.get(i + 1);
                if (curr.getStart() + curr.getCachedSize() != next.getStart()) {
                    throw new IllegalStateException(new StringBuilder(512)
                            .append("The following ").append(segTypeStr)
                            .append(" segments don't validate: ")
                            .append(curr.getFile().getAbsolutePath()).append(", ")
                            .append(next.getFile().getAbsolutePath()).toString());
                }
            }
        }

        @Override
        public void close() throws IOException {
            try {
                if (this.segments != null) {
                    for (final Segment segment : this.segments.getView()) {
                        segment.close();
                    }
                }
            } catch (Throwable e3) {
                logger.error("[Data Repair] Close data segments error", e3);
            }
        }

        private void deleteIndexFiles(final StringBuilder sBuilder) {
            if (indexDir.exists()) {
                if (!indexDir.isDirectory()) {
                    throw new RuntimeException(sBuilder
                            .append("[Data Repair] Topic index path is not a directory, path is ")
                            .append(indexPath).toString());
                }
                try {
                    FileUtil.fullyDeleteContents(indexDir);
                } catch (Throwable er) {
                    logger.error(sBuilder
                            .append("[Data Repair] delete index files error, path is ")
                            .append(indexPath).toString(), er);
                }
            } else {
                if (!indexDir.mkdirs()) {
                    throw new RuntimeException(sBuilder
                            .append("[Data Repair] Could not make index directory ")
                            .append(indexPath).toString());
                }
                if (!indexDir.isDirectory() || !indexDir.canRead()) {
                    throw new RuntimeException(sBuilder.append("[Data Repair] Index path ")
                            .append(indexPath).append(" is not a readable directory").toString());
                }
            }
        }

        private void createIndexFiles() {
            final Segment[] segments = this.segments.getView();
            if (segments.length == 0) {
                return;
            }
            long gQueueOffset = -1;
            Segment curPartSeg = null;
            final ByteBuffer dataBuffer = ByteBuffer.allocate(ONE_M_BYTES);
            final ByteBuffer indexBuffer =
                    ByteBuffer.allocate(DataStoreUtils.STORE_INDEX_HEAD_LEN);
            for (Segment curSegment : segments) {
                if (curSegment == null) {
                    continue;
                }
                try {
                    long curOffset = 0L;
                    while (curOffset < curSegment.getCachedSize()) {
                        dataBuffer.clear();
                        curSegment.read(dataBuffer, curOffset);
                        dataBuffer.flip();
                        int dataStart = 0;
                        int dataRealLimit = dataBuffer.limit();
                        for (dataStart = 0; dataStart < dataRealLimit; ) {
                            if (dataRealLimit - dataStart < DataStoreUtils.STORE_DATA_HEADER_LEN) {
                                dataStart += DataStoreUtils.STORE_DATA_HEADER_LEN;
                                break;
                            }
                            final int msgLen =
                                    dataBuffer.getInt(dataStart + DataStoreUtils.STORE_HEADER_POS_LENGTH);
                            final int msgToken =
                                    dataBuffer.getInt(dataStart + DataStoreUtils.STORE_HEADER_POS_DATATYPE);
                            if (msgToken != DataStoreUtils.STORE_DATA_TOKER_BEGIN_VALUE) {
                                dataStart += 1;
                                continue;
                            }
                            final int msgSize = msgLen + 4;
                            final long msgOffset =
                                    curSegment.getStart() + curOffset + dataStart;
                            final long queueOffset =
                                    dataBuffer.getLong(dataStart + DataStoreUtils.STORE_HEADER_POS_QUEUE_LOGICOFF);
                            final int partitionId =
                                    dataBuffer.getInt(dataStart + DataStoreUtils.STORE_HEADER_POS_QUEUEID);
                            final int keyCode =
                                    dataBuffer.getInt(dataStart + DataStoreUtils.STORE_HEADER_POS_KEYCODE);
                            final long timeRecv =
                                    dataBuffer.getLong(dataStart + DataStoreUtils.STORE_HEADER_POS_RECEIVEDTIME);
                            dataStart += msgSize;
                            indexBuffer.clear();
                            indexBuffer.putInt(partitionId);
                            indexBuffer.putLong(msgOffset);
                            indexBuffer.putInt(msgSize);
                            indexBuffer.putInt(keyCode);
                            indexBuffer.putLong(timeRecv);
                            indexBuffer.flip();
                            if (curPartSeg == null) {
                                if (gQueueOffset < 0) {
                                    gQueueOffset = queueOffset;
                                }
                                File newFile =
                                        new File(this.indexDir,
                                                DataStoreUtils.nameFromOffset(gQueueOffset, INDEX_SUFFIX));
                                curPartSeg =
                                        new FileSegment(queueOffset, newFile, SegmentType.INDEX);
                            }
                            curPartSeg.append(indexBuffer, timeRecv, timeRecv);
                            gQueueOffset += DataStoreUtils.STORE_INDEX_HEAD_LEN;
                            if (curPartSeg.getCachedSize() >= maxIndexSegmentSize) {
                                curPartSeg.flush(true);
                                curPartSeg.close();
                                curPartSeg = null;
                            }
                        }
                        curOffset += dataStart;
                    }
                    if (curPartSeg != null) {
                        curPartSeg.flush(true);
                    }
                } catch (Throwable ee) {
                    logger.error("Create Index file error ", ee);
                } finally {
                    if (curSegment != null) {
                        curSegment.relViewRef();
                    }
                }
            }
            try {
                if (curPartSeg != null) {
                    curPartSeg.flush(true);
                    curPartSeg.close();
                    curPartSeg = null;
                }
            } catch (Throwable e2) {
                logger.error("Close Index file error ", e2);
            }
        }

    }

}
