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

package com.webank.wedatasphere.streamis.jobmanager.manager;

import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.FutureScheduler;
import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.StreamisScheduler;
import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.StreamisSchedulerExecutorManager;
import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.TenancyConsumerManager;
import org.apache.linkis.scheduler.Scheduler;
import org.apache.linkis.scheduler.executer.ExecutorManager;
import org.apache.linkis.scheduler.queue.ConsumerManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contains the scheduler bean configuration
 */
@Configuration
public class JobManagerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ExecutorManager.class)
    public ExecutorManager executorManager(){
        return new StreamisSchedulerExecutorManager();
    }

    @Bean
    @ConditionalOnMissingBean(ConsumerManager.class)
    public ConsumerManager consumerManager(){
        return new TenancyConsumerManager();
    }

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean(FutureScheduler.class)
    public FutureScheduler scheduler(ExecutorManager executorManager, ConsumerManager consumerManager){
        return new StreamisScheduler(executorManager, consumerManager);
    }
}
