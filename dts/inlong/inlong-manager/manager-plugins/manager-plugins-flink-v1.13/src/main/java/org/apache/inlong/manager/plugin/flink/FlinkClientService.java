/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.plugin.flink;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.client.deployment.StandaloneClusterId;
import org.apache.flink.client.program.rest.RestClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.rest.messages.job.JobDetailsInfo;

import java.util.concurrent.CompletableFuture;

/**
 * Flink service, such as save or get flink config info, etc.
 */
@Slf4j
public class FlinkClientService {

    private final Configuration configuration;

    public FlinkClientService(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Get the Flink Client.
     */
    public RestClusterClient<StandaloneClusterId> getFlinkClient() throws Exception {
        try {
            return new RestClusterClient<>(configuration, StandaloneClusterId.getInstance());
        } catch (Exception e) {
            log.error("get flink client failed: ", e);
            throw new Exception("get flink client failed: " + e.getMessage());
        }
    }

    /**
     * Get the job status by the given job id.
     */
    public JobStatus getJobStatus(String jobId) throws Exception {
        try {
            RestClusterClient<StandaloneClusterId> client = getFlinkClient();
            JobID jobID = JobID.fromHexString(jobId);
            CompletableFuture<JobStatus> jobStatus = client.getJobStatus(jobID);
            return jobStatus.get();
        } catch (Exception e) {
            log.error("get job status by jobId={} failed: ", jobId, e);
            throw new Exception("get job status by jobId=" + jobId + " failed: " + e.getMessage());
        }
    }

    /**
     * Get job detail by the given job id.
     */
    public JobDetailsInfo getJobDetail(String jobId) throws Exception {
        try {
            RestClusterClient<StandaloneClusterId> client = getFlinkClient();
            JobID jobID = JobID.fromHexString(jobId);
            CompletableFuture<JobDetailsInfo> jobDetails = client.getJobDetails(jobID);
            return jobDetails.get();
        } catch (Exception e) {
            log.error("get job detail by jobId={} failed: ", jobId, e);
            throw new Exception("get job detail by jobId=" + jobId + " failed: " + e.getMessage());
        }
    }

    /**
     * Stop the Flink job with the savepoint.
     */
    public String stopJob(String jobId, boolean isDrain, String savepointDirectory) throws Exception {
        try {
            RestClusterClient<StandaloneClusterId> client = getFlinkClient();
            JobID jobID = JobID.fromHexString(jobId);
            CompletableFuture<String> stopResult = client.stopWithSavepoint(jobID, isDrain, savepointDirectory);
            return stopResult.get();
        } catch (Exception e) {
            log.error("stop job {} failed and savepoint directory is {} : ", jobId, savepointDirectory, e);
            throw new Exception("stop job " + jobId + " failed: " + e.getMessage());
        }
    }

    /**
     * Cancel the Flink job.
     */
    public void cancelJob(String jobId) throws Exception {
        try {
            RestClusterClient<StandaloneClusterId> client = getFlinkClient();
            JobID jobID = JobID.fromHexString(jobId);
            client.cancel(jobID);
        } catch (Exception e) {
            log.error("cancel job {} failed: ", jobId, e);
            throw new Exception("cancel job " + jobId + " failed: " + e.getMessage());
        }
    }
}
