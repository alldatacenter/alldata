/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.hive.filesystem;

import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.sink.PartitionPolicy;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;
import org.apache.inlong.sort.hive.HiveBulkWriterFactory;
import org.apache.inlong.sort.hive.HiveWriterFactory;
import org.apache.inlong.sort.hive.util.CacheHolder;
import org.apache.inlong.sort.hive.util.HiveTableUtil;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.connectors.hive.FlinkHiveException;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.formats.hadoop.bulk.HadoopFileCommitter;
import org.apache.flink.formats.hadoop.bulk.HadoopFileCommitterFactory;
import org.apache.flink.formats.hadoop.bulk.HadoopPathBasedBulkWriter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.streaming.api.functions.sink.filesystem.AbstractPartFileWriter;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketWriter;
import org.apache.flink.streaming.api.functions.sink.filesystem.InProgressFileWriter;
import org.apache.flink.streaming.api.functions.sink.filesystem.WriterProperties;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.catalog.hive.client.HiveShim;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.binary.BinaryRowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.ExceptionUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator.RecordWriter;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_DATABASE_PATTERN;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_ENABLE;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_FORMAT;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_TABLE_PATTERN;
import static org.apache.inlong.sort.hive.HiveOptions.HIVE_SCHEMA_SCAN_INTERVAL;

/**
 * The part-file writer that writes to the specified hadoop path.
 */
public class HadoopPathBasedPartFileWriter<IN, BucketID> extends AbstractPartFileWriter<IN, BucketID> {

    private static final Logger LOG = LoggerFactory.getLogger(HadoopPathBasedPartFileWriter.class);

    private final InLongHadoopPathBasedBulkWriter writer;

    private final HadoopFileCommitter fileCommitter;

    private BucketID bucketID;
    private Path targetPath;
    private Path inProgressPath;
    private final HiveShim hiveShim;
    private final String hiveVersion;

    private final boolean sinkMultipleEnable;
    private final String sinkMultipleFormat;

    private final String databasePattern;
    private final String tablePattern;

    private final SchemaUpdateExceptionPolicy schemaUpdatePolicy;

    private final PartitionPolicy partitionPolicy;

    private final String inputFormat;

    private final String outputFormat;

    private final String serializationLib;

    private transient JsonDynamicSchemaFormat jsonFormat;

    @Nullable
    private final transient SinkTableMetricData metricData;

    private final DirtyOptions dirtyOptions;

    private final @Nullable DirtySink<Object> dirtySink;

    public HadoopPathBasedPartFileWriter(final BucketID bucketID,
            Path targetPath,
            Path inProgressPath,
            InLongHadoopPathBasedBulkWriter writer,
            HadoopFileCommitter fileCommitter,
            long createTime,
            HiveShim hiveShim,
            String hiveVersion,
            boolean sinkMultipleEnable,
            String sinkMultipleFormat,
            String databasePattern,
            String tablePattern,
            @Nullable SinkTableMetricData metricData,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink,
            SchemaUpdateExceptionPolicy schemaUpdatePolicy,
            PartitionPolicy partitionPolicy,
            String inputFormat,
            String outputFormat,
            String serializationLib) {
        super(bucketID, createTime);

        this.bucketID = bucketID;
        this.targetPath = targetPath;
        this.inProgressPath = inProgressPath;
        this.writer = writer;
        this.fileCommitter = fileCommitter;
        this.hiveShim = hiveShim;
        this.hiveVersion = hiveVersion;
        this.sinkMultipleEnable = sinkMultipleEnable;
        this.sinkMultipleFormat = sinkMultipleFormat;
        this.databasePattern = databasePattern;
        this.tablePattern = tablePattern;
        this.metricData = metricData;
        this.schemaUpdatePolicy = schemaUpdatePolicy;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
        this.partitionPolicy = partitionPolicy;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.serializationLib = serializationLib;
    }

    @Override
    public void write(IN element, long currentTime) {
        String databaseName = null;
        String tableName = null;
        int recordNum = 1;
        int recordSize = 0;
        JsonNode rootNode = null;
        ObjectIdentifier identifier = null;
        HashMap<ObjectIdentifier, Long> ignoreWritingTableMap = CacheHolder.getIgnoreWritingTableMap();
        try {
            if (sinkMultipleEnable) {
                GenericRowData rowData = (GenericRowData) element;
                if (jsonFormat == null) {
                    jsonFormat = (JsonDynamicSchemaFormat) DynamicSchemaFormatFactory.getFormat(sinkMultipleFormat);
                }
                byte[] rawData = (byte[]) rowData.getField(0);
                rootNode = jsonFormat.deserialize(rawData);
                LOG.debug("root node: {}", rootNode);
                boolean isDDL = jsonFormat.extractDDLFlag(rootNode);
                if (isDDL) {
                    // Ignore ddl change for now
                    return;
                }
                databaseName = jsonFormat.parse(rootNode, databasePattern);
                tableName = jsonFormat.parse(rootNode, tablePattern);

                List<Map<String, Object>> physicalDataList = HiveTableUtil.jsonNode2Map(
                        jsonFormat.getPhysicalData(rootNode));
                recordNum = physicalDataList.size();

                identifier = HiveTableUtil.createObjectIdentifier(databaseName, tableName);

                // ignore writing data into this table
                if (ignoreWritingTableMap.containsKey(identifier)) {
                    return;
                }

                List<String> pkListStr = jsonFormat.extractPrimaryKeyNames(rootNode);
                RowType schema = jsonFormat.extractSchema(rootNode, pkListStr);
                HiveWriterFactory writerFactory = getHiveWriterFactory(identifier, schema, hiveVersion);

                // parse the real location of hive table
                Path inProgressFilePath = getInProgressPath(writerFactory);

                LOG.debug("in progress file path: {}", inProgressFilePath);
                Pair<RecordWriter, Function<RowData, Writable>> pair = getRecordWriterAndRowConverter(writerFactory,
                        inProgressFilePath);
                // reset record writer and row converter
                writer.setRecordWriter(pair.getLeft());
                writer.setRowConverter(pair.getRight());
                writer.setInProgressPath(inProgressFilePath);

                boolean replaceLineBreak = writerFactory.getStorageDescriptor().getInputFormat()
                        .contains("TextInputFormat");

                for (Map<String, Object> record : physicalDataList) {
                    // check and alter hive table if schema has changed
                    boolean changed = checkSchema(identifier, writerFactory, schema);
                    if (changed) {
                        // remove cache and reload hive writer factory
                        CacheHolder.getFactoryMap().remove(identifier);
                        writerFactory = HiveTableUtil.getWriterFactory(hiveShim, hiveVersion, identifier);
                        assert writerFactory != null;
                        FileSinkOperator.RecordWriter recordWriter = writerFactory.createRecordWriter(
                                inProgressFilePath);
                        Function<RowData, Writable> rowConverter = writerFactory.createRowDataConverter();
                        writer.setRecordWriter(recordWriter);
                        writer.setRowConverter(rowConverter);
                        CacheHolder.getRecordWriterHashMap().put(inProgressFilePath, recordWriter);
                        CacheHolder.getRowConverterHashMap().put(inProgressFilePath, rowConverter);
                    }

                    LOG.debug("record: {}", record);
                    LOG.debug("columns : {}", Arrays.deepToString(writerFactory.getAllColumns()));
                    LOG.debug("types: {}", Arrays.deepToString(writerFactory.getAllTypes()));
                    Pair<GenericRowData, Integer> rowDataPair = HiveTableUtil.getRowData(record,
                            writerFactory.getAllColumns(), writerFactory.getAllTypes(), replaceLineBreak);
                    GenericRowData genericRowData = rowDataPair.getLeft();
                    recordSize += rowDataPair.getRight();
                    LOG.debug("generic row data: {}", genericRowData);
                    writer.addElement(genericRowData);
                }
                if (metricData != null) {
                    metricData.outputMetrics(databaseName, tableName, recordNum, recordSize);
                }
            } else {
                RowData data = (RowData) element;
                writer.addElement(data);
                if (metricData != null) {
                    if (data instanceof BinaryRowData) {
                        // mysql cdc sends BinaryRowData
                        metricData.invoke(1, ((BinaryRowData) data).getSizeInBytes());
                    } else {
                        // oracle cdc sends GenericRowData
                        metricData.invoke(1, data.toString().getBytes(StandardCharsets.UTF_8).length);
                    }
                }
            }
        } catch (Exception e) {
            if (schemaUpdatePolicy == null || SchemaUpdateExceptionPolicy.THROW_WITH_STOP == schemaUpdatePolicy) {
                throw new FlinkHiveException("Failed to write data", e);
            } else if (SchemaUpdateExceptionPolicy.STOP_PARTIAL == schemaUpdatePolicy) {
                if (identifier != null) {
                    ignoreWritingTableMap.put(identifier, 1L);
                }
            } else if (SchemaUpdateExceptionPolicy.LOG_WITH_IGNORE == schemaUpdatePolicy) {
                handleDirtyData(databaseName, tableName, recordNum, recordSize, rootNode, jsonFormat, e);
            }
            LOG.error("Failed to write data", e);
        }
        markWrite(currentTime);
    }

    /**
     * upload dirty data metrics and write dirty data
     *
     * @param databaseName database name
     * @param tableName table name
     * @param recordNum record num
     * @param recordSize record byte size
     * @param data raw data
     * @param jsonFormat json formatter for formatting raw data
     * @param e exception
     */
    private void handleDirtyData(String databaseName,
            String tableName,
            int recordNum,
            int recordSize,
            JsonNode data,
            JsonDynamicSchemaFormat jsonFormat,
            Exception e) {
        // upload metrics for dirty data
        if (null != metricData) {
            if (sinkMultipleEnable) {
                metricData.outputDirtyMetrics(databaseName, tableName, recordNum, recordSize);
            } else {
                metricData.invokeDirty(recordNum, recordSize);
            }
        }

        if (!dirtyOptions.ignoreDirty()) {
            return;
        }
        if (data == null || jsonFormat == null) {
            return;
        }
        Triple<String, String, String> triple = getDirtyLabelTagAndIdentity(data, jsonFormat);
        String label = triple.getLeft();
        String tag = triple.getMiddle();
        String identify = triple.getRight();
        if (label == null || tag == null || identify == null) {
            LOG.warn("dirty label or tag or identify is null, ignore dirty data writing");
            return;
        }
        // archive dirty data
        DirtySinkHelper<Object> dirtySinkHelper = new DirtySinkHelper<>(dirtyOptions, dirtySink);
        List<Map<String, Object>> physicalDataList = HiveTableUtil.jsonNode2Map(jsonFormat.getPhysicalData(data));
        for (Map<String, Object> record : physicalDataList) {
            JsonNode jsonNode = HiveTableUtil.object2JsonNode(record);
            dirtySinkHelper.invoke(jsonNode, DirtyType.BATCH_LOAD_ERROR, label, tag, identify, e);
        }
    }

    /**
     * parse dirty label , tag and identify
     *
     * @param data raw data
     * @param jsonFormat json formatter
     * @return dirty label, tag and identify
     */
    private Triple<String, String, String> getDirtyLabelTagAndIdentity(JsonNode data,
            JsonDynamicSchemaFormat jsonFormat) {
        String dirtyLabel = null;
        String dirtyLogTag = null;
        String dirtyIdentify = null;
        try {
            if (dirtyOptions.ignoreDirty()) {
                if (dirtyOptions.getLabels() != null) {
                    dirtyLabel = jsonFormat.parse(data,
                            DirtySinkHelper.regexReplace(dirtyOptions.getLabels(), DirtyType.BATCH_LOAD_ERROR, null));
                }
                if (dirtyOptions.getLogTag() != null) {
                    dirtyLogTag = jsonFormat.parse(data,
                            DirtySinkHelper.regexReplace(dirtyOptions.getLogTag(), DirtyType.BATCH_LOAD_ERROR, null));
                }
                if (dirtyOptions.getIdentifier() != null) {
                    dirtyIdentify = jsonFormat.parse(data,
                            DirtySinkHelper.regexReplace(dirtyOptions.getIdentifier(), DirtyType.BATCH_LOAD_ERROR,
                                    null));
                }
            }
        } catch (Exception e) {
            LOG.warn("Parse dirty options failed. {}", ExceptionUtils.stringifyException(e));
        }
        return new ImmutableTriple<>(dirtyLabel, dirtyLogTag, dirtyIdentify);
    }

    /**
     * get hive writer factory, create table if not exists automatically
     *
     * @param identifier hive database and table name
     * @param schema hive field with flink type
     * @return hive writer factory
     */
    private HiveWriterFactory getHiveWriterFactory(ObjectIdentifier identifier, RowType schema, String hiveVersion) {
        HiveWriterFactory writerFactory = HiveTableUtil.getWriterFactory(hiveShim, hiveVersion, identifier);
        if (writerFactory == null) {
            // hive table may not exist, auto create
            HiveTableUtil.createTable(identifier.getDatabaseName(), identifier.getObjectName(), schema, partitionPolicy,
                    hiveVersion, inputFormat, outputFormat, serializationLib);
            writerFactory = HiveTableUtil.getWriterFactory(hiveShim, hiveVersion, identifier);
        }
        return writerFactory;
    }

    /**
     * get target hdfs path and temp hdfs path for data writing
     *
     * @param writerFactory hive writer factory
     * @return pair of target path and temp path
     */
    private Path getInProgressPath(HiveWriterFactory writerFactory) throws IOException, ClassNotFoundException {
        String location = writerFactory.getStorageDescriptor().getLocation();
        String path = targetPath.toUri().getPath();
        path = path.substring(path.indexOf("/tmp/") + 5);
        Path targetPath = new Path(location + "/" + path);
        return new Path(targetPath.getParent() + "/" + inProgressPath.getName());
    }

    /**
     * get hive record writer and row converter
     *
     * @param writerFactory hive writer factory
     * @param inProgressFilePath temp hdfs file path for writing data
     * @return pair of hive record writer and row converter objects
     */
    private Pair<RecordWriter, Function<RowData, Writable>> getRecordWriterAndRowConverter(
            HiveWriterFactory writerFactory, Path inProgressFilePath) {
        FileSinkOperator.RecordWriter recordWriter;
        Function<RowData, Writable> rowConverter;
        if (!CacheHolder.getRecordWriterHashMap().containsKey(inProgressFilePath)) {
            recordWriter = writerFactory.createRecordWriter(inProgressFilePath);
            rowConverter = writerFactory.createRowDataConverter();
            CacheHolder.getRecordWriterHashMap().put(inProgressFilePath, recordWriter);
            CacheHolder.getRowConverterHashMap().put(inProgressFilePath, rowConverter);
        } else {
            recordWriter = CacheHolder.getRecordWriterHashMap().get(inProgressFilePath);
            rowConverter = CacheHolder.getRowConverterHashMap().get(inProgressFilePath);
        }
        return new ImmutablePair<>(recordWriter, rowConverter);
    }

    /**
     * check if source table schema changes
     *
     * @param identifier hive database name and table name
     * @param writerFactory hive writer factory
     * @param schema hive field with flink types
     * @return if schema has changed
     */
    private boolean checkSchema(ObjectIdentifier identifier, HiveWriterFactory writerFactory, RowType schema) {
        HashMap<ObjectIdentifier, Long> schemaCheckTimeMap = CacheHolder.getSchemaCheckTimeMap();
        long lastUpdate = schemaCheckTimeMap.getOrDefault(identifier, -1L);
        // handle the schema every `HIVE_SCHEMA_SCAN_INTERVAL` milliseconds
        int scanSchemaInterval = Integer.parseInt(writerFactory.getJobConf()
                .get(HIVE_SCHEMA_SCAN_INTERVAL.key(), HIVE_SCHEMA_SCAN_INTERVAL.defaultValue() + ""));
        boolean changed = false;
        if (System.currentTimeMillis() - lastUpdate >= scanSchemaInterval) {
            changed = HiveTableUtil.changeSchema(schema, writerFactory.getAllColumns(),
                    writerFactory.getAllTypes(), identifier.getDatabaseName(), identifier.getObjectName(), hiveVersion);
            schemaCheckTimeMap.put(identifier, System.currentTimeMillis());
        }
        return changed;
    }

    @Override
    public InProgressFileRecoverable persist() {
        throw new UnsupportedOperationException("The path based writers do not support persisting");
    }

    @Override
    public PendingFileRecoverable closeForCommit() throws IOException {
        if (sinkMultipleEnable) {
            LOG.info("record writer cache {}", CacheHolder.getRecordWriterHashMap());
            Iterator<Path> iterator = CacheHolder.getRecordWriterHashMap().keySet().iterator();
            while (iterator.hasNext()) {
                Path inProgressFilePath = iterator.next();
                // one flink batch writes many hive tables, they are the same inProgressPath
                if (inProgressFilePath.getName().equals(this.inProgressPath.getName())) {
                    FileSinkOperator.RecordWriter recordWriter = CacheHolder.getRecordWriterHashMap()
                            .get(inProgressFilePath);
                    writer.setRecordWriter(recordWriter);
                    writer.flush();
                    writer.finish();
                    // clear cache
                    iterator.remove();
                    CacheHolder.getRowConverterHashMap().remove(inProgressFilePath);

                    // parse the target location of hive table
                    String tmpFileName = inProgressFilePath.getName();
                    String targetPathName = tmpFileName.substring(1, tmpFileName.lastIndexOf(".inprogress"));
                    Path targetPath = new Path(inProgressFilePath.getParent() + "/" + targetPathName);

                    LOG.info("file committer target path {}, in progress file {}", targetPath, inProgressFilePath);
                    HadoopRenameFileCommitter committer = new HadoopRenameFileCommitter(
                            ((HadoopRenameFileCommitter) fileCommitter).getConfiguration(),
                            targetPath,
                            inProgressFilePath,
                            true);
                    CacheHolder.getFileCommitterHashMap().put(inProgressFilePath, committer);
                }
            }
        } else {
            writer.flush();
            writer.finish();
            fileCommitter.preCommit();
        }
        return new HadoopPathBasedPendingFile(fileCommitter, getSize()).getRecoverable();
    }

    @Override
    public void dispose() {
        writer.dispose();
    }

    @Override
    public long getSize() throws IOException {
        return writer.getSize();
    }

    static class HadoopPathBasedPendingFile implements BucketWriter.PendingFile {

        private final HadoopFileCommitter fileCommitter;

        private final long fileSize;

        public HadoopPathBasedPendingFile(HadoopFileCommitter fileCommitter, long fileSize) {
            this.fileCommitter = fileCommitter;
            this.fileSize = fileSize;
        }

        @Override
        public void commit() throws IOException {
            fileCommitter.commit();
        }

        @Override
        public void commitAfterRecovery() throws IOException {
            fileCommitter.commitAfterRecovery();
        }

        public PendingFileRecoverable getRecoverable() {
            return new HadoopPathBasedPendingFileRecoverable(fileCommitter.getTargetFilePath(),
                    fileCommitter.getTempFilePath(), fileSize);
        }
    }

    @VisibleForTesting
    static class HadoopPathBasedPendingFileRecoverable implements PendingFileRecoverable {

        private final Path targetFilePath;

        private final Path tempFilePath;

        private final long fileSize;

        @Deprecated
        // Remained for compatibility
        public HadoopPathBasedPendingFileRecoverable(Path targetFilePath, Path tempFilePath) {
            this.targetFilePath = targetFilePath;
            this.tempFilePath = tempFilePath;
            this.fileSize = -1L;
        }

        public HadoopPathBasedPendingFileRecoverable(Path targetFilePath, Path tempFilePath, long fileSize) {
            this.targetFilePath = targetFilePath;
            this.tempFilePath = tempFilePath;
            this.fileSize = fileSize;
        }

        public Path getTargetFilePath() {
            return targetFilePath;
        }

        public Path getTempFilePath() {
            return tempFilePath;
        }

        public org.apache.flink.core.fs.Path getPath() {
            return new org.apache.flink.core.fs.Path(targetFilePath.toString());
        }

        public long getSize() {
            return fileSize;
        }
    }

    @VisibleForTesting
    static class HadoopPathBasedPendingFileRecoverableSerializer
            implements
                SimpleVersionedSerializer<PendingFileRecoverable> {

        static final HadoopPathBasedPendingFileRecoverableSerializer INSTANCE =
                new HadoopPathBasedPendingFileRecoverableSerializer();

        private static final Charset CHARSET = StandardCharsets.UTF_8;

        private static final int MAGIC_NUMBER = 0x2c853c90;

        @Override
        public int getVersion() {
            return 2;
        }

        @Override
        public byte[] serialize(PendingFileRecoverable pendingFileRecoverable) {
            if (!(pendingFileRecoverable instanceof HadoopPathBasedPartFileWriter.HadoopPathBasedPendingFileRecoverable)) {
                throw new UnsupportedOperationException("Only HadoopPathBasedPendingFileRecoverable is supported.");
            }

            HadoopPathBasedPendingFileRecoverable hadoopRecoverable =
                    (HadoopPathBasedPendingFileRecoverable) pendingFileRecoverable;
            Path path = hadoopRecoverable.getTargetFilePath();
            Path inProgressPath = hadoopRecoverable.getTempFilePath();

            byte[] pathBytes = path.toUri().toString().getBytes(CHARSET);
            byte[] inProgressBytes = inProgressPath.toUri().toString().getBytes(CHARSET);

            byte[] targetBytes = new byte[12 + pathBytes.length + inProgressBytes.length + Long.BYTES];
            ByteBuffer bb = ByteBuffer.wrap(targetBytes).order(ByteOrder.LITTLE_ENDIAN);
            bb.putInt(MAGIC_NUMBER);
            bb.putInt(pathBytes.length);
            bb.put(pathBytes);
            bb.putInt(inProgressBytes.length);
            bb.put(inProgressBytes);
            bb.putLong(hadoopRecoverable.getSize());

            return targetBytes;
        }

        @Override
        public HadoopPathBasedPendingFileRecoverable deserialize(int version, byte[] serialized) throws IOException {
            switch (version) {
                case 1:
                    return deserializeV1(serialized);
                case 2:
                    return deserializeV2(serialized);
                default:
                    throw new IOException("Unrecognized version or corrupt state: " + version);
            }
        }

        private HadoopPathBasedPendingFileRecoverable deserializeV1(byte[] serialized) throws IOException {
            final ByteBuffer bb = ByteBuffer.wrap(serialized).order(ByteOrder.LITTLE_ENDIAN);

            if (bb.getInt() != MAGIC_NUMBER) {
                throw new IOException("Corrupt data: Unexpected magic number.");
            }

            byte[] targetFilePathBytes = new byte[bb.getInt()];
            bb.get(targetFilePathBytes);
            String targetFilePath = new String(targetFilePathBytes, CHARSET);

            byte[] tempFilePathBytes = new byte[bb.getInt()];
            bb.get(tempFilePathBytes);
            String tempFilePath = new String(tempFilePathBytes, CHARSET);

            return new HadoopPathBasedPendingFileRecoverable(new Path(targetFilePath), new Path(tempFilePath));
        }

        private HadoopPathBasedPendingFileRecoverable deserializeV2(byte[] serialized) throws IOException {
            final ByteBuffer bb = ByteBuffer.wrap(serialized).order(ByteOrder.LITTLE_ENDIAN);

            if (bb.getInt() != MAGIC_NUMBER) {
                throw new IOException("Corrupt data: Unexpected magic number.");
            }

            byte[] targetFilePathBytes = new byte[bb.getInt()];
            bb.get(targetFilePathBytes);
            String targetFilePath = new String(targetFilePathBytes, CHARSET);

            byte[] tempFilePathBytes = new byte[bb.getInt()];
            bb.get(tempFilePathBytes);
            String tempFilePath = new String(tempFilePathBytes, CHARSET);

            long fileSize = bb.getLong();

            return new HadoopPathBasedPendingFileRecoverable(new Path(targetFilePath), new Path(tempFilePath),
                    fileSize);
        }
    }

    private static class UnsupportedInProgressFileRecoverableSerializable
            implements
                SimpleVersionedSerializer<InProgressFileRecoverable> {

        static final UnsupportedInProgressFileRecoverableSerializable INSTANCE =
                new UnsupportedInProgressFileRecoverableSerializable();

        @Override
        public int getVersion() {
            throw new UnsupportedOperationException("Persists the path-based part file write is not supported");
        }

        @Override
        public byte[] serialize(InProgressFileRecoverable obj) {
            throw new UnsupportedOperationException("Persists the path-based part file write is not supported");
        }

        @Override
        public InProgressFileRecoverable deserialize(int version, byte[] serialized) {
            throw new UnsupportedOperationException("Persists the path-based part file write is not supported");
        }
    }

    /**
     * Factory to create {@link HadoopPathBasedPartFileWriter}. This writer does not support
     * snapshotting the in-progress files. For pending files, it stores the target path and the
     * staging file path into the state.
     */
    public static class HadoopPathBasedBucketWriter<IN, BucketID> implements BucketWriter<IN, BucketID> {

        private final Configuration configuration;

        private final HadoopPathBasedBulkWriter.Factory<IN> bulkWriterFactory;

        private final HadoopFileCommitterFactory fileCommitterFactory;

        private final HiveWriterFactory hiveWriterFactory;

        @Nullable
        private final transient SinkTableMetricData metricData;

        private final DirtyOptions dirtyOptions;

        private final @Nullable DirtySink<Object> dirtySink;

        private final SchemaUpdateExceptionPolicy schemaUpdatePolicy;

        private final PartitionPolicy partitionPolicy;

        private final String inputFormat;

        private final String outputFormat;

        private final String serializationLib;

        private final HiveShim hiveShim;
        private final String hiveVersion;

        public HadoopPathBasedBucketWriter(Configuration configuration,
                HadoopPathBasedBulkWriter.Factory<IN> bulkWriterFactory,
                HadoopFileCommitterFactory fileCommitterFactory, @Nullable SinkTableMetricData metricData,
                DirtyOptions dirtyOptions, @Nullable DirtySink<Object> dirtySink,
                SchemaUpdateExceptionPolicy schemaUpdatePolicy,
                PartitionPolicy partitionPolicy,
                HiveShim hiveShim,
                String hiveVersion,
                String inputFormat,
                String outputFormat,
                String serializationLib) {
            this.configuration = configuration;
            this.bulkWriterFactory = bulkWriterFactory;
            this.hiveWriterFactory = ((HiveBulkWriterFactory) this.bulkWriterFactory).getFactory();
            this.fileCommitterFactory = fileCommitterFactory;
            this.metricData = metricData;
            this.dirtyOptions = dirtyOptions;
            this.dirtySink = dirtySink;
            this.schemaUpdatePolicy = schemaUpdatePolicy;
            this.partitionPolicy = partitionPolicy;
            this.hiveShim = hiveShim;
            this.hiveVersion = hiveVersion;
            this.inputFormat = inputFormat;
            this.outputFormat = outputFormat;
            this.serializationLib = serializationLib;
        }

        @Override
        public HadoopPathBasedPartFileWriter<IN, BucketID> openNewInProgressFile(BucketID bucketID,
                org.apache.flink.core.fs.Path flinkPath, long creationTime) throws IOException {

            Path path = new Path(flinkPath.toUri());
            HadoopFileCommitter fileCommitter = fileCommitterFactory.create(configuration, path);
            Path inProgressFilePath = fileCommitter.getTempFilePath();

            InLongHadoopPathBasedBulkWriter writer = (InLongHadoopPathBasedBulkWriter) bulkWriterFactory.create(path,
                    inProgressFilePath);
            JobConf jobConf = hiveWriterFactory.getJobConf();
            boolean sinkMultipleEnable = Boolean.parseBoolean(jobConf.get(SINK_MULTIPLE_ENABLE.key(), "false"));
            String sinkMultipleFormat = jobConf.get(SINK_MULTIPLE_FORMAT.key());
            String databasePattern = jobConf.get(SINK_MULTIPLE_DATABASE_PATTERN.key());
            String tablePattern = jobConf.get(SINK_MULTIPLE_TABLE_PATTERN.key());
            return new HadoopPathBasedPartFileWriter<>(bucketID,
                    path,
                    inProgressFilePath,
                    writer,
                    fileCommitter,
                    creationTime,
                    hiveShim,
                    hiveVersion,
                    sinkMultipleEnable,
                    sinkMultipleFormat,
                    databasePattern,
                    tablePattern,
                    metricData,
                    dirtyOptions,
                    dirtySink,
                    schemaUpdatePolicy,
                    partitionPolicy,
                    inputFormat,
                    outputFormat,
                    serializationLib);
        }

        @Override
        public PendingFile recoverPendingFile(PendingFileRecoverable pendingFileRecoverable) throws IOException {
            if (!(pendingFileRecoverable instanceof HadoopPathBasedPartFileWriter.HadoopPathBasedPendingFileRecoverable)) {
                throw new UnsupportedOperationException("Only HadoopPathBasedPendingFileRecoverable is supported.");
            }

            HadoopPathBasedPendingFileRecoverable hadoopRecoverable =
                    (HadoopPathBasedPendingFileRecoverable) pendingFileRecoverable;
            return new HadoopPathBasedPendingFile(
                    fileCommitterFactory.recoverForCommit(configuration, hadoopRecoverable.getTargetFilePath(),
                            hadoopRecoverable.getTempFilePath()),
                    hadoopRecoverable.getSize());
        }

        @Override
        public WriterProperties getProperties() {
            return new WriterProperties(UnsupportedInProgressFileRecoverableSerializable.INSTANCE,
                    HadoopPathBasedPendingFileRecoverableSerializer.INSTANCE, false);
        }

        @Override
        public InProgressFileWriter<IN, BucketID> resumeInProgressFileFrom(BucketID bucketID,
                InProgressFileRecoverable inProgressFileSnapshot, long creationTime) {

            throw new UnsupportedOperationException("Resume is not supported");
        }

        @Override
        public boolean cleanupInProgressFileRecoverable(InProgressFileRecoverable inProgressFileRecoverable) {
            return false;
        }
    }
}