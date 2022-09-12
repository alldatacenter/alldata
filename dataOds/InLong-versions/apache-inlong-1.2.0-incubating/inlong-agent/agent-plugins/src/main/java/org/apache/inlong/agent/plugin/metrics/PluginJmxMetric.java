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

import java.util.concurrent.atomic.AtomicLong;

import org.apache.inlong.agent.metrics.Metric;
import org.apache.inlong.common.metric.Dimension;
import org.apache.inlong.common.metric.MetricDomain;
import org.apache.inlong.common.metric.MetricItem;
import org.apache.inlong.common.metric.MetricRegister;

/**
 * metrics for agent plugin
 */
@MetricDomain(name = "PluginMetric")
public class PluginJmxMetric extends MetricItem implements PluginMetric {

    @Dimension
    private final String tagName;

    @Metric
    private final AtomicLong readNum = new AtomicLong(0);

    @Metric
    private final AtomicLong sendNum = new AtomicLong(0);

    @Metric
    private final AtomicLong sendFailedNum = new AtomicLong(0);

    @Metric
    private final AtomicLong readFailedNum = new AtomicLong(0);

    @Metric
    private final AtomicLong readSuccessNum = new AtomicLong(0);

    @Metric
    private final AtomicLong sendSuccessNum = new AtomicLong(0);

    public PluginJmxMetric(String tagName) {
        this.tagName = tagName;
        MetricRegister.register(this);
    }

    @Override
    public String getTagName() {
        return tagName;
    }

    @Override
    public void incReadNum() {
        readNum.incrementAndGet();
    }

    @Override
    public long getReadNum() {
        return readNum.get();
    }

    @Override
    public void incSendNum() {
        sendNum.incrementAndGet();
    }

    @Override
    public long getSendNum() {
        return sendNum.get();
    }

    @Override
    public void incReadFailedNum() {
        readFailedNum.incrementAndGet();
    }

    @Override
    public long getReadFailedNum() {
        return readFailedNum.get();
    }

    @Override
    public void incSendFailedNum() {
        sendFailedNum.incrementAndGet();
    }

    @Override
    public long getSendFailedNum() {
        return sendFailedNum.get();
    }

    @Override
    public void incReadSuccessNum() {
        readSuccessNum.incrementAndGet();
    }

    @Override
    public long getReadSuccessNum() {
        return readSuccessNum.get();
    }

    @Override
    public void incSendSuccessNum() {
        sendSuccessNum.incrementAndGet();
    }

    @Override
    public void incSendSuccessNum(int delta) {
        sendSuccessNum.addAndGet(delta);
    }

    @Override
    public long getSendSuccessNum() {
        return sendSuccessNum.get();
    }
}
