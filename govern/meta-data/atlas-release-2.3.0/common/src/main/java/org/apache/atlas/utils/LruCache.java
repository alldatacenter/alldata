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
package org.apache.atlas.utils;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fixed size LRU Cache.
 *
 */
public class LruCache<K, V> extends LinkedHashMap<K, V>{

    private static final long serialVersionUID = 8715233786643882558L;

    private static final Logger LOGGER = LoggerFactory.getLogger(LruCache.class.getName());

    /**
     * Specifies the number evictions that pass before a warning is logged.
     */
    private final int evictionWarningThrottle;

    // The number of evictions since the last warning was logged.
    private long evictionsSinceWarning =  0;

    // When the last eviction warning was issued.
    private Date lastEvictionWarning = new Date();

    // The maximum number of entries the cache holds.
    private final int capacity;


    /**
     *
     * @param cacheSize The size of the cache.
     * @param evictionWarningThrottle The number evictions that pass before a warning is logged.
     */
    public LruCache(int cacheSize, int evictionWarningThrottle) {
        super(cacheSize, 0.75f, true);
        this.evictionWarningThrottle = evictionWarningThrottle;
        this.capacity = cacheSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        if( size() > capacity) {
            evictionWarningIfNeeded();
            return true;
        }
        return false;
    }

    /**
     * Logs a warning if a threshold number of evictions has occurred since the
     * last warning.
     */
    private void evictionWarningIfNeeded() {
        // If not logging eviction warnings, just return.
        if (evictionWarningThrottle <= 0) {
            return;
        }

        evictionsSinceWarning++;

        if (evictionsSinceWarning >= evictionWarningThrottle) {
            DateFormat dateFormat = DateFormat.getDateTimeInstance();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("There have been " + evictionsSinceWarning
                        + " evictions from the cache since "
                        + dateFormat.format(lastEvictionWarning));
            }
            evictionsSinceWarning = 0;
            lastEvictionWarning = new Date();
        }
    }

}
