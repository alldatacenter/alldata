/**
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

package org.apache.inlong.common.metric.set;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.inlong.common.metric.CountMetric;
import org.apache.inlong.common.metric.Dimension;
import org.apache.inlong.common.metric.MetricDomain;
import org.apache.inlong.common.metric.MetricItem;

/**
 * 
 * DataProxyMetricItem
 */
@MetricDomain(name = "DataProxy")
public class DataProxyMetricItem extends MetricItem {
    @Dimension
    public String setId;
    @Dimension
    public String containerName;
    @Dimension
    public String containerIp;
    @Dimension
    public String sourceId;
    @Dimension
    public String sourceDataId;
    @Dimension
    public String inlongGroupId;
    @Dimension
    public String inlongStreamId;
    @Dimension
    public String sinkId;
    @Dimension
    public String sinkDataId;
    @CountMetric
    public AtomicLong readSuccessCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong readSuccessSize = new AtomicLong(0);
    @CountMetric
    public AtomicLong readFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong readFailSize = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendSize = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendSuccessCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendSuccessSize = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendFailCount = new AtomicLong(0);
    @CountMetric
    public AtomicLong sendFailSize = new AtomicLong(0);
    @CountMetric
    // sinkCallbackTime - sinkBeginTime(milliseconds)
    public AtomicLong sinkDuration = new AtomicLong(0);
    @CountMetric
    // sinkCallbackTime - sourceReceiveTime(milliseconds)
    public AtomicLong nodeDuration = new AtomicLong(0);
    @CountMetric
    // sinkCallbackTime - eventCreateTime(milliseconds)
    public AtomicLong wholeDuration = new AtomicLong(0);
}
