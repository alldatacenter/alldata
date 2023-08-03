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
package org.apache.seatunnel.app.thirdparty.engine;

import org.apache.seatunnel.engine.client.SeaTunnelClient;
import org.apache.seatunnel.engine.client.job.JobClient;
import org.apache.seatunnel.engine.common.config.ConfigProvider;
import org.apache.seatunnel.engine.common.config.JobConfig;
import org.apache.seatunnel.engine.core.job.JobDAGInfo;

import com.hazelcast.client.config.ClientConfig;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SeaTunnelEngineProxy {
    ClientConfig clientConfig = null;

    private SeaTunnelEngineProxy() {
        clientConfig = ConfigProvider.locateAndGetClientConfig();
    }

    public static SeaTunnelEngineProxy getInstance() {
        return SeaTunnelEngineProxyHolder.INSTANCE;
    }

    public String getMetricsContent(@NonNull String jobEngineId) {
        SeaTunnelClient seaTunnelClient = new SeaTunnelClient(clientConfig);
        try {
            return seaTunnelClient.getJobMetrics(Long.valueOf(jobEngineId));
        } finally {
            seaTunnelClient.close();
        }
    }

    public String getJobPipelineStatusStr(@NonNull String jobEngineId) {
        SeaTunnelClient seaTunnelClient = new SeaTunnelClient(clientConfig);
        try {
            return seaTunnelClient.getJobDetailStatus(Long.valueOf(jobEngineId));
        } finally {
            seaTunnelClient.close();
        }
    }

    public JobDAGInfo getJobInfo(@NonNull String jobEngineId) {
        SeaTunnelClient seaTunnelClient = new SeaTunnelClient(clientConfig);
        try {
            return seaTunnelClient.getJobInfo(Long.valueOf(jobEngineId));
        } finally {
            seaTunnelClient.close();
        }
    }

    public String getJobStatus(@NonNull String jobEngineId) {
        SeaTunnelClient seaTunnelClient = new SeaTunnelClient(clientConfig);
        try {
            return seaTunnelClient.getJobStatus(Long.valueOf(jobEngineId));
        } finally {
            seaTunnelClient.close();
        }
    }

    public Map<String, String> getClusterHealthMetrics() {
        SeaTunnelClient seaTunnelClient = new SeaTunnelClient(clientConfig);
        try {
            return seaTunnelClient.getClusterHealthMetrics();
        } finally {
            seaTunnelClient.close();
        }
    }

    private static class SeaTunnelEngineProxyHolder {
        private static final SeaTunnelEngineProxy INSTANCE = new SeaTunnelEngineProxy();
    }

    public String getAllRunningJobMetricsContent() {

        SeaTunnelClient seaTunnelClient = new SeaTunnelClient(clientConfig);
        try {
            /*return seaTunnelClient.getJobClient().getRunningJobMetrics();*/
            return "waitting ST 2.3.3";
        } finally {
            seaTunnelClient.close();
        }
    }

    public void pauseJob(String jobEngineId) {
        SeaTunnelClient seaTunnelClient = new SeaTunnelClient(clientConfig);
        JobClient jobClient = seaTunnelClient.getJobClient();
        jobClient.savePointJob(Long.valueOf(jobEngineId));
    }

    public void restoreJob(
            @NonNull String filePath, @NonNull Long jobInstanceId, @NonNull Long jobEngineId) {
        SeaTunnelClient seaTunnelClient = new SeaTunnelClient(clientConfig);
        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(jobInstanceId + "_job");
        try {
            seaTunnelClient.restoreExecutionContext(filePath, jobConfig, jobEngineId).execute();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
