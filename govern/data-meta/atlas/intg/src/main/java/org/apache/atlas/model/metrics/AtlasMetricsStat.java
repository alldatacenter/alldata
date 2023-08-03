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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.model.AtlasBaseModelObject;
import org.apache.atlas.utils.AtlasEntityUtil;
import org.apache.commons.collections.CollectionUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


/**
 * Atlas MetricsStat which includes Metrics' collection time and time to live (TTL).
 */
@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasMetricsStat extends AtlasBaseModelObject implements Serializable {

    public static final String METRICS_CATEGORY_GENERAL_PROPERTY  = "general";
    public static final String METRICS_COLLECTION_TIME_PROPERTY   = "collectionTime";
    public static final String METRICS_ID_PREFIX_PROPERTY         = "atlas_metrics_";

    private String       metricsId;
    private long         collectionTime;
    private long         timeToLiveMillis;

    private Map<String, Object> typeData;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AtlasMetrics metrics;

    public AtlasMetricsStat() {
    }

    public AtlasMetricsStat(String guid) {
        setGuid(guid);
    }

    public AtlasMetricsStat(AtlasMetrics metrics){
        this(metrics, null);
    }

    public AtlasMetricsStat(AtlasMetrics metrics,List<String> listOfTypeNames) {
        this(metrics, TimeUnit.HOURS.toMillis(AtlasConfiguration.METRICS_TIME_TO_LIVE_HOURS.getInt()),listOfTypeNames);

    }

    public AtlasMetricsStat(AtlasMetrics metrics, long timeToLiveMillis, List<String> listOfTypeNames) {
        collectionTime = metrics == null ?
                System.currentTimeMillis() : (long) metrics.getMetric(METRICS_CATEGORY_GENERAL_PROPERTY, METRICS_COLLECTION_TIME_PROPERTY);
        setCollectionTime(collectionTime);

        setMetricsId(METRICS_ID_PREFIX_PROPERTY + getCollectionTime() + "@" + AtlasEntityUtil.getMetadataNamespace());

        setTimeToLiveMillis(timeToLiveMillis);
        setMetrics(metrics);
        setGuid(getGuid());

        this.typeData = CollectionUtils.isEmpty(listOfTypeNames) ? null : new HashMap<>();
        AtlasEntityUtil.metricsToTypeData(metrics, listOfTypeNames, typeData);
    }



    public String getMetricsId() {
        return metricsId;
    }

    public void setMetricsId(String metricsId) {
        this.metricsId = metricsId;
    }

    public long getCollectionTime() {
        return collectionTime;
    }

    public void setCollectionTime(long collectionTime) {
        this.collectionTime = collectionTime;
    }

    public long getTimeToLiveMillis() {
        return timeToLiveMillis;
    }

    public void setTimeToLiveMillis(long timeToLiveMillis) {
        this.timeToLiveMillis = timeToLiveMillis;
    }

    public AtlasMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(AtlasMetrics metrics) {
        this.metrics = metrics;
    }

    public Map<String, Object> getTypeData() {
        return typeData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AtlasMetricsStat that = (AtlasMetricsStat) o;
        return Objects.equals(metrics, that.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), metrics);
    }

    @Override
    protected StringBuilder toString(StringBuilder sb) {
        sb.append(", metricsId=").append(metricsId);
        sb.append(", collectionTime=").append(collectionTime);
        sb.append(", timeToLiveMillis=").append(timeToLiveMillis);
        sb.append(", metrics=");
        if (metrics == null) {
            sb.append("null");
        } else {
            sb.append(metrics);
        }

        return sb;
    }
}