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

package org.apache.inlong.agent.core.job;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class JobPrometheusMetrics implements JobMetrics {

    public static final String AGENT_JOB_METRICS_PREFIX = "inlong_agent_job_";

    public static final String RUNNING_JOB_GAUGE_NAME = "running_job_count";
    public static final String FATAL_JOB_COUNTER_NAME = "fatal_job_count";

    private static final String AGENT_JOB_METRIC = "AgentJobMetric";

    private static final Gauge RUNNING_JOB_GAUGE = Gauge.build()
            .name(AGENT_JOB_METRICS_PREFIX + RUNNING_JOB_GAUGE_NAME)
            .help("The count of jobs currently running.")
            .labelNames("tag")
            .register();

    private static final Counter FATAL_JOB_COUNTER = Counter.build()
            .name(AGENT_JOB_METRICS_PREFIX + FATAL_JOB_COUNTER_NAME)
            .help("The total number of current fatal jobs.")
            .labelNames("tag")
            .register();

    @Override
    public void incRunningJobCount() {
        RUNNING_JOB_GAUGE.labels(AGENT_JOB_METRIC).inc();
    }

    @Override
    public void decRunningJobCount() {
        RUNNING_JOB_GAUGE.labels(AGENT_JOB_METRIC).dec();
    }

    @Override
    public void incFatalJobCount() {
        FATAL_JOB_COUNTER.labels(AGENT_JOB_METRIC).inc();
    }
}
