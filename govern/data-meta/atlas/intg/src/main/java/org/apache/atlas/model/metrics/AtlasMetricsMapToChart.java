/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.model.metrics;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * AtlasMetricsForChart is a formatted data type specifically for rendering the Stacked Area Chart.
 * The Stacked Area Chart takes three String values as "key". For the Atlas Metrics entity, they are "Active", "Deleted", and "Shell".
 * The Stacked Area Chart also takes a list of pairs (primitive values) as "values".
 * The first element in the pair is collectionTime. It is used for rendering x-axis of the chart.
 * The second element is the Atlas Metrics entity's count. It is used for rendering y-axis of the chart.
 */
@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasMetricsMapToChart {
    private String key;
    private List<long[]> values;

    public AtlasMetricsMapToChart(String key, List<long[]> values) {
        this.key    = key;
        this.values = values;
    }

    public String getKey() { return key; }

    public void setKey(String key) {
        this.key = key;
    }

    public List<long[]> getValues() {
        return values;
    }

    public void setValues(List<long[]> values) {
        this.values = values;
    }

}