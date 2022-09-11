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

package org.apache.inlong.sort.standalone.source.sortsdk;

import org.apache.flume.Context;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.metrics.SortMetricItem;
import org.apache.inlong.sort.standalone.metrics.SortMetricItemSet;
import org.apache.inlong.sort.standalone.metrics.audit.AuditUtils;
import org.apache.inlong.sort.standalone.source.SourceContext;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Derived class of {@link SourceContext} which implements methods to report metrics.
 */
public final class SortSdkSourceContext extends SourceContext {

    // Metric item set of Sort to create and maintain specific metric group.
    private final SortMetricItemSet metricItemSet;

    /**
     * Type of metrics to report.
     *
     * <p> For {@link SortSdkSource}, there are only two types of fetch results, success or failure.</p>
     */
    public enum FetchResult {
        SUCCESS,
        FAILURE
    }

    /**
     * Constructor of {@link SourceContext}.
     *
     * @param sourceName Name of source. Usually the class name of source.
     * @param context The configured source context.
     */
    public SortSdkSourceContext(
            @NotBlank(message = "sourceName should not be empty or null") final String sourceName,
            @NotNull(message = "context should not be null") final Context context) {

        super(sourceName, context);
        this.metricItemSet = new SortMetricItemSet(sourceName);
        MetricRegister.register(metricItemSet);
    }

    /**
     * Entrance to report fetch metrics.
     *
     * @param event The fetched event. May be <b>null</b> when fetch failed occurs.
     * @param sortId Sort id of event.
     * @param topic Topic that event fetched from. May be <b>null</b> when fetch failed occurs.
     * @param fetchResult Result of fetching, SUCCESS or FAILURE.
     */
    public void reportToMetric(
            @Nullable final ProfileEvent event,
            @Nullable final String sortId,
            @Nullable final String topic,
            @NotNull(message = "Must specify fetch result") final SortSdkSourceContext.FetchResult fetchResult) {

        final Map<String, String> dimensions = this.createSortSdkSourceDimensionMap(event, sortId, topic);
        final SortMetricItem metricItem = metricItemSet.findMetricItem(dimensions);
        final int msgSize = event != null ? event.getBody().length : -1;
        this.reportToMetric(event, metricItem, fetchResult, msgSize);
    }

    /**
     * Selector of metric report flow.
     *
     * @param event The fetched event. May be <b>null</b> when fetch failed occurs.
     * @param item MetricItem that report to.
     * @param fetchResult Result of fetching, SUCCESS or FAILURE.
     * @param size The fetch length. -1 means fetch failure.
     */
    private void reportToMetric(
            @Nullable final ProfileEvent event,
            @NotNull final SortMetricItem item,
            final FetchResult fetchResult,
            final int size) {

        switch (fetchResult) {
            case SUCCESS:
                reportToMetric(item.readSuccessCount, item.readSuccessSize, size);
                AuditUtils.add(AuditUtils.AUDIT_ID_READ_SUCCESS, event);
                break;
            case FAILURE:
                reportToMetric(item.readFailCount, item.readFailSize, size);
                break;
            default:
                break;
        }
    }

    /**
     * Report to a specific metric group.
     *
     * @param countMetric Metric of event count.
     * @param sizeMetric Metric of event size.
     * @param size Size of event.
     */
    private void reportToMetric(
            @NotNull final AtomicLong countMetric,
            @NotNull final AtomicLong sizeMetric,
            final int size) {
        countMetric.incrementAndGet();
        sizeMetric.addAndGet(size);
    }

    /**
     * Generator of report dimensions.
     *
     * <p> For the case of fetch {@link FetchResult#FAILURE}, the event may be null,
     * the {@link org.apache.inlong.sort.standalone.utils.Constants#INLONG_GROUP_ID}
     * and the {@link org.apache.inlong.sort.standalone.utils.Constants#INLONG_STREAM_ID} will not be specified. </p>
     *
     * @param event Event to be reported.
     * @param sortId Sort id of fetched event.
     * @param topic Topic of event.
     *
     * @return The dimensions of reported event.
     */
    private Map<String, String> createSortSdkSourceDimensionMap(
            final ProfileEvent event,
            final String sortId,
            final String topic) {

        final Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_SOURCE_ID, this.getSourceName());
        dimensions.put(SortMetricItem.KEY_TASK_NAME, sortId);
        dimensions.put(SortMetricItem.KEY_SOURCE_DATA_ID, topic);
        if (event != null) {
            SortMetricItem.fillInlongId(event, dimensions);
            long msgTime = event.getRawLogTime();
            long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
            dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        }
        return dimensions;
    }

}
