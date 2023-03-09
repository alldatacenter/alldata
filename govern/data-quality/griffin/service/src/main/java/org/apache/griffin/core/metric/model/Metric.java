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

package org.apache.griffin.core.metric.model;

import java.util.List;

import org.apache.griffin.core.measure.entity.DqType;

public class Metric {

    private String name;
    private DqType type;
    private String owner;
    private List<MetricValue> metricValues;

    public Metric() {
    }

    public Metric(String name, DqType type, String owner,
                  List<MetricValue> metricValues) {
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.metricValues = metricValues;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DqType getType() {
        return type;
    }

    public void setType(DqType type) {
        this.type = type;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<MetricValue> getMetricValues() {
        return metricValues;
    }

    public void setMetricValues(List<MetricValue> metricValues) {
        this.metricValues = metricValues;
    }
}
