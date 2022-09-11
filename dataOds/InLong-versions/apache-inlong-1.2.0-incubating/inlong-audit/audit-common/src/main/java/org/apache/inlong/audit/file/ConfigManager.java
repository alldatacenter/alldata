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

package org.apache.inlong.audit.file;

import com.google.gson.Gson;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.audit.file.holder.PropertiesConfigHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    private static final Map<String, ConfigHolder> holderMap =
            new ConcurrentHashMap<>();

    private static ConfigManager instance = null;

    private static String DEFAULT_CONFIG_PROPERTIES = "server.properties";

    static {
        instance = getInstance(DEFAULT_CONFIG_PROPERTIES, true);
        ReloadConfigWorker reloadProperties = new ReloadConfigWorker(instance);
        reloadProperties.setDaemon(true);
        reloadProperties.start();
    }

    public static ConfigManager getInstance() {
        return instance;
    }

    /**
     * get instance for manager
     * @return
     */
    public static ConfigManager getInstance(String fileName, boolean needToCheckChanged) {
        synchronized (ConfigManager.class) {
            if (instance == null) {
                instance = new ConfigManager();
            }
            ConfigHolder holder = holderMap.get(fileName);
            if (holder == null) {
                holder = new PropertiesConfigHolder(fileName, needToCheckChanged);
                holder.loadFromFileToHolder();
                holderMap.putIfAbsent(fileName, holder);
            }
        }
        return instance;
    }

    public Map<String, String> getProperties(String fileName) {
        ConfigHolder holder = holderMap.get(fileName);
        if (holder != null) {
            return holder.getHolder();
        }
        return null;
    }

    private boolean updatePropertiesHolder(Map<String, String> result,
            String holderName, boolean addElseRemove) {
        if (StringUtils.isNotEmpty(holderName)) {
            PropertiesConfigHolder holder = (PropertiesConfigHolder)
                    holderMap.get(holderName + ".properties");
            return updatePropertiesHolder(result, holder, true);
        }
        return true;
    }

    /**
     * update old maps, reload local files if changed.
     *
     * @param result        - map pending to be added
     * @param holder        - property holder
     * @param addElseRemove - if add(true) else remove(false)
     * @return true if changed else false.
     */
    private boolean updatePropertiesHolder(Map<String, String> result,
                                           PropertiesConfigHolder holder, boolean addElseRemove) {
        Map<String, String> tmpHolder = holder.forkHolder();
        boolean changed = false;
        for (Entry<String, String> entry : result.entrySet()) {
            String oldValue = addElseRemove
                    ? tmpHolder.put(entry.getKey(), entry.getValue()) : tmpHolder.remove(entry.getKey());
            // if addElseRemove is false, that means removing item, changed is true.
            if (oldValue == null || !oldValue.equals(entry.getValue()) || !addElseRemove) {
                changed = true;
            }
        }

        if (changed) {
            return holder.loadFromHolderToFile(tmpHolder);
        } else {
            return false;
        }
    }

    public ConfigHolder getDefaultConfigHolder() {
        return holderMap.get(DEFAULT_CONFIG_PROPERTIES);
    }

    public ConfigHolder getConfigHolder(String fileName) {
        return holderMap.get(fileName);
    }

    /**
     * load worker
     */
    private static class ReloadConfigWorker extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(ReloadConfigWorker.class);
        private final ConfigManager configManager;
        private final CloseableHttpClient httpClient;
        private final Gson gson = new Gson();
        private boolean isRunning = true;

        public ReloadConfigWorker(ConfigManager managerInstance) {
            this.configManager = managerInstance;
            this.httpClient = constructHttpClient();
        }

        private synchronized CloseableHttpClient constructHttpClient() {
            long timeoutInMs = TimeUnit.MILLISECONDS.toMillis(50000);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout((int) timeoutInMs)
                    .setSocketTimeout((int) timeoutInMs).build();
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            httpClientBuilder.setDefaultRequestConfig(requestConfig);
            return httpClientBuilder.build();
        }

        public int getRandom(int min, int max) {
            return (int) (Math.random() * (max + 1 - min)) + min;
        }

        private long getSleepTime() {
            String sleepTimeInMsStr =
                    configManager.getProperties(DEFAULT_CONFIG_PROPERTIES).get(
                            "configCheckIntervalMs");
            long sleepTimeInMs = 10000;
            try {
                if (sleepTimeInMsStr != null) {
                    sleepTimeInMs = Long.parseLong(sleepTimeInMsStr);
                }
            } catch (Exception ignored) {
                LOG.info("ignored Exception ", ignored);
            }
            return sleepTimeInMs + getRandom(0, 5000);
        }

        public void close() {
            isRunning = false;
        }

        private void checkLocalFile() {
            for (ConfigHolder holder : holderMap.values()) {
                boolean isChanged = holder.checkAndUpdateHolder();
                if (isChanged) {
                    holder.executeCallbacks();
                }
            }
        }

        private boolean checkWithManager(String hostUrl) {
            HttpGet httpGet = null;
            try {
                String url = "http://" + hostUrl + "/api/inlong/manager/openapi/audit/getConfig";
                LOG.info("start to request {} to get config info", url);
                httpGet = new HttpGet(url);
                httpGet.addHeader(HttpHeaders.CONNECTION, "close");

                // request with post
                CloseableHttpResponse response = httpClient.execute(httpGet);
                String returnStr = EntityUtils.toString(response.getEntity());
                // get groupId <-> topic and m value.

                Map<String, String> configJsonMap = gson.fromJson(returnStr, Map.class);
                if (configJsonMap != null && configJsonMap.size() > 0) {
                    for (Entry<String, String> entry : configJsonMap.entrySet()) {
                        Map<String, String> valueMap = gson.fromJson(entry.getValue(), Map.class);
                        configManager.updatePropertiesHolder(valueMap,
                                entry.getKey(), true);
                    }
                }
            } catch (Exception ex) {
                LOG.error("exception caught", ex);
                return false;
            } finally {
                if (httpGet != null) {
                    httpGet.releaseConnection();
                }
            }
            return true;
        }

        private void checkRemoteConfig() {

            try {
                String managerHosts = configManager.getProperties(DEFAULT_CONFIG_PROPERTIES).get("manager_hosts");
                String[] hostList = StringUtils.split(managerHosts, ",");
                for (String host : hostList) {
                    if (checkWithManager(host)) {
                        break;
                    }
                }
            } catch (Exception ex) {
                LOG.error("exception caught", ex);
            }
        }

        @Override
        public void run() {
            long count = 0;
            while (isRunning) {

                long sleepTimeInMs = getSleepTime();
                count += 1;
                try {
                    checkLocalFile();
                    // wait for 30 seconds to update remote config
                    if (count % 3 == 0) {
                        checkRemoteConfig();
                        count = 0;
                    }
                    TimeUnit.MILLISECONDS.sleep(sleepTimeInMs);
                } catch (Exception ex) {
                    LOG.error("exception caught", ex);
                }
            }
        }
    }
}
