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

import static org.apache.inlong.dataproxy.config.loader.ConfigLoader.RELOAD_INTERVAL;
import static org.apache.inlong.dataproxy.config.loader.IdTopicConfigLoader.IDTOPIC_CONFIG_TYPE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.ClassUtils;
import org.apache.flume.Context;
import org.apache.flume.conf.Configurable;
import org.apache.inlong.dataproxy.config.loader.ContextIdTopicConfigLoader;
import org.apache.inlong.dataproxy.config.loader.IdTopicConfigLoader;
import org.apache.inlong.dataproxy.config.pojo.IdTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * IdTopicConfigHolder
 */
public class IdTopicConfigHolder implements Configurable {

    public static final Logger LOG = LoggerFactory.getLogger(IdTopicConfigHolder.class);

    protected Context context;
    private long reloadInterval;
    private Timer reloadTimer;
    private IdTopicConfigLoader loader;

    private List<IdTopicConfig> configList = new ArrayList<>();
    private Map<String, IdTopicConfig> configMap = new ConcurrentHashMap<>();

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        this.context = context;
        this.reloadInterval = context.getLong(RELOAD_INTERVAL, 60000L);
        String loaderType = context.getString(IDTOPIC_CONFIG_TYPE, ContextIdTopicConfigLoader.class.getName());
        LOG.info("Init IdTopicConfigLoader,loaderType:{}", loaderType);
        try {
            Class<?> loaderClass = ClassUtils.getClass(loaderType);
            Object loaderObject = loaderClass.getDeclaredConstructor().newInstance();
            if (loaderObject instanceof IdTopicConfigLoader) {
                this.loader = (IdTopicConfigLoader) loaderObject;
            }
        } catch (Throwable t) {
            LOG.error("Fail to init loader,loaderType:{},error:{}", loaderType, t.getMessage());
            LOG.error(t.getMessage(), t);
        }
        if (this.loader == null) {
            this.loader = new ContextIdTopicConfigLoader();
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
            List<IdTopicConfig> newConfigList = this.loader.load();
            Map<String, IdTopicConfig> newConfigMap = new ConcurrentHashMap<>();
            for (IdTopicConfig config : newConfigList) {
                newConfigMap.put(config.getUid(), config);
                config.formatTopicName();
            }
            this.configList = newConfigList;
            this.configMap = newConfigMap;
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * get configList
     * 
     * @return the configList
     */
    public List<IdTopicConfig> getConfigList() {
        return configList;
    }

    /**
     * getTopic
     * 
     * @param  uid
     * @return
     */
    public String getTopic(String uid) {
        IdTopicConfig config = this.configMap.get(uid);
        if (config != null) {
            return config.getTopicName();
        }
        return null;
    }
}
