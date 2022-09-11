/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.agent.core.task;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.inlong.common.metric.CountMetric;
import org.apache.inlong.common.metric.Dimension;
import org.apache.inlong.common.metric.GaugeMetric;
import org.apache.inlong.common.metric.MetricDomain;
import org.apache.inlong.common.metric.MetricItem;
import org.apache.inlong.common.metric.MetricRegister;

/**
 * metrics for agent task
 */
@MetricDomain(name = "AgentTask")
public class TaskJmxMetrics extends MetricItem implements TaskMetrics {

    private static final TaskJmxMetrics JOB_METRICS = new TaskJmxMetrics();
    private static final AtomicBoolean REGISTER_ONCE = new AtomicBoolean(false);
    private static final String AGENT_TASK = "AgentTaskMetric";

    @Dimension
    public String module;

    @GaugeMetric
    private final AtomicLong runningTasks = new AtomicLong(0);

    @GaugeMetric
    private final AtomicLong retryingTasks = new AtomicLong(0);

    @CountMetric
    private final AtomicLong fatalTasks = new AtomicLong(0);

    private TaskJmxMetrics() {
    }

    public static TaskJmxMetrics create() {
        // register one time.
        if (REGISTER_ONCE.compareAndSet(false, true)) {
            JOB_METRICS.module = AGENT_TASK;
            MetricRegister.register(JOB_METRICS);
        }
        return JOB_METRICS;
    }

    @Override
    public void incRunningTaskCount() {
        runningTasks.incrementAndGet();
    }

    @Override
    public void decRunningTaskCount() {
        runningTasks.decrementAndGet();
    }

    @Override
    public void incRetryingTaskCount() {
        retryingTasks.incrementAndGet();
    }

    @Override
    public void decRetryingTaskCount() {
        retryingTasks.decrementAndGet();
    }

    @Override
    public void incFatalTaskCount() {
        fatalTasks.incrementAndGet();
    }
}
