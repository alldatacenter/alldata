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

import org.apache.flink.util.Preconditions;

import java.io.Serializable;

/**
 * S3 options
 */
public class S3Options implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;
    private static final long DEFAULT_MAX_BATCH_BYTES = 1024 * 10L;
    private static final long DEFAULT_INTERVAL_MILLIS = 10000L;
    private static final String DEFAULT_FIELD_DELIMITER = ",";
    private static final String DEFAULT_LINE_DELIMITER = "\n";
    private static final String DEFAULT_FORMAT = "csv";

    private final Integer batchSize;
    private final Integer maxRetries;
    private final Long batchIntervalMs;
    private final Long maxBatchBytes;
    private final boolean ignoreSideOutputErrors;
    private final boolean enableDirtyLog;
    private final String format;
    private final String fieldDelimiter;
    private final String lineDelimiter;
    private final String endpoint;
    private final String region;
    private final String bucket;
    private final String key;
    private final String accessKeyId;
    private final String secretKeyId;

    private S3Options(Integer batchSize, Integer maxRetries, Long batchIntervalMs, Long maxBatchBytes,
            String format, boolean ignoreSideOutputErrors, boolean enableDirtyLog, String fieldDelimiter,
            String lineDelimiter, String endpoint, String region, String bucket, String key,
            String accessKeyId, String secretKeyId) {
        Preconditions.checkArgument(maxRetries >= 0);
        Preconditions.checkArgument(maxBatchBytes >= 0);
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.batchIntervalMs = batchIntervalMs;
        this.maxBatchBytes = maxBatchBytes;
        this.format = format;
        this.ignoreSideOutputErrors = ignoreSideOutputErrors;
        this.enableDirtyLog = enableDirtyLog;
        this.fieldDelimiter = fieldDelimiter;
        this.lineDelimiter = lineDelimiter;
        this.endpoint = Preconditions.checkNotNull(endpoint, "endpoint is null");
        this.region = Preconditions.checkNotNull(region, "region is null");
        this.bucket = Preconditions.checkNotNull(bucket, "bucket is null");
        this.key = Preconditions.checkNotNull(key, "key is null");
        this.accessKeyId = accessKeyId;
        this.secretKeyId = secretKeyId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public Long getBatchIntervalMs() {
        return batchIntervalMs;
    }

    public Long getMaxBatchBytes() {
        return maxBatchBytes;
    }

    public String getFormat() {
        return format;
    }

    public boolean ignoreSideOutputErrors() {
        return ignoreSideOutputErrors;
    }

    public boolean enableDirtyLog() {
        return enableDirtyLog;
    }

    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    public String getLineDelimiter() {
        return lineDelimiter;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRegion() {
        return region;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretKeyId() {
        return secretKeyId;
    }

    public static class Builder {

        private Integer batchSize = DEFAULT_BATCH_SIZE;
        private Integer maxRetries = DEFAULT_MAX_RETRY_TIMES;
        private Long batchIntervalMs = DEFAULT_INTERVAL_MILLIS;
        private Long maxBatchBytes = DEFAULT_MAX_BATCH_BYTES;
        private String format = DEFAULT_FORMAT;
        private boolean ignoreSideOutputErrors;
        private boolean enableDirtyLog;
        private String fieldDelimiter = DEFAULT_FIELD_DELIMITER;
        private String lineDelimiter = DEFAULT_LINE_DELIMITER;
        private String endpoint;
        private String region;
        private String bucket;
        private String key;
        private String accessKeyId;
        private String secretKeyId;

        public Builder setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder setBatchIntervalMs(Long batchIntervalMs) {
            this.batchIntervalMs = batchIntervalMs;
            return this;
        }

        public Builder setMaxBatchBytes(Long maxBatchBytes) {
            this.maxBatchBytes = maxBatchBytes;
            return this;
        }

        public Builder setFormat(String format) {
            this.format = format;
            return this;
        }

        public Builder setIgnoreSideOutputErrors(boolean ignoreSideOutputErrors) {
            this.ignoreSideOutputErrors = ignoreSideOutputErrors;
            return this;
        }

        public Builder setEnableDirtyLog(boolean enableDirtyLog) {
            this.enableDirtyLog = enableDirtyLog;
            return this;
        }

        public Builder setFieldDelimiter(String fieldDelimiter) {
            this.fieldDelimiter = fieldDelimiter;
            return this;
        }

        public Builder setLineDelimiter(String lineDelimiter) {
            this.lineDelimiter = lineDelimiter;
            return this;
        }

        public Builder setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder setRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder setBucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        public Builder setSecretKeyId(String secretKeyId) {
            this.secretKeyId = secretKeyId;
            return this;
        }

        public S3Options build() {
            return new S3Options(batchSize, maxRetries, batchIntervalMs, maxBatchBytes, format,
                    ignoreSideOutputErrors, enableDirtyLog, fieldDelimiter, lineDelimiter, endpoint,
                    region, bucket, key, accessKeyId, secretKeyId);
        }
    }
}
