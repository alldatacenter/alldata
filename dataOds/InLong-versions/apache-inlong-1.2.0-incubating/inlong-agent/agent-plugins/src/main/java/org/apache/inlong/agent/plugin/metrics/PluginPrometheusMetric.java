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

package org.apache.inlong.agent.plugin.metrics;

import io.prometheus.client.Counter;

public class PluginPrometheusMetric implements PluginMetric {

    public static final String AGENT_PLUGIN_METRICS_PREFIX = "inlong_agent_plugin_";

    public static final String READ_NUM_COUNTER_NAME = "read_num_count";
    public static final String SEND_NUM_COUNTER_NAME = "send_num_count";
    public static final String READ_FAILED_NUM_COUNTER_NAME = "read_failed_num_count";
    public static final String SEND_FAILED_NUM_COUNTER_NAME = "send_failed_num_count";
    public static final String READ_SUCCESS_NUM_COUNTER_NAME = "read_success_num_count";
    public static final String SEND_SUCCESS_NUM_COUNTER_NAME = "send_success_num_count";

    private final String tagName;

    private static final Counter READ_NUM_COUNTER = Counter.build()
            .name(AGENT_PLUGIN_METRICS_PREFIX + READ_NUM_COUNTER_NAME)
            .help("The total number of reads.")
            .labelNames("tag")
            .register();

    private static final Counter SEND_NUM_COUNTER = Counter.build()
            .name(AGENT_PLUGIN_METRICS_PREFIX + SEND_NUM_COUNTER_NAME)
            .help("The total number of sends.")
            .labelNames("tag")
            .register();

    private static final Counter READ_FAILED_NUM_COUNTER = Counter.build()
            .name(AGENT_PLUGIN_METRICS_PREFIX + READ_FAILED_NUM_COUNTER_NAME)
            .help("The total number of failed reads.")
            .labelNames("tag")
            .register();

    private static final Counter SEND_FAILED_NUM_COUNTER = Counter.build()
            .name(AGENT_PLUGIN_METRICS_PREFIX + SEND_FAILED_NUM_COUNTER_NAME)
            .help("The total number of failed sends.")
            .labelNames("tag")
            .register();

    private static final Counter READ_SUCCESS_NUM_COUNTER = Counter.build()
            .name(AGENT_PLUGIN_METRICS_PREFIX + READ_SUCCESS_NUM_COUNTER_NAME)
            .help("The total number of successful reads.")
            .labelNames("tag")
            .register();

    private static final Counter SEND_SUCCESS_NUM_COUNTER = Counter.build()
            .name(AGENT_PLUGIN_METRICS_PREFIX + SEND_SUCCESS_NUM_COUNTER_NAME)
            .help("The total number of successful sends.")
            .labelNames("tag")
            .register();

    public PluginPrometheusMetric(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public String getTagName() {
        return tagName;
    }

    @Override
    public void incReadNum() {
        READ_NUM_COUNTER.labels(tagName).inc();
    }

    @Override
    public long getReadNum() {
        return (long) READ_NUM_COUNTER.labels(tagName).get();
    }

    @Override
    public void incSendNum() {
        SEND_NUM_COUNTER.labels(tagName).inc();
    }

    @Override
    public long getSendNum() {
        return (long) SEND_NUM_COUNTER.labels(tagName).get();
    }

    @Override
    public void incReadFailedNum() {
        READ_FAILED_NUM_COUNTER.labels(tagName).inc();
    }

    @Override
    public long getReadFailedNum() {
        return (long) READ_FAILED_NUM_COUNTER.labels(tagName).get();
    }

    @Override
    public void incSendFailedNum() {
        SEND_FAILED_NUM_COUNTER.labels(tagName).inc();
    }

    @Override
    public long getSendFailedNum() {
        return (long) SEND_FAILED_NUM_COUNTER.labels(tagName).get();
    }

    @Override
    public void incReadSuccessNum() {
        READ_SUCCESS_NUM_COUNTER.labels(tagName).inc();
    }

    @Override
    public long getReadSuccessNum() {
        return (long) READ_SUCCESS_NUM_COUNTER.labels(tagName).get();
    }

    @Override
    public void incSendSuccessNum() {
        SEND_SUCCESS_NUM_COUNTER.labels(tagName).inc();
    }

    @Override
    public void incSendSuccessNum(int delta) {
        SEND_SUCCESS_NUM_COUNTER.labels(tagName).inc(delta);
    }

    @Override
    public long getSendSuccessNum() {
        return (long) SEND_SUCCESS_NUM_COUNTER.labels(tagName).get();
    }
}
