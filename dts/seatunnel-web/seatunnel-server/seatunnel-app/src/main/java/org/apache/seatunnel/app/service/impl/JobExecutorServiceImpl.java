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

package org.apache.seatunnel.app.service.impl;

import org.apache.seatunnel.app.common.Result;
import org.apache.seatunnel.app.dal.dao.IJobInstanceDao;
import org.apache.seatunnel.app.dal.entity.JobInstance;
import org.apache.seatunnel.app.domain.response.engine.Engine;
import org.apache.seatunnel.app.domain.response.executor.JobExecutorRes;
import org.apache.seatunnel.app.service.IJobExecutorService;
import org.apache.seatunnel.app.service.IJobInstanceService;
import org.apache.seatunnel.app.thirdparty.engine.SeaTunnelEngineProxy;
import org.apache.seatunnel.app.thirdparty.metrics.EngineMetricsExtractorFactory;
import org.apache.seatunnel.app.thirdparty.metrics.IEngineMetricsExtractor;
import org.apache.seatunnel.common.config.Common;
import org.apache.seatunnel.common.config.DeployMode;
import org.apache.seatunnel.engine.client.SeaTunnelClient;
import org.apache.seatunnel.engine.client.job.ClientJobProxy;
import org.apache.seatunnel.engine.client.job.JobExecutionEnvironment;
import org.apache.seatunnel.engine.common.config.ConfigProvider;
import org.apache.seatunnel.engine.common.config.JobConfig;
import org.apache.seatunnel.engine.core.job.JobStatus;

import org.springframework.stereotype.Service;

import com.hazelcast.client.config.ClientConfig;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class JobExecutorServiceImpl implements IJobExecutorService {
    @Resource private IJobInstanceService jobInstanceService;
    @Resource private IJobInstanceDao jobInstanceDao;

    @Override
    public Result jobExecute(Integer userId, Long jobDefineId) {

        JobExecutorRes executeResource =
                jobInstanceService.createExecuteResource(userId, jobDefineId);
        String jobConfig = executeResource.getJobConfig();

        String configFile = writeJobConfigIntoConfFile(jobConfig, jobDefineId);

        Long jobInstanceId =
                executeJobBySeaTunnel(userId, configFile, executeResource.getJobInstanceId());
        return Result.success(jobInstanceId);
    }

    public String writeJobConfigIntoConfFile(String jobConfig, Long jobDefineId) {
        String projectRoot = System.getProperty("user.dir");
        String filePath = projectRoot + "\\profile\\" + Long.toString(jobDefineId) + ".conf";
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }

            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(jobConfig);
            bufferedWriter.close();

            log.info("File created and content written successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }

    public Long executeJobBySeaTunnel(Integer userId, String filePath, Long jobInstanceId) {
        Common.setDeployMode(DeployMode.CLIENT);
        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(jobInstanceId + "_job");
        SeaTunnelClient seaTunnelClient = createSeaTunnelClient();
        try {
            JobExecutionEnvironment jobExecutionEnv =
                    seaTunnelClient.createExecutionContext(filePath, jobConfig);
            final ClientJobProxy clientJobProxy = jobExecutionEnv.execute();
            JobInstance jobInstance = jobInstanceDao.getJobInstance(jobInstanceId);
            jobInstance.setJobEngineId(Long.toString(clientJobProxy.getJobId()));
            jobInstanceDao.update(jobInstance);

            CompletableFuture.runAsync(
                    () -> {
                        waitJobFinish(
                                clientJobProxy,
                                userId,
                                jobInstanceId,
                                Long.toString(clientJobProxy.getJobId()),
                                seaTunnelClient);
                    });

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return jobInstanceId;
    }

    public void waitJobFinish(
            ClientJobProxy clientJobProxy,
            Integer userId,
            Long jobInstanceId,
            String jobEngineId,
            SeaTunnelClient seaTunnelClient) {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        CompletableFuture<JobStatus> future =
                CompletableFuture.supplyAsync(
                        () -> {
                            return clientJobProxy.waitForJobComplete();
                        },
                        executor);
        try {
            JobStatus jobStatus = future.get();
            if (JobStatus.FINISHED.equals(jobStatus)) {
                jobInstanceService.complete(userId, jobInstanceId, jobEngineId);
                executor.shutdown();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            seaTunnelClient.close();
        }
    }

    private SeaTunnelClient createSeaTunnelClient() {
        ClientConfig clientConfig = ConfigProvider.locateAndGetClientConfig();
        clientConfig.setClusterName(getClusterName("seatunnel"));
        return new SeaTunnelClient(clientConfig);
    }

    public static String getClusterName(String testClassName) {
        //        return System.getProperty("user.name") + "_" + testClassName;
        return testClassName;
    }

    @Override
    public Result jobPause(Integer userId, Long jobInstanceId) {
        JobInstance jobInstance = jobInstanceDao.getJobInstance(jobInstanceId);
        if (getJobStatusFromEngine(jobInstance, jobInstance.getJobEngineId()) == "RUNNING") {
            pauseJobInEngine(jobInstance.getJobEngineId());
        }
        return Result.success();
    }

    private String getJobStatusFromEngine(@NonNull JobInstance jobInstance, String jobEngineId) {

        Engine engine = new Engine(jobInstance.getEngineName(), jobInstance.getEngineVersion());

        IEngineMetricsExtractor engineMetricsExtractor =
                (new EngineMetricsExtractorFactory(engine)).getEngineMetricsExtractor();

        return engineMetricsExtractor.getJobStatus(jobEngineId);
    }

    private void pauseJobInEngine(@NonNull String jobEngineId) {
        SeaTunnelEngineProxy.getInstance().pauseJob(jobEngineId);
    }

    @Override
    public Result jobStore(Integer userId, Long jobInstanceId) {
        JobInstance jobInstance = jobInstanceDao.getJobInstance(jobInstanceId);

        String projectRoot = System.getProperty("user.dir");
        String filePath =
                projectRoot + "\\profile\\" + Long.toString(jobInstance.getJobDefineId()) + ".conf";

        SeaTunnelEngineProxy.getInstance()
                .restoreJob(filePath, jobInstanceId, Long.valueOf(jobInstance.getJobEngineId()));
        return Result.success();
    }
}
