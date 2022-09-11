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

package org.apache.inlong.sdk.dataproxy.pb.metrics;

import java.util.List;
import java.util.Map;

import org.apache.inlong.common.metric.MetricDomain;
import org.apache.inlong.common.metric.MetricItem;
import org.apache.inlong.common.metric.MetricItemSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * SdkMetricItemSet
 */
@MetricDomain(name = "ProxySdk")
public class SdkMetricItemSet extends MetricItemSet<SdkMetricItem> {

    public static final Logger LOG = LoggerFactory.getLogger(SdkMetricItemSet.class);

    /**
     * Constructor
     * 
     * @param name
     */
    public SdkMetricItemSet(String name) {
        super(name);
    }

    /**
     * createItem
     * 
     * @return
     */
    @Override
    protected SdkMetricItem createItem() {
        return new SdkMetricItem();
    }

    /**
     * snapshot
     * 
     * @return
     */
    public List<MetricItem> snapshot() {
        List<MetricItem> snapshot = super.snapshot();
        return snapshot;
    }

    /**
     * getItemMap
     * 
     * @return
     */
    public Map<String, SdkMetricItem> getItemMap() {
        return this.itemMap;
    }
}
