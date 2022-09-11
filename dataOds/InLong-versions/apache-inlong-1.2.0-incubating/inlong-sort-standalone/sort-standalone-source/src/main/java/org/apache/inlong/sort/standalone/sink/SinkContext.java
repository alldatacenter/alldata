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

package org.apache.inlong.sort.standalone.sink;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigHolder;
import org.apache.inlong.sort.standalone.metrics.SortMetricItem;
import org.apache.inlong.sort.standalone.metrics.SortMetricItemSet;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * SinkContext
 */
public class SinkContext {

    public static final Logger LOG = InlongLoggerFactory.getLogger(SinkContext.class);

    public static final String KEY_MAX_THREADS = "maxThreads";
    public static final String KEY_PROCESSINTERVAL = "processInterval";
    public static final String KEY_RELOADINTERVAL = "reloadInterval";
    public static final String KEY_TASK_NAME = "taskName";

    protected final String clusterId;
    protected final String taskName;
    protected final String sinkName;
    protected final Context sinkContext;

    protected SortTaskConfig sortTaskConfig;

    protected final Channel channel;
    //
    protected final int maxThreads;
    protected final long processInterval;
    protected final long reloadInterval;
    //
    protected final SortMetricItemSet metricItemSet;
    protected Timer reloadTimer;

    /**
     * Constructor
     * 
     * @param sinkName
     * @param context
     * @param channel
     */
    public SinkContext(String sinkName, Context context, Channel channel) {
        this.sinkName = sinkName;
        this.sinkContext = context;
        this.channel = channel;
        this.clusterId = context.getString(CommonPropertiesHolder.KEY_CLUSTER_ID);
        this.taskName = context.getString(KEY_TASK_NAME);
        this.maxThreads = sinkContext.getInteger(KEY_MAX_THREADS, 10);
        this.processInterval = sinkContext.getInteger(KEY_PROCESSINTERVAL, 100);
        this.reloadInterval = sinkContext.getLong(KEY_RELOADINTERVAL, 60000L);
        //
        this.metricItemSet = new SortMetricItemSet(sinkName);
        MetricRegister.register(this.metricItemSet);
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
    protected void setReloadTimer() {
        reloadTimer = new Timer(true);
        TimerTask task = new TimerTask() {

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
            this.sortTaskConfig = SortClusterConfigHolder.getTaskConfig(taskName);
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * get clusterId
     * 
     * @return the clusterId
     */
    public String getClusterId() {
        return clusterId;
    }

    /**
     * get taskName
     * 
     * @return the taskName
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * get sinkName
     * 
     * @return the sinkName
     */
    public String getSinkName() {
        return sinkName;
    }

    /**
     * get sinkContext
     * 
     * @return the sinkContext
     */
    public Context getSinkContext() {
        return sinkContext;
    }

    /**
     * get sortTaskConfig
     * 
     * @return the sortTaskConfig
     */
    public SortTaskConfig getSortTaskConfig() {
        return sortTaskConfig;
    }

    /**
     * get channel
     * 
     * @return the channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * get maxThreads
     * 
     * @return the maxThreads
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * get processInterval
     * 
     * @return the processInterval
     */
    public long getProcessInterval() {
        return processInterval;
    }

    /**
     * get reloadInterval
     * 
     * @return the reloadInterval
     */
    public long getReloadInterval() {
        return reloadInterval;
    }

    /**
     * get metricItemSet
     * 
     * @return the metricItemSet
     */
    public SortMetricItemSet getMetricItemSet() {
        return metricItemSet;
    }

    /**
     * fillInlongId
     *
     * @param currentRecord
     * @param dimensions
     */
    public static void fillInlongId(ProfileEvent currentRecord, Map<String, String> dimensions) {
        String inlongGroupId = currentRecord.getInlongGroupId();
        inlongGroupId = (StringUtils.isBlank(inlongGroupId)) ? "-" : inlongGroupId;
        String inlongStreamId = currentRecord.getInlongStreamId();
        inlongStreamId = (StringUtils.isBlank(inlongStreamId)) ? "-" : inlongStreamId;
        dimensions.put(SortMetricItem.KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(SortMetricItem.KEY_INLONG_STREAM_ID, inlongStreamId);
    }
}
