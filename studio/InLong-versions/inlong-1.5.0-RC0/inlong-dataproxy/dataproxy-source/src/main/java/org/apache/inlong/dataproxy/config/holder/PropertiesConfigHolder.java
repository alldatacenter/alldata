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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.dataproxy.config.ConfigHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * properties to map
 */
public class PropertiesConfigHolder extends ConfigHolder {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesConfigHolder.class);
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Map<String, String> holder;

    public PropertiesConfigHolder(String fileName) {
        super(fileName);
    }

    @Override
    public void loadFromFileToHolder() {
        readWriteLock.readLock().lock();
        try {
            Map<String, String> tmpHolder = loadProperties();
            LOG.info(getFileName() + " load content {}", tmpHolder);
            holder = tmpHolder;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * holder
     */
    public Map<String, String> forkHolder() {
        Map<String, String> tmpHolder = new HashMap<>();
        if (holder != null) {
            tmpHolder.putAll(holder);
        }
        return tmpHolder;
    }

    private List<String> getStringListFromHolder(Map<String, String> tmpHolder) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : tmpHolder.entrySet()) {
            result.add(entry.getKey() + "=" + entry.getValue());
        }
        return result;
    }

    /**
     * load from holder
     */
    public boolean loadFromHolderToFile(Map<String, String> tmpHolder) {
        readWriteLock.writeLock().lock();
        boolean isSuccess = false;
        String filePath = getFilePath();
        if (StringUtils.isBlank(filePath)) {
            LOG.error("error in writing file as the file path is null.");
        }
        try {
            File sourceFile = new File(filePath);
            File targetFile = new File(getNextBackupFileName());
            File tmpNewFile = new File(getFileName() + ".tmp");

            if (sourceFile.exists()) {
                FileUtils.copyFile(sourceFile, targetFile);
            }
            List<String> lines = getStringListFromHolder(tmpHolder);
            FileUtils.writeLines(tmpNewFile, lines);

            FileUtils.copyFile(tmpNewFile, sourceFile);
            tmpNewFile.delete();
            isSuccess = true;
            getFileChanged().set(true);
        } catch (Exception ex) {
            LOG.error("error in writing file", ex);
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return isSuccess;
    }

    protected Map<String, String> loadProperties() {
        Map<String, String> result = new HashMap<>();
        String fileName = getFileName();
        if (StringUtils.isBlank(fileName)) {
            LOG.error("fail to load properties as the file name is null.");
            return result;
        }

        InputStream inStream = null;
        try {
            URL url = getClass().getClassLoader().getResource(fileName);
            inStream = url != null ? url.openStream() : null;
            if (inStream == null) {
                LOG.error("fail to load properties from {} as the input stream is null", fileName);
                return result;
            }

            Properties props = new Properties();
            props.load(inStream);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                result.put((String) entry.getKey(), (String) entry.getValue());
            }
        } catch (Exception e) {
            LOG.error("fail to load properties, file ={}, and e= {}", fileName, e);
        } finally {
            if (null != inStream) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    LOG.error("fail to loadTopics in inStream.close for file: {}", fileName, e);
                }
            }
        }
        return result;
    }

    public Map<String, String> getHolder() {
        return holder;
    }
}
