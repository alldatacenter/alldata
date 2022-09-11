/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.pb.config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.ClassUtils;
import org.apache.flume.Context;
import org.apache.inlong.sdk.commons.protocol.InlongId;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.InlongStreamConfig;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyClusterConfig;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyClusterResult;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProxyClusterConfigHolder
 */
public class ProxyClusterConfigHolder {

    public static final Logger LOG = LoggerFactory.getLogger(ProxyClusterConfigHolder.class);

    private static ProxyClusterConfigHolder instance;

    protected Context context;
    private long reloadInterval;
    private Timer reloadTimer;
    private ProxyClusterConfigLoader loader;

    // Map<inlongGroupId+inlongStreamId, ProxyClusterResult>
    private Map<String, ProxyClusterResult> inlongStreamMap = new ConcurrentHashMap<>();
    // Map<proxyClusterId, ProxyInfo>
    private Map<String, ProxyClusterResult> proxyClusterMap = new ConcurrentHashMap<>();

    /**
     * Constructor
     */
    private ProxyClusterConfigHolder() {

    }

    /**
     * configure
     * 
     * @param context
     */
    private void configure(Context context) {
        this.context = context;
        this.reloadInterval = context.getLong(ProxyClusterConfigLoader.KEY_RELOAD_INTERVAL, 60000L);
        String strLoaderType = context.getString(ProxyClusterConfigLoader.KEY_LOADER_TYPE);
        LoaderType loaderType = LoaderType.valueOf(strLoaderType);
        switch (loaderType) {
            case File :
                this.loader = new FileProxyClusterConfigLoader();
                break;
            case Manager :
                this.loader = new ManagerProxyClusterConfigLoader();
                break;
            case Plugin :
                try {
                    String strLoaderClass = context.getString(ProxyClusterConfigLoader.KEY_LOADER_TYPE_PLUGIN_CLASS);
                    Class<?> loaderClass = ClassUtils.getClass(strLoaderClass);
                    Object loaderObject = loaderClass.getDeclaredConstructor().newInstance();
                    if (loaderObject instanceof ManagerProxyClusterConfigLoader) {
                        this.loader = (ManagerProxyClusterConfigLoader) loaderObject;
                    }
                } catch (Throwable t) {
                    LOG.error("Fail to init loader,loaderType:{},error:{}", loaderType, t);
                }
                break;
            case Context :
            default :
                this.loader = new ContextProxyClusterConfigLoader();
                break;
        }
        this.loader.configure(context);
    }

    /**
     * start
     */
    public static void start(Context context) {
        if (instance != null) {
            return;
        }
        synchronized (ProxyClusterConfigHolder.class) {
            try {
                if (instance != null) {
                    return;
                }
                instance = new ProxyClusterConfigHolder();
                instance.configure(context);
                instance.reload();
                instance.setReloadTimer();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * close
     */
    public static void close() {
        try {
            instance.reloadTimer.cancel();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * setReloadTimer
     */
    private void setReloadTimer() {
        reloadTimer = new Timer(true);
        TimerTask task = new TimerTask() {

            /**
             * run
             */
            public void run() {
                try {
                    reload();
                } catch (Throwable e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        };
        reloadTimer.schedule(task, new Date(System.currentTimeMillis() + reloadInterval), reloadInterval);
    }

    /**
     * reload
     */
    private void reload() {
        // prepare parameters
        Map<String, ProxyClusterResult> currentProxyMap = this.proxyClusterMap;
        List<ProxyInfo> proxys = new ArrayList<>(currentProxyMap.size());
        for (Entry<String, ProxyClusterResult> entry : currentProxyMap.entrySet()) {
            ProxyInfo proxyInfo = new ProxyInfo();
            proxyInfo.setClusterId(entry.getKey());
            proxyInfo.setMd5(entry.getValue().getMd5());
        }
        // load
        Map<String, ProxyClusterResult> newResultMap = this.loader.loadByClusterIds(proxys);
        if (newResultMap == null) {
            return;
        }
        // parse proxy
        Map<String, ProxyClusterResult> newProxyMap = new ConcurrentHashMap<>();
        for (Entry<String, ProxyClusterResult> entry : newResultMap.entrySet()) {
            if (entry.getValue().isHasUpdated()) {
                newProxyMap.put(entry.getKey(), entry.getValue());
            } else {
                ProxyClusterResult oldResult = currentProxyMap.get(entry.getKey());
                if (oldResult != null) {
                    newProxyMap.put(oldResult.getClusterId(), oldResult);
                }
            }
        }
        // parse stream
        Map<String, ProxyClusterResult> newStreamMap = new ConcurrentHashMap<>();
        for (Entry<String, ProxyClusterResult> entry : newProxyMap.entrySet()) {
            ProxyClusterResult result = entry.getValue();
            for (InlongStreamConfig stream : result.getConfig().getInlongStreamList()) {
                String newUid = InlongId.generateUid(stream.getInlongGroupId(), stream.getInlongStreamId());
                newStreamMap.put(newUid, result);
            }
        }
        this.proxyClusterMap = newProxyMap;
        this.inlongStreamMap = newStreamMap;
    }

    /**
     * getConfigByStream
     * 
     * @param  inlongGroupId
     * @param  inlongStreamId
     * @return
     */
    public static ProxyClusterConfig getConfigByStream(String inlongGroupId, String inlongStreamId) {
        String uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
        Map<String, ProxyClusterResult> currentStreamMap = instance.inlongStreamMap;
        ProxyClusterResult result = currentStreamMap.get(uid);
        if (result != null) {
            return result.getConfig();
        }
        // query from loader
        result = instance.loader.loadByStream(inlongGroupId, inlongStreamId);
        if (result == null || result.getConfig() == null) {
            result = new ProxyClusterResult();
            result.setConfig(null);
            currentStreamMap.put(uid, result);
        } else {
            for (InlongStreamConfig stream : result.getConfig().getInlongStreamList()) {
                String newUid = InlongId.generateUid(stream.getInlongGroupId(), stream.getInlongStreamId());
                currentStreamMap.put(newUid, result);
            }
            Map<String, ProxyClusterResult> newProxyMap = new ConcurrentHashMap<>();
            newProxyMap.putAll(instance.proxyClusterMap);
            newProxyMap.put(result.getClusterId(), result);
            instance.proxyClusterMap = newProxyMap;
        }
        return result.getConfig();
    }

    /**
     * getConfigByClusterId
     * 
     * @param  clusterId
     * @return
     */
    public static ProxyClusterConfig getConfigByClusterId(String clusterId) {
        ProxyClusterResult result = instance.proxyClusterMap.get(clusterId);
        if (result != null) {
            return result.getConfig();
        }
        return null;
    }

    /**
     * get inlongStreamMap
     * 
     * @return the inlongStreamMap
     */
    public static Map<String, ProxyClusterResult> getInlongStreamMap() {
        return instance.inlongStreamMap;
    }

    /**
     * get proxyClusterMap
     * 
     * @return the proxyClusterMap
     */
    public static Map<String, ProxyClusterResult> getProxyClusterMap() {
        return instance.proxyClusterMap;
    }

}
