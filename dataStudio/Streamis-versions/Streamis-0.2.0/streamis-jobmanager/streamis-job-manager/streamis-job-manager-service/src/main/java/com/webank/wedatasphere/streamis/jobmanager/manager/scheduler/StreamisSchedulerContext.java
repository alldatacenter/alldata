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

import org.apache.linkis.common.listener.ListenerEventBus;
import org.apache.linkis.scheduler.SchedulerContext;
import org.apache.linkis.scheduler.event.ScheduleEvent;
import org.apache.linkis.scheduler.event.SchedulerEventListener;
import org.apache.linkis.scheduler.executer.ExecutorManager;
import org.apache.linkis.scheduler.queue.ConsumerManager;
import org.apache.linkis.scheduler.queue.GroupFactory;

/**
 * Streamis scheduler context
 */
public class StreamisSchedulerContext implements SchedulerContext {

    /**
     * Group factory
     */
    private final GroupFactory groupFactory;

    /**
     * Consumer manager
     */
    private final ConsumerManager consumerManager;

    /**
     * Executor manager
     */
    private final ExecutorManager executorManager;

    public StreamisSchedulerContext(GroupFactory groupFactory, ConsumerManager consumerManager,
                                    ExecutorManager executorManager){
        this.groupFactory = groupFactory;
        this.consumerManager = consumerManager;
        this.consumerManager.setSchedulerContext(this);
        this.executorManager = executorManager;
    }

    @Override
    public GroupFactory getOrCreateGroupFactory() {
        return groupFactory;
    }

    @Override
    public ConsumerManager getOrCreateConsumerManager() {
        return consumerManager;
    }

    @Override
    public ExecutorManager getOrCreateExecutorManager() {
        return executorManager;
    }

    @Override
    public ListenerEventBus<? extends SchedulerEventListener, ? extends ScheduleEvent> getOrCreateSchedulerListenerBus() {
        return null;
    }
}
