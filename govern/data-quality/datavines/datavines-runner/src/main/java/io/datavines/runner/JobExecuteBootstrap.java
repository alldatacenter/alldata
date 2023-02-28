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
package io.datavines.runner;

import io.datavines.common.config.Configurations;
import io.datavines.common.config.DataVinesJobConfig;
import io.datavines.common.entity.JobExecutionInfo;
import io.datavines.common.entity.JobExecutionRequest;
import io.datavines.common.entity.job.SubmitJob;
import io.datavines.common.enums.JobType;
import io.datavines.common.utils.*;
import io.datavines.engine.config.DataVinesConfigurationManager;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JobExecuteBootstrap {

    public static void main(String[] args) {
        if (args.length != 1) {
            log.info("parameter path is null, please check");
            System.exit(1);
        }

        String parameterJson = FileUtils.readFile(args[0]);
        //进行参数解析和构造
        if (StringUtils.isEmpty(parameterJson)) {
            log.info("parameter is null, please check");
            System.exit(1);
        }

        SubmitJob submitJob = JSONUtils.parseObject(parameterJson, SubmitJob.class);
        if (submitJob == null) {
            log.info("parameter parse to submit job error");
            System.exit(1);
        }

        long id = System.currentTimeMillis();
        JobExecutionInfo jobExecutionInfo = new JobExecutionInfo(
                id, submitJob.getName(),
                submitJob.getEngineType(), JSONUtils.toJsonString(submitJob.getEngineParameter()),
                submitJob.getErrorDataStorageType(), JSONUtils.toJsonString(submitJob.getErrorDataStorageParameter()), submitJob.getName()+"_"+ id,
                submitJob.getValidateResultDataStorageType(), JSONUtils.toJsonString(submitJob.getValidateResultDataStorageParameter()),
                submitJob.getParameter());

        Map<String,String> inputParameter = new HashMap<>();

        DataVinesJobConfig qualityConfig =
                DataVinesConfigurationManager.generateConfiguration(JobType.DATA_QUALITY, inputParameter, jobExecutionInfo);

        JobExecutionRequest jobExecutionRequest = new JobExecutionRequest();
        jobExecutionRequest.setJobExecutionName(submitJob.getName());
        jobExecutionRequest.setJobExecutionId(id);
        jobExecutionRequest.setJobExecutionUniqueId(submitJob.getName()+"_"+ id);
        jobExecutionRequest.setApplicationParameter(JSONUtils.toJsonString(qualityConfig));
        jobExecutionRequest.setEngineType(submitJob.getEngineType());
        jobExecutionRequest.setEngineParameter(JSONUtils.toJsonString(submitJob.getEngineParameter()));
        jobExecutionRequest.setErrorDataStorageType(submitJob.getErrorDataStorageType());
        jobExecutionRequest.setErrorDataStorageParameter(JSONUtils.toJsonString(submitJob.getErrorDataStorageParameter()));
        jobExecutionRequest.setValidateResultDataStorageType(submitJob.getValidateResultDataStorageType());
        jobExecutionRequest.setValidateResultDataStorageParameter(JSONUtils.toJsonString(submitJob.getValidateResultDataStorageParameter()));
        jobExecutionRequest.setNotificationParameters(JSONUtils.toJsonString(submitJob.getNotificationParameters()));
        jobExecutionRequest.setEn(submitJob.isLanguageEn());
        Configurations configurations = new Configurations(CommonPropertyUtils.getProperties());
        JobRunner jobRunner = new JobRunner(jobExecutionRequest, configurations);
        jobRunner.run();
    }
}
