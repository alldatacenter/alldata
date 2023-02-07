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

package org.apache.inlong.agent.cache;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * use local file as cache.
 */
public class LocalFileCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileCache.class);

    private final File file;
    private final long cacheTime;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * file with cache time in ms
     * @param file file
     * @param cacheTime cache time.
     */
    public LocalFileCache(File file, long cacheTime) {
        this.file = file;
        this.cacheTime = cacheTime;
    }

    public String getCacheInfo() {
        // lock before reading cache.
        lock.readLock().lock();
        String result = null;
        try {
            result = FileUtils.readFileToString(this.file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.error("exception on reading {}", this.file, ex);
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    /**
     * write string to cache
     */
    public void writeToCache(String info) {
        lock.writeLock().lock();
        try {
            FileUtils.writeStringToFile(this.file, info, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.error("exception on writing {}", this.file, ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * check whether cache is expired.
     * @return true if cache is expired else false.
     */
    public boolean cacheIsExpired() {
        lock.readLock().lock();
        try {
            return System.currentTimeMillis() - file.lastModified() > cacheTime;
        } finally {
            lock.readLock().unlock();
        }
    }
}
