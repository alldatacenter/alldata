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

package org.apache.inlong.dataproxy.config.holder;

import static org.apache.inlong.dataproxy.config.loader.CacheClusterConfigLoader.CACHE_CLUSTER_CONFIG_TYPE;
import static org.apache.inlong.dataproxy.config.loader.ConfigLoader.RELOAD_INTERVAL;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.ClassUtils;
import org.apache.flume.Context;
import org.apache.flume.conf.Configurable;
import org.apache.inlong.dataproxy.config.loader.CacheClusterConfigLoader;
import org.apache.inlong.dataproxy.config.loader.ContextCacheClusterConfigLoader;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * CacheClusterConfigHolder
 */
public class CacheClusterConfigHolder implements Configurable {

    public static final Logger LOG = LoggerFactory.getLogger(CacheClusterConfigHolder.class);

    protected Context context;
    private long reloadInterval;
    private Timer reloadTimer;
    private CacheClusterConfigLoader loader;

    private List<CacheClusterConfig> configList;

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        this.context = context;
        this.reloadInterval = context.getLong(RELOAD_INTERVAL, 60000L);
        String loaderType = context.getString(CACHE_CLUSTER_CONFIG_TYPE,
                ContextCacheClusterConfigLoader.class.getName());
        LOG.info("Init CacheClusterConfigLoader,loaderType:{}", loaderType);
        try {
            Class<?> loaderClass = ClassUtils.getClass(loaderType);
            Object loaderObject = loaderClass.getDeclaredConstructor().newInstance();
            if (loaderObject instanceof CacheClusterConfigLoader) {
                this.loader = (CacheClusterConfigLoader) loaderObject;
            }
        } catch (Throwable t) {
            LOG.error("Fail to init loader,loaderType:{},error:{}", loaderType, t.getMessage());
            LOG.error(t.getMessage(), t);
        }
        if (this.loader == null) {
            this.loader = new ContextCacheClusterConfigLoader();
        }
        this.loader.configure(context);
    }

    /**
     * start
     */
    public void start() {
        try {
            this.reload();
            this.setReloadTimer();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * close
     */
    public void close() {
        try {
            this.reloadTimer.cancel();
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
                reload();
            }
        };
        reloadTimer.schedule(task, new Date(System.currentTimeMillis() + reloadInterval), reloadInterval);
    }

    /**
     * reload
     */
    public void reload() {
        try {
            this.configList = this.loader.load();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * get configList
     * 
     * @return the configList
     */
    public List<CacheClusterConfig> getConfigList() {
        return configList;
    }

}
