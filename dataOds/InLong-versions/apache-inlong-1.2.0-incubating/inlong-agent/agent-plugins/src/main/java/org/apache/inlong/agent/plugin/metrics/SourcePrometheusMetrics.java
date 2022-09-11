/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.agent.plugin.metrics;

import io.prometheus.client.Counter;

public class SourcePrometheusMetrics implements SourceMetrics {

    public static final String AGENT_SOURCE_METRICS_PREFIX = "inlong_agent_source_";

    public static final String SOURCE_SUCCESS_COUNTER_NAME = "success_count";
    public static final String SOURCE_FAIL_COUNTER_NAME = "fail_count";

    private final String tagName;

    private static final Counter SOURCE_SUCCESS_COUNTER = Counter.build()
            .name(AGENT_SOURCE_METRICS_PREFIX + SOURCE_SUCCESS_COUNTER_NAME)
            .help("The success message count in agent source since agent started.")
            .labelNames("tag")
            .register();

    private static final Counter SOURCE_FAIL_COUNTER = Counter.build()
            .name(AGENT_SOURCE_METRICS_PREFIX + SOURCE_FAIL_COUNTER_NAME)
            .help("The failed message count in agent source since agent started.")
            .labelNames("tag")
            .register();

    public SourcePrometheusMetrics(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public String getTagName() {
        return tagName;
    }

    @Override
    public void incSourceSuccessCount() {
        SOURCE_SUCCESS_COUNTER.labels(tagName).inc();
    }

    @Override
    public long getSourceSuccessCount() {
        return (long) SOURCE_SUCCESS_COUNTER.labels(tagName).get();
    }

    @Override
    public void incSourceFailCount() {
        SOURCE_FAIL_COUNTER.labels(tagName).inc();
    }

    @Override
    public long getSourceFailCount() {
        return (long) SOURCE_FAIL_COUNTER.labels(tagName).get();
    }
}
