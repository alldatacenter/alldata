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

package org.apache.inlong.sort.redis.common.config;

import org.apache.flink.annotation.Internal;

import java.io.Serializable;
import java.util.Objects;

/**
 * Options for the Redis lookup.
 */
@Internal
public class RedisLookupOptions implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;

    private final long cacheMaxSize;
    private final long cacheExpireMs;
    private final int maxRetryTimes;
    /**
     * Asynchronous processing has not been implemented yet, but the entry is reserved
     */
    private final boolean lookupAsync;

    public RedisLookupOptions(
            long cacheMaxSize, long cacheExpireMs, int maxRetryTimes, boolean lookupAsync) {
        this.cacheMaxSize = cacheMaxSize;
        this.cacheExpireMs = cacheExpireMs;
        this.maxRetryTimes = maxRetryTimes;
        this.lookupAsync = lookupAsync;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public long getCacheExpireMs() {
        return cacheExpireMs;
    }

    public int getMaxRetryTimes() {
        return maxRetryTimes;
    }

    public boolean getLookupAsync() {
        return lookupAsync;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RedisLookupOptions) {
            RedisLookupOptions options = (RedisLookupOptions) o;
            return Objects.equals(cacheMaxSize, options.cacheMaxSize)
                    && Objects.equals(cacheExpireMs, options.cacheExpireMs)
                    && Objects.equals(maxRetryTimes, options.maxRetryTimes)
                    && Objects.equals(lookupAsync, options.lookupAsync);
        } else {
            return false;
        }
    }

    /**
     * Builder of {@link RedisLookupOptions}.
     */
    public static class Builder {

        private long cacheMaxSize = -1L;
        private long cacheExpireMs = 0L;
        private int maxRetryTimes = DEFAULT_MAX_RETRY_TIMES;
        private boolean lookupAsync = false;

        /**
         * optional, lookup cache max size, over this value, the old data will be eliminated.
         */
        public Builder setCacheMaxSize(long cacheMaxSize) {
            this.cacheMaxSize = cacheMaxSize;
            return this;
        }

        /**
         * optional, lookup cache expire mills, over this time, the old data will expire.
         */
        public Builder setCacheExpireMs(long cacheExpireMs) {
            this.cacheExpireMs = cacheExpireMs;
            return this;
        }

        /**
         * optional, max retry times for Hbase connector.
         */
        public Builder setMaxRetryTimes(int maxRetryTimes) {
            this.maxRetryTimes = maxRetryTimes;
            return this;
        }

        /**
         * optional, whether to set async lookup.
         */
        public Builder setLookupAsync(boolean lookupAsync) {
            this.lookupAsync = lookupAsync;
            return this;
        }

        public RedisLookupOptions build() {
            return new RedisLookupOptions(cacheMaxSize, cacheExpireMs, maxRetryTimes, lookupAsync);
        }
    }
}
