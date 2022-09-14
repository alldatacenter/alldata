/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.scheduler;

import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.exception.StreamisScheduleException;
import org.apache.commons.lang3.StringUtils;
import org.apache.linkis.common.utils.Utils;
import org.apache.linkis.scheduler.listener.ConsumerListener;
import org.apache.linkis.scheduler.queue.*;
import org.apache.linkis.scheduler.queue.fifoqueue.FIFOGroup;
import org.apache.linkis.scheduler.queue.fifoqueue.FIFOUserConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Tenancy consumer manager
 */
public class TenancyConsumerManager extends ConsumerManager {

    private static final Logger LOG = LoggerFactory.getLogger(TenancyConsumerManager.class);

    private ConsumerListener consumerListener;

    private final Map<String, ExecutorService> tenancyExecutorServices = new ConcurrentHashMap<>();

    private final Map<String, Consumer> consumerGroupMap = new ConcurrentHashMap<>();

    /**
     * Default executor service
     */
    private ExecutorService defaultExecutorService;

    @Override
    public void setConsumerListener(ConsumerListener consumerListener) {
        this.consumerListener = consumerListener;
    }

    @Override
    public synchronized ExecutorService getOrCreateExecutorService() {
        if (Objects.isNull(defaultExecutorService)){
            Group group = getSchedulerContext().getOrCreateGroupFactory().getOrCreateGroup(null);
            if (group instanceof FIFOGroup){
                defaultExecutorService = Utils.newCachedThreadPool(((FIFOGroup) group).getMaxRunningJobs() +
                                + 1,
                        TenancyGroupFactory.GROUP_NAME_PREFIX + TenancyGroupFactory.DEFAULT_TENANCY + "-Executor-", true);
                // Put the default executor into tenancy executor map
                tenancyExecutorServices.put(TenancyGroupFactory.DEFAULT_TENANCY, defaultExecutorService);
            } else {
                throw new StreamisScheduleException.Runtime("Cannot construct the executor service " +
                        "using the default group: [" + group.getClass().getCanonicalName() + "]", null);
            }
        }
        return this.defaultExecutorService;
    }

    @Override
    public Consumer getOrCreateConsumer(String groupName) {
        Consumer resultConsumer = consumerGroupMap.computeIfAbsent(groupName, groupName0 -> {
            Consumer consumer = createConsumer(groupName);
            Group group = getSchedulerContext().getOrCreateGroupFactory().getGroup(groupName);
            consumer.setGroup(group);
            consumer.setConsumeQueue(new LoopArrayQueue(group));
            int maxRunningJobs = -1;
            if (group instanceof FIFOGroup){
                maxRunningJobs = ((FIFOGroup)group).getMaxRunningJobs();
            }
            LOG.info("Create a new consumer for group: [name: {}, maxRunningJobs: {}, initCapacity: {}, maxCapacity: {}]",
                    groupName, maxRunningJobs, group.getInitCapacity(), group.getMaximumCapacity());
            Optional.ofNullable(consumerListener).ifPresent(listener -> listener.onConsumerCreated(consumer));
            consumer.start();
            return consumer;
        });
        if (resultConsumer instanceof FIFOUserConsumer){
            ((FIFOUserConsumer)resultConsumer).setLastTime(System.currentTimeMillis());
        }
        return resultConsumer;
    }

    @Override
    public Consumer createConsumer(String groupName) {
        Group group = getSchedulerContext().getOrCreateGroupFactory().getGroup(groupName);
        return new FIFOUserConsumer(getSchedulerContext(), getOrCreateExecutorService(groupName), group);
    }

    @Override
    public void destroyConsumer(String groupName) {
        Optional.ofNullable(this.consumerGroupMap.get(groupName)).ifPresent(consumer -> {
            LOG.warn("Start to shutdown the consumer of group: [{}]", groupName);
            consumerGroupMap.remove(groupName);
            consumer.shutdown();
            Optional.ofNullable(consumerListener).ifPresent(listener -> listener.onConsumerDestroyed(consumer));
            LOG.warn("End to shutdown the consumer for group: [{}]", groupName);
        });
    }

    @Override
    public void shutdown() {
        LOG.warn("Shutdown all the consumers which is working");
        consumerGroupMap.forEach((group, consumer) -> {
            LOG.info("Shutdown consumer for group: {}, running events: {}", group, consumer.getRunningEvents());
            consumer.shutdown();
        });
        tenancyExecutorServices.forEach((tenancy, executorService) -> executorService.shutdownNow());
    }

    @Override
    public Consumer[] listConsumers() {
        return consumerGroupMap.values().toArray(new Consumer[0]);
    }

    protected ExecutorService getOrCreateExecutorService(String groupName){
        GroupFactory groupFactory = getSchedulerContext().getOrCreateGroupFactory();
        if (groupFactory instanceof TenancyGroupFactory){
            TenancyGroupFactory tenancyGroupFactory = (TenancyGroupFactory)groupFactory;
            String tenancy = tenancyGroupFactory.getTenancyByGroupName(groupName);
            groupFactory.getGroup(groupName);
            if (StringUtils.isNotBlank(tenancy)){
                return tenancyExecutorServices.computeIfAbsent(tenancy, tenancyName -> {
                    // Use the default value of max running jobs
                    return Utils.newCachedThreadPool(tenancyGroupFactory.getDefaultMaxRunningJobs()  + 1,
                            TenancyGroupFactory.GROUP_NAME_PREFIX + tenancy + "-Executor-", true);
                });
            }
        }
        return getOrCreateExecutorService();
    }
}
