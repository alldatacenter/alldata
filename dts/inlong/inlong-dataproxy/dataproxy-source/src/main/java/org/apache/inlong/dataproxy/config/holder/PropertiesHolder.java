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

import org.apache.inlong.dataproxy.config.ConfigHolder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * properties to map
 */
public abstract class PropertiesHolder extends ConfigHolder {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesHolder.class);
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected final ConcurrentHashMap<String, String> confHolder = new ConcurrentHashMap<>();

    public PropertiesHolder(String fileName) {
        super(fileName);
    }

    public boolean fullUpdateConfigMap(Map<String, String> newConfigMap) {
        if (newConfigMap == null || newConfigMap.isEmpty()) {
            return false;
        }
        Map<String, String> filterMap = filterInValidRecords(newConfigMap);
        if (filterMap.isEmpty()) {
            LOG.info("Update properties {}, but the records are all illegal {}",
                    getFileName(), newConfigMap);
            return false;
        }
        return compAndStorePropertiesToFile(filterMap);
    }

    public boolean insertNewConfigMap(Map<String, String> insertConfigMap) {
        return insertOrRemoveProperties(true, insertConfigMap);
    }

    public boolean deleteConfigMap(Map<String, String> rmvConfigMap) {
        return insertOrRemoveProperties(false, rmvConfigMap);
    }

    protected abstract Map<String, String> filterInValidRecords(Map<String, String> configMap);

    protected abstract boolean updateCacheData();

    @Override
    protected boolean loadFromFileToHolder() {
        readWriteLock.readLock().lock();
        try {
            Map<String, String> loadMap = loadConfigFromFile();
            if (loadMap == null || loadMap.isEmpty()) {
                LOG.debug("Load changed properties {}, but no records configured", getFileName());
                return false;
            }
            // filter blank items
            Map<String, String> filteredMap = filterInValidRecords(loadMap);
            if (filteredMap.isEmpty()) {
                LOG.info("Load changed properties {}, but the records are all illegal {}",
                        getFileName(), loadMap);
                return false;
            }
            // remove records
            Set<String> rmvKeys = new HashSet<>();
            for (Map.Entry<String, String> entry : confHolder.entrySet()) {
                if (entry == null || entry.getKey() == null) {
                    continue;
                }
                if (!filteredMap.containsKey(entry.getKey())) {
                    rmvKeys.add(entry.getKey());
                }
            }
            for (String tmpKey : rmvKeys) {
                confHolder.remove(tmpKey);
            }
            // insert records
            Set<String> repKeys = new HashSet<>();
            for (Map.Entry<String, String> entry : filteredMap.entrySet()) {
                if (!entry.getValue().equals(confHolder.get(entry.getKey()))) {
                    confHolder.put(entry.getKey(), entry.getValue());
                    repKeys.add(entry.getKey());
                }
            }
            if (rmvKeys.isEmpty() && repKeys.isEmpty()) {
                return false;
            }
            // update cache data
            boolean result = updateCacheData();
            // output update result
            LOG.info("Load changed properties {}, loaded config {}, updated holder {}, updated cache {}",
                    getFileName(), loadMap, confHolder, result);
            return true;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private synchronized boolean insertOrRemoveProperties(boolean isInsert,
            Map<String, String> changeConfigMap) {
        if (changeConfigMap == null || changeConfigMap.isEmpty()) {
            return false;
        }
        Map<String, String> filteredMap = filterInValidRecords(changeConfigMap);
        if (filteredMap.isEmpty()) {
            LOG.info("Part {} properties {}, but the records are all illegal {}",
                    (isInsert ? "insert" : "remove"), getFileName(), changeConfigMap);
            return false;
        }
        boolean changed = false;
        Map<String, String> newConfigMap = forkHolder();
        if (isInsert) {
            for (Map.Entry<String, String> entry : filteredMap.entrySet()) {
                String oldValue = newConfigMap.put(entry.getKey(), entry.getValue());
                if (!ObjectUtils.equals(oldValue, entry.getValue())) {
                    changed = true;
                }
            }
        } else {
            for (Map.Entry<String, String> entry : filteredMap.entrySet()) {
                String oldValue = newConfigMap.remove(entry.getKey());
                if (oldValue != null) {
                    changed = true;
                }
            }
        }
        if (!changed) {
            return false;
        }
        return compAndStorePropertiesToFile(newConfigMap);
    }

    private boolean compAndStorePropertiesToFile(Map<String, String> newConfigMap) {
        if (newConfigMap == null || newConfigMap.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (Map.Entry<String, String> entry : newConfigMap.entrySet()) {
            if (!entry.getValue().equals(confHolder.get(entry.getKey()))) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            for (Map.Entry<String, String> entry : confHolder.entrySet()) {
                if (entry == null || entry.getKey() == null) {
                    continue;
                }
                if (!newConfigMap.containsKey(entry.getKey())) {
                    changed = true;
                    break;
                }
            }
        }
        if (!changed) {
            return false;
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : newConfigMap.entrySet()) {
            lines.add(entry.getKey() + "=" + entry.getValue());
        }
        return storeConfigToFile(lines);
    }

    /**
     * fork current cached records
     */
    private Map<String, String> forkHolder() {
        Map<String, String> tmpHolder = new HashMap<>();
        if (confHolder != null) {
            tmpHolder.putAll(confHolder);
        }
        return tmpHolder;
    }

    /**
     * load from holder
     */
    private boolean storeConfigToFile(List<String> configLines) {
        boolean isSuccess = false;
        String filePath = getFilePath();
        if (StringUtils.isBlank(filePath)) {
            LOG.error("Error in writing file {} as the file path is null.", getFileName());
            return isSuccess;
        }
        readWriteLock.writeLock().lock();
        try {
            File sourceFile = new File(filePath);
            File targetFile = new File(getNextBackupFileName());
            File tmpNewFile = new File(getFileName() + ".tmp");

            if (sourceFile.exists()) {
                FileUtils.copyFile(sourceFile, targetFile);
            }
            FileUtils.writeLines(tmpNewFile, configLines);
            FileUtils.copyFile(tmpNewFile, sourceFile);
            tmpNewFile.delete();
            isSuccess = true;
            setFileChanged();
        } catch (Throwable ex) {
            LOG.error("Error in writing file {}", getFileName(), ex);
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return isSuccess;
    }

    private Map<String, String> loadConfigFromFile() {
        Map<String, String> result = new HashMap<>();
        if (StringUtils.isBlank(getFileName())) {
            LOG.error("Fail to load properties {} as the file name is null.", getFileName());
            return result;
        }
        InputStream inStream = null;
        try {
            URL url = getClass().getClassLoader().getResource(getFileName());
            inStream = url != null ? url.openStream() : null;
            if (inStream == null) {
                LOG.error("Fail to load properties {} as the input stream is null", getFileName());
                return result;
            }
            Properties props = new Properties();
            props.load(inStream);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                result.put((String) entry.getKey(), (String) entry.getValue());
            }
        } catch (Throwable e) {
            LOG.error("Fail to load properties {}", getFileName(), e);
        } finally {
            if (null != inStream) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    LOG.error("Fail in inStream.close for file {}", getFileName(), e);
                }
            }
        }
        return result;
    }
}
