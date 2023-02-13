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

package org.apache.inlong.sort.standalone;

import org.apache.flume.Context;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterConfig;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.sdk.commons.admin.AdminTask;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigHolder;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.inlong.sort.standalone.utils.Constants.RELOAD_INTERVAL;

/**
 * SortCluster
 */
public class SortCluster {

    public static final Logger LOG = InlongLoggerFactory.getLogger(SortCluster.class);

    private Timer reloadTimer;
    private Map<String, SortTask> taskMap = new ConcurrentHashMap<>();
    private List<SortTask> deletingTasks = new ArrayList<>();
    private AdminTask adminTask;

    /**
     * start
     */
    public void start() {
        try {
            this.reload();
            this.setReloadTimer();
            // start admin task
            this.adminTask = new AdminTask(new Context(CommonPropertiesHolder.get()));
            this.adminTask.start();
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
            // stop sort task
            for (Entry<String, SortTask> entry : this.taskMap.entrySet()) {
                entry.getValue().stop();
            }
            // stop admin task
            if (this.adminTask != null) {
                this.adminTask.stop();
            }
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
        long reloadInterval = CommonPropertiesHolder.getLong(RELOAD_INTERVAL, 60000L);
        reloadTimer.schedule(task, new Date(System.currentTimeMillis() + reloadInterval), reloadInterval);
    }

    /**
     * reload
     */
    public void reload() {
        try {
            // get new config
            SortClusterConfig newConfig = SortClusterConfigHolder.getClusterConfig();
            if (newConfig == null) {
                return;
            }
            // add new task
            for (SortTaskConfig taskConfig : newConfig.getSortTasks()) {
                String newTaskName = taskConfig.getName();
                if (taskMap.containsKey(newTaskName)) {
                    continue;
                }
                SortTask newTask = new SortTask(newTaskName);
                newTask.start();
                this.taskMap.put(newTaskName, newTask);
            }
            // remove task
            deletingTasks.clear();
            for (Entry<String, SortTask> entry : taskMap.entrySet()) {
                String taskName = entry.getKey();
                boolean isFound = false;
                for (SortTaskConfig taskConfig : newConfig.getSortTasks()) {
                    if (taskName.equals(taskConfig.getName())) {
                        isFound = true;
                        break;
                    }
                }
                if (!isFound) {
                    this.deletingTasks.add(entry.getValue());
                }
            }
            // stop deleting task list
            for (SortTask task : deletingTasks) {
                task.stop();
                taskMap.remove(task.getTaskName());
            }
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
