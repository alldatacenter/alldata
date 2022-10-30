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

package org.apache.inlong.tubemq.corebase.metric.impl;

import org.apache.inlong.tubemq.corebase.metric.Metric;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;

/**
 * BaseMetric, store general information about the metric item, such as the metric name.
 *
 * The metric includes a base name and a prefix, the full name is composed of
 *   the prefix + "_" + the base name, different name are selected
 *   for output according to the metric output requirements.
 */
public class BaseMetric implements Metric {
    // metric short name
    private final String shortName;
    // metric full name
    private final String fullName;

    public BaseMetric(String metricName, String prefix) {
        this.shortName = metricName;
        if (TStringUtils.isEmpty(prefix)) {
            this.fullName = metricName;
        } else {
            this.fullName = prefix + "_" + metricName;
        }
    }

    @Override
    public String getShortName() {
        return this.shortName;
    }

    @Override
    public String getFullName() {
        return this.fullName;
    }
}
