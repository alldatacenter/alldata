/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.metrics;

import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MetricsDownsamplingMethod {
  // Allow for 2 minute discrepancy to account for client side buffering,
  // this ensures at least some data is returned in the initial few minutes.
  private static final long OUT_OF_BAND_TIME_ALLOWANCE = 120000;
  private static final Logger LOG = LoggerFactory.getLogger(MetricsDownsamplingMethod.class);

  // Downsampling methods iterate over the entire metrics result to create output array.
  // Passing down @TemporalInfo avoids re-iterating to filter out out of band data.
  public abstract Number[][] reportMetricData(TimelineMetric metricData,
                                              MetricsDataTransferMethod dataTransferMethod,
                                              TemporalInfo temporalInfo);

  protected boolean isWithinTemporalQueryRange(Long timestamp, TemporalInfo temporalInfo) {
    boolean retVal = temporalInfo == null ||
      timestamp >= (temporalInfo.getStartTimeMillis() - OUT_OF_BAND_TIME_ALLOWANCE)
        && timestamp <= temporalInfo.getEndTimeMillis();

    if (!retVal && LOG.isTraceEnabled()) {
      LOG.trace("Ignoring out of band metric with ts: {}, temporalInfo: startTime = {}, endTime = {}",
        timestamp, temporalInfo.getStartTimeMillis(), temporalInfo.getEndTimeMillis());
    }

    return retVal;
  }
}
