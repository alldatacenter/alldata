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

package org.apache.inlong.sort.standalone.config.holder;

import static org.apache.inlong.sort.standalone.utils.Constants.RELOAD_INTERVAL;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Context;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterConfig;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.sort.standalone.config.loader.ClassResourceSortClusterConfigLoader;
import org.apache.inlong.sort.standalone.config.loader.ManagerSortClusterConfigLoader;
import org.apache.inlong.sort.standalone.config.loader.SortClusterConfigLoader;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * SortClusterConfigHolder
 */
public final class SortClusterConfigHolder {

    public static final Logger LOG = InlongLoggerFactory.getLogger(SortClusterConfigHolder.class);

    private static SortClusterConfigHolder instance;

    private long reloadInterval;
    private Timer reloadTimer;
    private SortClusterConfigLoader loader;
    private SortClusterConfig config;

    /**
     * Constructor
     */
    private SortClusterConfigHolder() {

    }

    /**
     * getInstance
     * 
     * @return
     */
    private static SortClusterConfigHolder get() {
        if (instance != null) {
            return instance;
        }
        synchronized (SortClusterConfigHolder.class) {
            instance = new SortClusterConfigHolder();
            instance.reloadInterval = CommonPropertiesHolder.getLong(RELOAD_INTERVAL, 60000L);
            String loaderType = CommonPropertiesHolder
                    .getString(SortClusterConfigType.KEY_TYPE, SortClusterConfigType.MANAGER.name());

            if (SortClusterConfigType.FILE.name().equalsIgnoreCase(loaderType)) {
                instance.loader = new ClassResourceSortClusterConfigLoader();
            } else if (SortClusterConfigType.MANAGER.name().equalsIgnoreCase(loaderType)) {
                instance.loader = new ManagerSortClusterConfigLoader();
            } else {
                // user-defined
                try {
                    Class<?> loaderClass = ClassUtils.getClass(loaderType);
                    Object loaderObject = loaderClass.getDeclaredConstructor().newInstance();
                    if (loaderObject instanceof SortClusterConfigLoader) {
                        instance.loader = (SortClusterConfigLoader) loaderObject;
                    }
                } catch (Throwable t) {
                    LOG.error("Fail to init loader,loaderType:{},error:{}", loaderType, t.getMessage());
                }
            }
            if (instance.loader == null) {
                instance.loader = new ClassResourceSortClusterConfigLoader();
            }
            try {
                instance.loader.configure(new Context(CommonPropertiesHolder.get()));
                instance.reload();
                instance.setReloadTimer();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return instance;
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
    private void reload() {
        try {
            SortClusterConfig newConfig = this.loader.load();
            if (newConfig != null) {
                this.config = newConfig;
            }
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * getClusterConfig
     * 
     * @return
     */
    public static SortClusterConfig getClusterConfig() {
        return get().config;
    }

    /**
     * getTaskConfig
     * 
     * @param  sortTaskName
     * @return
     */
    public static SortTaskConfig getTaskConfig(String sortTaskName) {
        SortClusterConfig config = get().config;
        if (config != null && config.getSortTasks() != null) {
            for (SortTaskConfig task : config.getSortTasks()) {
                if (StringUtils.equals(sortTaskName, task.getName())) {
                    return task;
                }
            }
        }
        return null;
    }
}
