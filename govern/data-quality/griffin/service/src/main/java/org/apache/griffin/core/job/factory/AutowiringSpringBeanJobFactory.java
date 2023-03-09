/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.job.factory;

import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * Auto-wiring SpringBeanJobFactory is special  BeanJobFactory that adds auto-wiring support against
 * {@link SpringBeanJobFactory} allowing you to inject properties from the scheduler context, job data map
 * and trigger data entries into the job bean.
 *
 * @see SpringBeanJobFactory
 * @see ApplicationContextAware
 */
public final class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory
    implements ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(AutowiringSpringBeanJobFactory.class);

    private transient AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(final ApplicationContext context) {
        beanFactory = context.getAutowireCapableBeanFactory();
    }

    @Override
    protected Object createJobInstance(final TriggerFiredBundle bundle) {
        try {
            final Object job = super.createJobInstance(bundle);
            beanFactory.autowireBean(job);
            return job;
        } catch (Exception e) {
            LOGGER.error("fail to create job instance. {}", e);
        }
        return null;
    }
}
