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

package org.apache.inlong.sort.base.dirty.sink.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.formats.json.RowDataToJsonConverters.RowDataToJsonConverter;
import org.apache.flink.runtime.util.ExecutorThreadFactory;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.inlong.sort.base.dirty.DirtyData;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.dirty.utils.FormatUtils;
import org.apache.inlong.sort.base.util.LabelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * S3 dirty sink that is used to sink dirty data to s3
 *
 * @param <T>
 */
public class S3DirtySink<T> implements DirtySink<T> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(S3DirtySink.class);

    private final Map<String, List<String>> batchMap = new HashMap<>();
    private final S3Options s3Options;
    private final AtomicLong readInNum = new AtomicLong(0);
    private final AtomicLong writeOutNum = new AtomicLong(0);
    private final AtomicLong errorNum = new AtomicLong(0);
    private final DataType physicalRowDataType;
    private RowData.FieldGetter[] fieldGetters;
    private RowDataToJsonConverter converter;
    private long batchBytes = 0L;
    private int size;
    private transient volatile boolean closed = false;
    private transient volatile boolean flushing = false;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> scheduledFuture;
    private transient S3Helper s3Helper;

    public S3DirtySink(S3Options s3Options, DataType physicalRowDataType) {
        this.s3Options = s3Options;
        this.physicalRowDataType = physicalRowDataType;
    }

    @Override
    public void open(Configuration configuration) throws Exception {
        converter = FormatUtils.parseRowDataToJsonConverter(physicalRowDataType.getLogicalType());
        fieldGetters = FormatUtils.parseFieldGetters(physicalRowDataType.getLogicalType());
        AmazonS3 s3Client;
        if (s3Options.getAccessKeyId() != null && s3Options.getSecretKeyId() != null) {
            BasicAWSCredentials awsCreds =
                    new BasicAWSCredentials(s3Options.getAccessKeyId(), s3Options.getSecretKeyId());
            s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                            s3Options.getEndpoint(),
                            s3Options.getRegion()))
                    .build();
        } else {
            s3Client = AmazonS3ClientBuilder.standard().withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(s3Options.getEndpoint(), s3Options.getRegion())).build();
        }
        s3Helper = new S3Helper(s3Client, s3Options);
        this.scheduler = new ScheduledThreadPoolExecutor(1,
                new ExecutorThreadFactory("s3-dirty-sink"));
        this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(() -> {
            if (!closed && !flushing) {
                flush();
            }
        }, s3Options.getBatchIntervalMs(), s3Options.getBatchIntervalMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void invoke(DirtyData<T> dirtyData) throws Exception {
        try {
            addBatch(dirtyData);
        } catch (Exception e) {
            if (!s3Options.ignoreSideOutputErrors()) {
                throw new RuntimeException(String.format("Add batch to identifier:%s failed, the dirty data: %s.",
                        dirtyData.getIdentifier(), dirtyData.toString()), e);
            }
            LOGGER.warn("Add batch to identifier:{} failed "
                    + "and the dirty data will be throw away in the future"
                    + " because the option 'dirty.side-output.ignore-errors' is 'true'", dirtyData.getIdentifier());
        }
        if (valid() && !flushing) {
            flush();
        }
    }

    private boolean valid() {
        return (s3Options.getBatchSize() > 0 && size >= s3Options.getBatchSize())
                || batchBytes >= s3Options.getMaxBatchBytes();
    }

    private void addBatch(DirtyData<T> dirtyData) throws IOException {
        readInNum.incrementAndGet();
        String value;
        Map<String, String> labelMap = LabelUtils.parseLabels(dirtyData.getLabels());
        T data = dirtyData.getData();
        if (data instanceof RowData) {
            value = format((RowData) data, dirtyData.getRowType(), labelMap);
        } else if (data instanceof JsonNode) {
            value = format((JsonNode) data, labelMap);
        } else {
            // Only support csv format when the row is not a 'RowData' and 'JsonNode'
            value = FormatUtils.csvFormat(data, labelMap, s3Options.getFieldDelimiter());
        }
        if (s3Options.enableDirtyLog()) {
            LOGGER.info("[{}] {}", dirtyData.getLogTag(), value);
        }
        batchBytes += value.getBytes(UTF_8).length;
        size++;
        batchMap.computeIfAbsent(dirtyData.getIdentifier(), k -> new ArrayList<>()).add(value);
    }

    private String format(RowData data, LogicalType rowType,
            Map<String, String> labels) throws JsonProcessingException {
        String value;
        switch (s3Options.getFormat()) {
            case "csv":
                RowData.FieldGetter[] getters = fieldGetters;
                if (rowType != null) {
                    getters = FormatUtils.parseFieldGetters(rowType);
                }
                value = FormatUtils.csvFormat(data, getters, labels, s3Options.getFieldDelimiter());
                break;
            case "json":
                RowDataToJsonConverter jsonConverter = converter;
                if (rowType != null) {
                    jsonConverter = FormatUtils.parseRowDataToJsonConverter(rowType);
                }
                value = FormatUtils.jsonFormat(data, jsonConverter, labels);
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Unsupported format for: %s", s3Options.getFormat()));
        }
        return value;
    }

    private String format(JsonNode data, Map<String, String> labels) throws JsonProcessingException {
        String value;
        switch (s3Options.getFormat()) {
            case "csv":
                value = FormatUtils.csvFormat(data, labels, s3Options.getFieldDelimiter());
                break;
            case "json":
                value = FormatUtils.jsonFormat(data, labels);
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Unsupported format for: %s", s3Options.getFormat()));
        }
        return value;
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            if (this.scheduledFuture != null) {
                scheduledFuture.cancel(false);
                this.scheduler.shutdown();
            }
            try {
                flush();
            } catch (Exception e) {
                LOGGER.warn("Writing records to s3 failed.", e);
                throw new RuntimeException("Writing records to s3 failed.", e);
            }
        }
    }

    /**
     * Flush data to s3
     */
    public synchronized void flush() {
        flushing = true;
        if (!hasRecords()) {
            flushing = false;
            return;
        }
        for (Entry<String, List<String>> kvs : batchMap.entrySet()) {
            flushSingleIdentifier(kvs.getKey(), kvs.getValue());
        }
        batchMap.clear();
        batchBytes = 0;
        size = 0;
        flushing = false;
        LOGGER.info("S3 dirty sink statistics: readInNum: {}, writeOutNum: {}, errorNum: {}",
                readInNum.get(), writeOutNum.get(), errorNum.get());
    }

    /**
     * Flush data of single identifier to s3
     *
     * @param identifier The identifier of dirty data
     * @param values The values of the identifier
     */
    private void flushSingleIdentifier(String identifier, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        String content = null;
        try {
            content = StringUtils.join(values, StringEscapeUtils.unescapeJava(s3Options.getLineDelimiter()));
            s3Helper.upload(identifier, content);
            LOGGER.info("Write {} records to s3 of identifier: {}", values.size(), identifier);
            writeOutNum.addAndGet(values.size());
            // Clean the data that has been loaded.
            values.clear();
        } catch (Exception e) {
            errorNum.addAndGet(values.size());
            if (!s3Options.ignoreSideOutputErrors()) {
                throw new RuntimeException(
                        String.format("Writing records to s3 of identifier:%s failed, the value: %s.",
                                identifier, content),
                        e);
            }
            LOGGER.warn("Writing records to s3 of identifier:{} failed "
                    + "and the dirty data will be throw away in the future"
                    + " because the option 'dirty.side-output.ignore-errors' is 'true'", identifier);
        }
    }

    private boolean hasRecords() {
        if (batchMap.isEmpty()) {
            return false;
        }
        for (List<String> value : batchMap.values()) {
            if (!value.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
