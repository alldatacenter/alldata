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

package org.apache.inlong.audit.file.holder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.io.FileUtils;
import org.apache.inlong.audit.file.ConfigHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * properties to map
 */
public class PropertiesConfigHolder extends ConfigHolder {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesConfigHolder.class);
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Map<String, String> holder;

    public PropertiesConfigHolder(String fileName, boolean needToCheckChanged) {
        super(fileName, needToCheckChanged);
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
     *
     * @return
     */
    public Map<String, String> forkHolder() {
        Map<String, String> tmpHolder = new HashMap<String, String>();
        if (holder != null) {
            for (Map.Entry<String, String> entry : holder.entrySet()) {
                tmpHolder.put(entry.getKey(), entry.getValue());
            }
        }
        return tmpHolder;
    }

    private List<String> getStringListFromHolder(Map<String, String> tmpHolder) {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, String> entry : tmpHolder.entrySet()) {
            result.add(entry.getKey() + "=" + entry.getValue());
        }
        return result;
    }

    /**
     * load from holder
     * @param tmpHolder
     * @return
     */
    public boolean loadFromHolderToFile(Map<String, String> tmpHolder) {
        readWriteLock.writeLock().lock();
        boolean isSuccess = false;
        try {
            File sourceFile = new File(getFilePath());
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
        Map<String, String> result = new HashMap<String, String>();
        InputStream inStream = null;
        try {
            URL url = getClass().getClassLoader().getResource(getFileName());
            inStream = url != null ? url.openStream() : null;

            if (inStream == null) {
                LOG.error("InputStream {} is null!", getFileName());
            }
            Properties props = new Properties();
            props.load(inStream);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                result.put((String) entry.getKey(), (String) entry.getValue());
            }
        } catch (UnsupportedEncodingException e) {
            LOG.error("fail to load properties, file ={}, and e= {}", getFileName(), e);
        } catch (Exception e) {
            LOG.error("fail to load properties, file ={}, and e= {}", getFileName(), e);
        } finally {
            if (null != inStream) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    LOG.error("fail to loadTopics, inStream.close ,and e= {}", getFileName(), e);
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, String> getHolder() {
        return holder;
    }

}
