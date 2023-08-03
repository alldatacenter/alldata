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

import org.apache.inlong.manager.plugin.flink.dto.FlinkConfig;
import org.apache.inlong.manager.plugin.flink.dto.FlinkInfo;
import org.apache.inlong.manager.plugin.flink.dto.StopWithSavepointRequest;
import org.apache.inlong.manager.plugin.flink.enums.TaskCommitType;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.JobStatus;

import static org.apache.flink.api.common.JobStatus.FINISHED;
import static org.apache.inlong.manager.plugin.util.FlinkUtils.getExceptionStackMsg;

/**
 * Integration task runner for start, restart or stop flink service.
 */
@Slf4j
public class IntegrationTaskRunner implements Runnable {

    private static final Integer TRY_MAX_TIMES = 60;
    private static final Integer INTERVAL = 10;
    private final FlinkService flinkService;
    private final FlinkInfo flinkInfo;
    private final Integer commitType;

    public IntegrationTaskRunner(FlinkService flinkService, FlinkInfo flinkInfo, Integer commitType) {
        this.flinkService = flinkService;
        this.flinkInfo = flinkInfo;
        this.commitType = commitType;
    }

    @Override
    public void run() {
        TaskCommitType commitType = TaskCommitType.getInstance(this.commitType);
        if (commitType == null) {
            commitType = TaskCommitType.START_NOW;
        }
        switch (commitType) {
            case START_NOW:
                try {
                    String jobId = flinkService.submit(flinkInfo);
                    flinkInfo.setJobId(jobId);
                    log.info("Start job {} success in backend", jobId);
                } catch (Exception e) {
                    String msg = String.format("Start job %s failed in backend exception[%s]", flinkInfo.getJobId(),
                            getExceptionStackMsg(e));
                    log.warn(msg);
                    flinkInfo.setException(true);
                    flinkInfo.setExceptionMsg(msg);
                }
                break;
            case RESUME:
                try {
                    String jobId = flinkService.restore(flinkInfo);
                    log.info("Restore job {} success in backend", jobId);
                } catch (Exception e) {
                    String msg = String.format("Restore job %s failed in backend exception[%s]", flinkInfo.getJobId(),
                            getExceptionStackMsg(e));
                    log.warn(msg);
                    flinkInfo.setException(true);
                    flinkInfo.setExceptionMsg(msg);
                }
                break;
            case RESTART:
                try {
                    StopWithSavepointRequest stopWithSavepointRequest = new StopWithSavepointRequest();
                    FlinkConfig flinkConfig = flinkService.getFlinkConfig();
                    stopWithSavepointRequest.setDrain(flinkConfig.isDrain());
                    stopWithSavepointRequest.setTargetDirectory(flinkConfig.getSavepointDirectory());
                    String location = flinkService.stopJob(flinkInfo.getJobId(), stopWithSavepointRequest);
                    flinkInfo.setSavepointPath(location);
                    log.info("the jobId: {} savepoint: {} ", flinkInfo.getJobId(), location);
                    int times = 0;
                    while (times < TRY_MAX_TIMES) {
                        JobStatus jobStatus = flinkService.getJobStatus(flinkInfo.getJobId());
                        // restore job
                        if (jobStatus == FINISHED) {
                            try {
                                String jobId = flinkService.restore(flinkInfo);
                                log.info("Restore job {} success in backend", jobId);
                            } catch (Exception e) {
                                log.error("Restore job failed in backend", e);
                            }
                            break;
                        }
                        log.info("Try start job  but the job {} is {}", flinkInfo.getJobId(), jobStatus.toString());
                        try {
                            Thread.sleep(INTERVAL * 1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        times++;
                    }
                    log.info("Restart job {} success in backend", flinkInfo.getJobId());
                } catch (Exception e) {
                    String msg = String.format("Restart job %s failed in backend exception[%s]", flinkInfo.getJobId(),
                            getExceptionStackMsg(e));
                    log.warn(msg);
                    flinkInfo.setException(true);
                    flinkInfo.setExceptionMsg(msg);
                }
                break;
            case STOP:
                try {
                    StopWithSavepointRequest stopWithSavepointRequest = new StopWithSavepointRequest();
                    FlinkConfig flinkConfig = flinkService.getFlinkConfig();
                    stopWithSavepointRequest.setDrain(flinkConfig.isDrain());
                    stopWithSavepointRequest.setTargetDirectory(flinkConfig.getSavepointDirectory());
                    String location = flinkService.stopJob(flinkInfo.getJobId(), stopWithSavepointRequest);
                    flinkInfo.setSavepointPath(location);
                    log.info("the jobId {} savepoint: {} ", flinkInfo.getJobId(), location);
                } catch (Exception e) {
                    String msg = String.format("stop job %s failed in backend exception[%s]", flinkInfo.getJobId(),
                            getExceptionStackMsg(e));
                    log.warn(msg);
                    flinkInfo.setException(true);
                    flinkInfo.setExceptionMsg(msg);
                }
                break;
            case DELETE:
                try {
                    flinkService.cancelJob(flinkInfo.getJobId());
                    log.info("delete job {} success in backend", flinkInfo.getJobId());
                    JobStatus jobStatus = flinkService.getJobStatus(flinkInfo.getJobId());
                    if (jobStatus.isTerminalState()) {
                        log.info("delete job {} success in backend", flinkInfo.getJobId());
                    } else {
                        log.info("delete job {} failed in backend", flinkInfo.getJobId());
                    }
                } catch (Exception e) {
                    String msg = String.format("delete job %s failed in backend exception[%s]", flinkInfo.getJobId(),
                            getExceptionStackMsg(e));
                    log.warn(msg);
                    flinkInfo.setException(true);
                    flinkInfo.setExceptionMsg(msg);
                }
                break;
            default:
                String msg = "not found commitType";
                flinkInfo.setException(true);
                log.warn(msg);
                flinkInfo.setExceptionMsg(msg);
        }
    }
}
