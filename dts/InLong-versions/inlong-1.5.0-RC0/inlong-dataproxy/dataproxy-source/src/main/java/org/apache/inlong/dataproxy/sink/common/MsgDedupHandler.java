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

package org.apache.inlong.dataproxy.sink.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// message deduplication handler
public class MsgDedupHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(MsgDedupHandler.class);
    private static final int DEF_MAX_SURVIVE_CNT_SIZE = 5000000;
    private static final int DEF_MAX_SURVIVE_TIME_MS = 30000;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile boolean enableDataDedup;
    private LoadingCache<String, Long> msgSeqIdCache = null;

    public MsgDedupHandler() {
        enableDataDedup = false;
    }

    public void start(boolean cfgEnableFun, int cfgMaxSurviveTime, int cfgMaxSurviveSize) {
        if (this.started.compareAndSet(false, true)) {
            this.enableDataDedup = cfgEnableFun;
            int maxSurviveTime = cfgMaxSurviveTime;
            int maxSurviveSize = cfgMaxSurviveSize;
            if (this.enableDataDedup) {
                if (maxSurviveTime < 1000) {
                    maxSurviveTime = DEF_MAX_SURVIVE_TIME_MS;
                }
                if (maxSurviveSize < 0) {
                    maxSurviveSize = DEF_MAX_SURVIVE_CNT_SIZE;
                }
                msgSeqIdCache = CacheBuilder
                        .newBuilder().concurrencyLevel(4 * 8).initialCapacity(5000000)
                        .expireAfterAccess(maxSurviveTime, TimeUnit.MILLISECONDS)
                        .maximumSize(maxSurviveSize)
                        .build(new CacheLoader<String, Long>() {

                            @Override
                            public Long load(String key) {
                                return System.currentTimeMillis();
                            }
                        });
            }
            logger.info("Initial message deduplication handler, enable = "
                    + this.enableDataDedup + ", configured survived-time = "
                    + cfgMaxSurviveTime + ", valid survived-time = "
                    + maxSurviveTime + ", configured survived-size = "
                    + cfgMaxSurviveSize + ", valid survived-size = "
                    + maxSurviveSize);
        }
    }

    public void invalidMsgSeqId(String msgSeqId) {
        if (enableDataDedup && msgSeqId != null) {
            if (msgSeqIdCache.asMap().containsKey(msgSeqId)) {
                msgSeqIdCache.invalidate(msgSeqId);
            }
        }
    }

    public boolean judgeDupAndPutMsgSeqId(String msgSeqId) {
        boolean isInclude = false;
        if (enableDataDedup && msgSeqId != null) {
            isInclude = msgSeqIdCache.asMap().containsKey(msgSeqId);
            msgSeqIdCache.put(msgSeqId, System.currentTimeMillis());
        }
        return isInclude;
    }

    public String getCacheStatsInfo() {
        if (enableDataDedup) {
            return msgSeqIdCache.stats().toString();
        }
        return "Disable for message data deduplication function";
    }

    public CacheStats getCacheData() {
        if (enableDataDedup) {
            return msgSeqIdCache.stats();
        }
        return null;
    }
}
