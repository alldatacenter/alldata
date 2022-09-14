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

package com.webank.wedatasphere.streamis.jobmanager.launcher;

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.JobInfo;
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager.JobLaunchManager;
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager.JobLaunchManager$;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.manager.SimpleFlinkJobLaunchManager$;
import org.apache.linkis.common.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

@Configuration
public class JobLauncherAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(JobLauncherAutoConfiguration.class);

    public static final String DEFAULT_JOB_LAUNCH_MANGER = SimpleFlinkJobLaunchManager$.MODULE$.INSTANCE_NAME();

    @Bean(initMethod = "init", destroyMethod = "destroy")
    @ConditionalOnMissingBean(JobLaunchManager.class)
    @SuppressWarnings("unchecked")
    public JobLaunchManager<? extends JobInfo> defaultJobLaunchManager(){
        // First to scan the available job launch manager
        ClassUtils.reflections().getSubTypesOf(JobLaunchManager.class).stream()
                .filter(clazz -> !ClassUtils.isInterfaceOrAbstract(clazz)).forEach(clazz -> {
                    Constructor<?> constructor = null;
                    try {
                        constructor = clazz.getConstructor();
                    } catch (NoSuchMethodException e) {
                        LOG.warn("Job launch manger: [{}] has no empty constructor ", clazz.getCanonicalName(), e);
                    }
                    if (Objects.nonNull(constructor)){
                        try {
                            JobLaunchManager<? extends JobInfo> launchManager = (JobLaunchManager<? extends JobInfo>) constructor.newInstance();
                            JobLaunchManager$.MODULE$.registerJobManager(launchManager.getName(), launchManager);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            LOG.warn("Unable to instance the job launch manager: [{}]", clazz.getCanonicalName(), e);
                        }
                    }
                });
        // Use the flink job launch manager as default
        JobLaunchManager<? extends JobInfo> defaultManager = JobLaunchManager$.MODULE$.getJobManager(DEFAULT_JOB_LAUNCH_MANGER);
        if (Objects.isNull(defaultManager)){
            throw new IllegalArgumentException("Unable to find the default job launch manger: [" + DEFAULT_JOB_LAUNCH_MANGER +
                    "], please check the jar classpath and configuration");
        }
        return defaultManager;
    }
}
