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

package org.apache.inlong.dataproxy.config.holder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.dataproxy.config.ConfigHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * save to list
 */
public class IPVisitConfigHolder extends ConfigHolder {

    private static final Logger LOG = LoggerFactory.getLogger(IPVisitConfigHolder.class);
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final boolean isBlackList;
    private final ConcurrentHashMap<String, Long> holder = new ConcurrentHashMap<>();

    public IPVisitConfigHolder(boolean isBlackList, String fileName) {
        super(fileName);
        this.isBlackList = isBlackList;
    }

    @Override
    public boolean loadFromFileToHolder() {
        readWriteLock.writeLock().lock();
        try {
            Map<String, Long> tmpHolder = loadFile();
            if (tmpHolder == null) {
                return false;
            }
            // clear removed keys
            boolean added = false;
            boolean removed = false;
            Set<String> rmvKeys = new HashSet<>();
            for (Map.Entry<String, Long> entry : holder.entrySet()) {
                if (!tmpHolder.containsKey(entry.getKey())) {
                    rmvKeys.add(entry.getKey());
                }
            }
            for (String tmpKey : rmvKeys) {
                removed = true;
                holder.remove(tmpKey);
            }
            // add new keys
            for (Map.Entry<String, Long> entry : tmpHolder.entrySet()) {
                if (!holder.containsKey(entry.getKey())) {
                    added = true;
                    holder.put(entry.getKey(), entry.getValue());
                }
            }
            return (isBlackList && added) || (!isBlackList && removed);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public boolean isContain(String strRemoteIP) {
        if (strRemoteIP == null) {
            return false;
        }
        return holder.containsKey(strRemoteIP);
    }

    public boolean isEmptyConfig() {
        return holder.isEmpty();
    }

    public Map<String, Long> getHolder() {
        return holder;
    }

    private Map<String, Long> loadFile() {
        String filePath = getFilePath();
        if (StringUtils.isBlank(filePath)) {
            LOG.error("Fail to load " + getFileName() + " as the file path is empty");
            return null;
        }
        FileReader reader = null;
        BufferedReader br = null;
        Map<String, Long> configMap = new HashMap<>();
        try {
            String line;
            reader = new FileReader(filePath);
            br = new BufferedReader(reader);
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (StringUtils.isBlank(line)
                        || line.startsWith("#")
                        || line.startsWith(";")) {
                    continue;
                }
                configMap.put(line, System.currentTimeMillis());
            }
            return configMap;
        } catch (Throwable e) {
            LOG.error("Fail to load " + getFileName() + ", path = {}, and e = {}", filePath, e);
            return null;
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(br);
        }
    }
}
