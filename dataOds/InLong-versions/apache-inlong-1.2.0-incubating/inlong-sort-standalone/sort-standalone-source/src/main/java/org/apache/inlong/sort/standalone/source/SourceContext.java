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

package org.apache.inlong.sort.standalone.source;

import org.apache.flume.Context;
import org.apache.inlong.common.metric.MetricItem;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Base source context <b>WITHOUT</b> metric reporter.
 * The derived classes of SourceContext may implement {@link MetricItem} and
 * realize methods to report customized metrics.
 */
public class SourceContext {

    // The key of reload interval of source.
    private static final String KEY_RELOAD_INTERVAL = "reloadInterval";

    // the default reload interval value in ms.
    private static final long DEFAULT_RELOAD_INTERVAL_MS = 6000L;

    // The configured source context.
    private final Context sourceContext;

    // Name of source. Usually the class name of source.
    private final String sourceName;

    // Cluster Id of source.
    @NotNull
    private final String clusterId;

    /**
     * Constructor of {@link SourceContext}.
     *
     * @param sourceName Name of source. Usually the class name of source.
     * @param context The configured source context.
     */
    public SourceContext(
            @NotBlank(message = "sourceName should not be empty or null") final String sourceName,
            @NotNull(message = "context should not be null") final Context context) {

        this.sourceName = sourceName;
        this.sourceContext = context;
        this.clusterId = context.getString(CommonPropertiesHolder.KEY_CLUSTER_ID);
    }

    /**
     * Obtain the reload interval of source.
     * @return Reload interval of source.
     */
    public final long getReloadInterval() {
        return sourceContext.getLong(SourceContext.KEY_RELOAD_INTERVAL, DEFAULT_RELOAD_INTERVAL_MS);
    }

    /**
     * Obtain the cluster Id of source.
     * @return Cluster Id of source.
     */
    public final String getClusterId() {
        return clusterId;
    }

    /**
     * Obtain the name of source.
     * @return Name of source.
     */
    public final String getSourceName() {
        return sourceName;
    }

}
