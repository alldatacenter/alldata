/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seatunnel.app.thirdparty.metrics;

import org.apache.seatunnel.app.dal.entity.JobInstanceHistory;
import org.apache.seatunnel.app.dal.entity.JobMetrics;

import lombok.NonNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface IEngineMetricsExtractor {

    List<JobMetrics> getMetricsByJobEngineId(@NonNull String jobEngineId);

    LinkedHashMap<Integer, String> getJobPipelineStatus(@NonNull String jobEngineId);

    JobInstanceHistory getJobHistoryById(String jobEngineId);

    /** contains finished, failed, canceled */
    boolean isJobEnd(@NonNull String jobEngineId);

    boolean isJobEndStatus(@NonNull String jobStatus);

    List<Map<String, String>> getClusterHealthMetrics();

    String getJobStatus(@NonNull String jobEngineId);

    /** Obtain all running task metrics in the engine cluster */
    Map<Long, HashMap<Integer, JobMetrics>> getAllRunningJobMetrics();

    Map<Integer, JobMetrics> getMetricsByJobEngineIdRTMap(@NonNull String jobEngineId);
}
