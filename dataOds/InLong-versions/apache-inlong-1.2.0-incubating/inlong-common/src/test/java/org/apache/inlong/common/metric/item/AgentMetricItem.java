/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.common.metric.item;

import org.apache.inlong.common.metric.CountMetric;
import org.apache.inlong.common.metric.Dimension;
import org.apache.inlong.common.metric.GaugeMetric;
import org.apache.inlong.common.metric.MetricDomain;
import org.apache.inlong.common.metric.MetricItem;

import java.util.concurrent.atomic.AtomicLong;

/**
 * AgentMetricItem, like PluginMetric
 */
@MetricDomain(name = "Agent")
public class AgentMetricItem extends MetricItem {

    @Dimension
    public String module;
    @Dimension
    public String aspect;
    @Dimension
    public String tag;

    @CountMetric
    public AtomicLong readNum = new AtomicLong(0);

    @CountMetric
    public AtomicLong sendNum = new AtomicLong(0);

    @CountMetric
    public AtomicLong sendFailedNum = new AtomicLong(0);

    @CountMetric
    public AtomicLong readFailedNum = new AtomicLong(0);

    @CountMetric
    public AtomicLong readSuccessNum = new AtomicLong(0);

    @CountMetric
    public AtomicLong sendSuccessNum = new AtomicLong(0);

    @GaugeMetric
    public AtomicLong runningTasks = new AtomicLong(0);

}