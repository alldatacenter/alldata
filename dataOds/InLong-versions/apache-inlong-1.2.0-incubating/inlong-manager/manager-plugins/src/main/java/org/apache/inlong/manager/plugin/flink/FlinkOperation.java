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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.runtime.rest.messages.job.JobDetailsInfo;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.plugin.flink.dto.FlinkInfo;
import org.apache.inlong.manager.plugin.flink.enums.TaskCommitType;
import org.apache.inlong.manager.plugin.util.FlinkUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.api.common.JobStatus.RUNNING;

/**
 * Flink task operation, such restart or stop flink job.
 */
@Slf4j
public class FlinkOperation {

    private static final String CONFIG_FILE = "application.properties";
    private static final String CONNECTOR_DIR_KEY = "sort.connector.dir";
    private static final String JOB_TERMINATED_MSG = "the job not found by id %s, "
            + "or task already terminated or savepoint path is null";
    private static final String INLONG_MANAGER = "inlong-manager";
    private static final String INLONG_SORT = "inlong-sort";
    private static final String SORT_JAR_PATTERN = "^sort-dist.*jar$";
    private static Properties properties;
    private final FlinkService flinkService;

    public FlinkOperation(FlinkService flinkService) {
        this.flinkService = flinkService;
    }

    /**
     * Get sort connector directory
     */
    private static String getConnectorDir(String parent) throws IOException {
        if (properties == null) {
            properties = new Properties();
            String path = Thread.currentThread().getContextClassLoader().getResource("").getPath() + CONFIG_FILE;
            try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
                properties.load(inputStream);
            }
        }
        return properties.getProperty(CONNECTOR_DIR_KEY, Paths.get(parent, INLONG_SORT, "connectors").toString());
    }

    /**
     * Get Sort connector jar patterns from the Flink info.
     */
    private static String getConnectorJarPattern(FlinkInfo flinkInfo) {
        if (StringUtils.isNotEmpty(flinkInfo.getSourceType()) && StringUtils.isNotEmpty(flinkInfo.getSinkType())) {
            return String.format("^sort-connector-(?i)(%s|%s).*jar$", flinkInfo.getSourceType(),
                    flinkInfo.getSinkType());
        } else {
            return "^sort-connector-.*jar$";
        }
    }

    /**
     * Restart the Flink job.
     */
    public void restart(FlinkInfo flinkInfo) throws Exception {
        String jobId = flinkInfo.getJobId();
        boolean terminated = isNullOrTerminated(jobId);
        if (terminated) {
            String message = String.format("restart job failed, as " + JOB_TERMINATED_MSG, jobId);
            log.error(message);
            throw new Exception(message);
        }

        Future<?> future = TaskRunService.submit(
                new IntegrationTaskRunner(flinkService, flinkInfo, TaskCommitType.RESTART.getCode()));
        future.get();
    }

    /**
     * Start the Flink job, if the job id was not empty, restore it.
     */
    public void start(FlinkInfo flinkInfo) throws Exception {
        String jobId = flinkInfo.getJobId();
        try {
            // Start a new task without savepoint
            if (StringUtils.isEmpty(jobId)) {
                IntegrationTaskRunner taskRunner = new IntegrationTaskRunner(flinkService, flinkInfo,
                        TaskCommitType.START_NOW.getCode());
                Future<?> future = TaskRunService.submit(taskRunner);
                future.get();
            } else {
                // Restore an old task with savepoint
                boolean noSavepoint = isNullOrTerminated(jobId) || StringUtils.isEmpty(flinkInfo.getSavepointPath());
                if (noSavepoint) {
                    String message = String.format("restore job failed, as " + JOB_TERMINATED_MSG, jobId);
                    log.error(message);
                    throw new Exception(message);
                }

                IntegrationTaskRunner taskRunner = new IntegrationTaskRunner(flinkService, flinkInfo,
                        TaskCommitType.RESUME.getCode());
                Future<?> future = TaskRunService.submit(taskRunner);
                future.get();
            }
        } catch (Exception e) {
            log.warn("submit flink job failed for {}", flinkInfo, e);
            throw new Exception("submit flink job failed: " + e.getMessage());
        }
    }

    /**
     * Build Flink local path.
     */
    public void genPath(FlinkInfo flinkInfo, String dataflow) throws Exception {
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        log.info("gen path from {}", path);

        int index = path.indexOf(INLONG_MANAGER);
        if (index == -1) {
            throw new Exception(INLONG_MANAGER + " path not found in " + path);
        }

        path = path.substring(0, path.lastIndexOf(File.separator));
        String startPath = path.substring(0, index);
        String basePath = startPath + INLONG_SORT;
        File file = new File(basePath);
        if (!file.exists()) {
            String message = String.format("file path [%s] not found", basePath);
            log.error(message);
            throw new Exception(message);
        }

        String jarPath = FlinkUtils.findFile(basePath, SORT_JAR_PATTERN);
        flinkInfo.setLocalJarPath(jarPath);
        log.info("get sort jar path success, path: {}", jarPath);

        String connectorDir = getConnectorDir(startPath);
        List<String> connectorPaths = FlinkUtils.listFiles(connectorDir, getConnectorJarPattern(flinkInfo), -1);
        if (CollectionUtils.isEmpty(connectorPaths)) {
            String message = String.format("no sort connectors found in %s", connectorDir);
            log.error(message);
            throw new RuntimeException(message);
        }

        flinkInfo.setConnectorJarPaths(connectorPaths);
        log.info("get sort connector paths success, paths: {}", connectorPaths);

        if (FlinkUtils.writeConfigToFile(path, flinkInfo.getJobName(), dataflow)) {
            flinkInfo.setLocalConfPath(path + File.separator + flinkInfo.getJobName());
        } else {
            String message = String.format("write dataflow to %s failed", path);
            log.error(message + ", dataflow: {}", dataflow);
            throw new Exception(message);
        }
    }

    /**
     * Stop the Flink job.
     */
    public void stop(FlinkInfo flinkInfo) throws Exception {
        String jobId = flinkInfo.getJobId();
        boolean terminated = isNullOrTerminated(jobId);
        if (terminated) {
            String message = String.format("stop job failed, as " + JOB_TERMINATED_MSG, jobId);
            log.error(message);
            throw new Exception(message);
        }

        Future<?> future = TaskRunService.submit(
                new IntegrationTaskRunner(flinkService, flinkInfo, TaskCommitType.STOP.getCode()));
        future.get();
    }

    /**
     * Delete the Flink job
     */
    public void delete(FlinkInfo flinkInfo) throws Exception {
        String jobId = flinkInfo.getJobId();
        JobDetailsInfo jobDetailsInfo = flinkService.getJobDetail(jobId);
        if (jobDetailsInfo == null) {
            throw new Exception(String.format("delete job failed as the job not found for %s", jobId));
        }

        JobStatus jobStatus = jobDetailsInfo.getJobStatus();
        if (jobStatus != null && jobStatus.isTerminalState()) {
            String message = String.format("not support delete %s as the task was terminated", jobId);
            message = jobStatus.isGloballyTerminalState() ? message + " globally" : " locally";
            throw new Exception(message);
        }

        Future<?> future = TaskRunService.submit(
                new IntegrationTaskRunner(flinkService, flinkInfo, TaskCommitType.DELETE.getCode()));
        future.get();
    }

    /**
     * Status of Flink job.
     */
    public void pollJobStatus(FlinkInfo flinkInfo) throws Exception {
        if (flinkInfo.isException()) {
            throw new BusinessException("startup failed: " + flinkInfo.getExceptionMsg());
        }
        String jobId = flinkInfo.getJobId();
        if (StringUtils.isBlank(jobId)) {
            log.error("job id cannot empty for {}", flinkInfo);
            throw new Exception("job id cannot empty");
        }

        while (true) {
            try {
                JobDetailsInfo jobDetailsInfo = flinkService.getJobDetail(jobId);
                if (jobDetailsInfo == null) {
                    log.error("job detail not found by {}", jobId);
                    throw new Exception(String.format("job detail not found by %s", jobId));
                }

                JobStatus jobStatus = jobDetailsInfo.getJobStatus();
                if (jobStatus.isTerminalState()) {
                    log.error("job was terminated for {}, exception: {}", jobId, flinkInfo.getExceptionMsg());
                    throw new Exception("job was terminated for " + jobId);
                }

                if (jobStatus == RUNNING) {
                    log.info("job status is Running for {}", jobId);
                    break;
                }
                log.info("job was not Running for {}", jobId);
                TimeUnit.SECONDS.sleep(5);
            } catch (Exception e) {
                log.error("poll job status error for {}, exception: ", flinkInfo, e);
            }
        }
    }

    /**
     * Check whether the job was terminated by the given job id.
     */
    private boolean isNullOrTerminated(String jobId) throws Exception {
        JobDetailsInfo jobDetailsInfo = flinkService.getJobDetail(jobId);
        boolean terminated = jobDetailsInfo == null || jobDetailsInfo.getJobStatus() == null;
        if (terminated) {
            log.warn("job detail or job status was null for [{}]", jobId);
            return true;
        }

        terminated = jobDetailsInfo.getJobStatus().isTerminalState();
        log.warn("job terminated state was [{}] for [{}]", terminated, jobDetailsInfo);
        return terminated;
    }

}
