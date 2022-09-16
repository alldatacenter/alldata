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

package org.apache.inlong.sort.standalone.source.sortsdk;

import org.apache.commons.lang3.ClassUtils;
import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.source.AbstractSource;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.sdk.commons.admin.AdminServiceRegister;
import org.apache.inlong.sdk.sort.api.QueryConsumeConfig;
import org.apache.inlong.sdk.sort.api.SortClient;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.api.SortClientFactory;
import org.apache.inlong.sdk.sort.impl.ManagerReportHandlerImpl;
import org.apache.inlong.sdk.sort.impl.MetricReporterImpl;
import org.apache.inlong.sort.standalone.admin.ConsumerServiceMBean;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.config.holder.ManagerUrlHandler;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigHolder;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigType;
import org.apache.inlong.sort.standalone.config.holder.SortSourceConfigType;
import org.apache.inlong.sort.standalone.config.loader.ClassResourceQueryConsumeConfig;
import org.apache.inlong.sort.standalone.utils.FlumeConfigGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default Source implementation of InLong.
 *
 * <p>
 * SortSdkSource acquired msg from different upstream data store by register {@link SortClient} for each sort task. The
 * only things SortSdkSource should do is to get one client by the sort task id, or remove one client when the task is
 * finished or schedule to other source instance.
 * </p>
 *
 * <p>
 * The Default Manager of InLong will schedule the partition and topic automatically.
 * </p>
 *
 * <p>
 * Because all sources should implement {@link Configurable}, the SortSdkSource should have default constructor
 * <b>WITHOUT</b> any arguments, and parameters will be configured by {@link Configurable#configure(Context)}.
 * </p>
 */
public final class SortSdkSource extends AbstractSource
        implements Configurable, Runnable, EventDrivenSource, ConsumerServiceMBean {

    // Log of {@link SortSdkSource}.
    private static final Logger LOG = LoggerFactory.getLogger(SortSdkSource.class);

    public static final String SORT_SDK_PREFIX = "sortsdk.";

    // Default pool of {@link ScheduledExecutorService}.
    private static final int CORE_POOL_SIZE = 1;

    // Default consume strategy of {@link SortClient}.
    private static final SortClientConfig.ConsumeStrategy defaultStrategy = SortClientConfig.ConsumeStrategy.lastest;

    private static final String KEY_SORT_SDK_CLIENT_NUM = "sortSdkClientNum";
    private static final int DEFAULT_SORT_SDK_CLIENT_NUM = 1;

    private String taskName;

    // Context of SortSdkSource.
    private SortSdkSourceContext context;

    // The cluster name of sort.
    private String sortClusterName;

    // Reload config interval.
    private long reloadInterval;

    // Executor for config reloading.
    private ScheduledExecutorService pool;

    // {@link SortClient}.
    private List<SortClient> sortClients = new ArrayList<>();

    /**
     * Start SortSdkSource.
     */
    @Override
    public synchronized void start() {
        int sortSdkClientNum = CommonPropertiesHolder.getInteger(KEY_SORT_SDK_CLIENT_NUM, DEFAULT_SORT_SDK_CLIENT_NUM);
        LOG.info("start to SortSdkSource:{}, client num is {}", taskName, sortSdkClientNum);
        for (int i = 0; i < sortSdkClientNum; i++) {
            this.sortClients.add(this.newClient(taskName));
        }
    }

    /**
     * Stop {@link #pool} and close all {@link SortClient}.
     */
    @Override
    public void stop() {
        pool.shutdownNow();
        LOG.info("Close sort client {}.", taskName);
        for (SortClient sortClient : sortClients) {
            sortClient.getConfig().setStopConsume(true);
            sortClient.close();
        }
    }

    /**
     * Entrance of {@link #pool} to reload clients with fix rate {@link #reloadInterval}.
     */
    @Override
    public void run() {
        LOG.info("start to reload SortSdkSource:{}", taskName);
        for (SortClient sortClient : sortClients) {
            sortClient.getConfig().setManagerApiUrl(ManagerUrlHandler.getSortSourceConfigUrl());
        }
    }

    /**
     * Configure parameters.
     *
     * @param context Context of source.
     */
    @Override
    public void configure(Context context) {
        this.taskName = context.getString(FlumeConfigGenerator.KEY_TASK_NAME);
        this.context = new SortSdkSourceContext(getName(), context);
        this.sortClusterName = SortClusterConfigHolder.getClusterConfig().getClusterName();
        this.reloadInterval = this.context.getReloadInterval();
        this.initReloadExecutor();
        // register
        AdminServiceRegister.register(ConsumerServiceMBean.MBEAN_TYPE, taskName, this);
    }

    /**
     * Init ScheduledExecutorService with fix reload rate {@link #reloadInterval}.
     */
    private void initReloadExecutor() {
        this.pool = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
        pool.scheduleAtFixedRate(this, reloadInterval, reloadInterval, TimeUnit.SECONDS);
    }

    /**
     * Create one {@link SortClient} with specific sort task.
     *
     * <p>
     * In current version, the {@link FetchCallback} will hold the client to ACK. For more details see
     * {@link FetchCallback#onFinished}
     * </p>
     *
     * @param  sortTaskName Sort in of new client.
     * @return        New sort client.
     */
    private SortClient newClient(final String sortTaskName) {
        LOG.info("Start to new sort client for task: {}", sortTaskName);
        try {
            final SortClientConfig clientConfig = new SortClientConfig(sortTaskName, this.sortClusterName,
                    new DefaultTopicChangeListener(),
                    SortSdkSource.defaultStrategy, InetAddress.getLocalHost().getHostAddress());
            final FetchCallback callback = FetchCallback.Factory.create(sortTaskName, getChannelProcessor(), context);
            clientConfig.setCallback(callback);
            Map<String, String> sortSdkParams = this.getSortClientConfigParameters();
            clientConfig.setParameters(sortSdkParams);

            // create SortClient
            String configType = CommonPropertiesHolder
                    .getString(SortSourceConfigType.KEY_TYPE, SortSourceConfigType.MANAGER.name());
            SortClient client = null;
            if (SortClusterConfigType.FILE.name().equalsIgnoreCase(configType)) {
                LOG.info("Create sort sdk client in file way:{}", configType);
                ClassResourceQueryConsumeConfig queryConfig = new ClassResourceQueryConsumeConfig();
                client = SortClientFactory.createSortClient(clientConfig,
                        queryConfig,
                        new MetricReporterImpl(clientConfig),
                        new ManagerReportHandlerImpl());
            } else if (SortClusterConfigType.MANAGER.name().equalsIgnoreCase(configType)) {
                LOG.info("Create sort sdk client in manager way:{}", configType);
                clientConfig.setManagerApiUrl(ManagerUrlHandler.getSortSourceConfigUrl());
                client = SortClientFactory.createSortClient(clientConfig);
            } else {
                LOG.info("Create sort sdk client in custom way:{}", configType);
                // user-defined
                Class<?> loaderClass = ClassUtils.getClass(configType);
                Object loaderObject = loaderClass.getDeclaredConstructor().newInstance();
                if (loaderObject instanceof Configurable) {
                    ((Configurable) loaderObject).configure(new Context(CommonPropertiesHolder.get()));
                }
                if (!(loaderObject instanceof QueryConsumeConfig)) {
                    LOG.error("Got exception when create QueryConsumeConfig instance,config key:{},config class:{}",
                            SortSourceConfigType.KEY_TYPE, configType);
                    return null;
                }
                // if it specifies the type of QueryConsumeConfig.
                client = SortClientFactory.createSortClient(clientConfig,
                        (QueryConsumeConfig) loaderObject,
                        new MetricReporterImpl(clientConfig),
                        new ManagerReportHandlerImpl());
            }

            // init
            client.init();
            // temporary use to ACK fetched msg.
            callback.setClient(client);
            return client;
        } catch (UnknownHostException ex) {
            LOG.error("Got one UnknownHostException when init client of id:{}", sortTaskName, ex);
        } catch (Throwable th) {
            LOG.error("Got one throwable when init client of id:{}", sortTaskName, th);
        }
        return null;
    }

    /**
     * getSortClientConfigParameters
     * @return Map
     */
    private Map<String, String> getSortClientConfigParameters() {
        Map<String, String> commonParams = CommonPropertiesHolder.getContext().getSubProperties(SORT_SDK_PREFIX);
        Map<String, String> sortSdkParams = new HashMap<>(commonParams);
        SortTaskConfig taskConfig = SortClusterConfigHolder.getTaskConfig(taskName);
        if (taskConfig != null) {
            Map<String, String> sinkParams = taskConfig.getSinkParams();
            if (sinkParams != null) {
                Context sinkContext = new Context(sinkParams);
                sortSdkParams.putAll(sinkContext.getSubProperties(SORT_SDK_PREFIX));
            }
        }
        return sortSdkParams;
    }

    /**
     * stopConsumer
     */
    @Override
    public void stopConsumer() {
        for (SortClient sortClient : sortClients) {
            sortClient.getConfig().setStopConsume(true);
        }
    }

    /**
     * recoverConsumer
     */
    @Override
    public void recoverConsumer() {
        for (SortClient sortClient : sortClients) {
            sortClient.getConfig().setStopConsume(false);
        }
    }
}
