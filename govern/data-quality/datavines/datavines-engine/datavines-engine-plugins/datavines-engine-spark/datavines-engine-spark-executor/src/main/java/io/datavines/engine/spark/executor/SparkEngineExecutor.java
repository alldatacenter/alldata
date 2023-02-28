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
package io.datavines.engine.spark.executor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.datavines.common.entity.JobExecutionRequest;
import io.datavines.common.utils.FileUtils;
import io.datavines.engine.executor.core.base.AbstractYarnEngineExecutor;
import io.datavines.engine.spark.executor.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;

import io.datavines.common.config.Configurations;
import io.datavines.common.config.DataVinesJobConfig;
import io.datavines.common.entity.ProcessResult;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.LoggerUtils;
import io.datavines.engine.executor.core.executor.ShellCommandProcess;
import io.datavines.engine.spark.executor.parameter.SparkArgsUtils;
import io.datavines.engine.spark.executor.parameter.SparkParameters;

public class SparkEngineExecutor extends AbstractYarnEngineExecutor {

    /**
     * spark2 command
     */
    private static final String SPARK2_COMMAND = "${SPARK_HOME2}/bin/spark-submit";

    private Configurations configurations;

    @Override
    public void init(JobExecutionRequest jobExecutionRequest, Logger logger, Configurations configurations) throws Exception {

        String threadLoggerInfoName = String.format(LoggerUtils.JOB_LOG_INFO_FORMAT, jobExecutionRequest.getJobExecutionUniqueId());
        Thread.currentThread().setName(threadLoggerInfoName);

        this.jobExecutionRequest = jobExecutionRequest;
        this.logger = logger;
        this.shellCommandProcess = new ShellCommandProcess(this::logHandle,
                logger, jobExecutionRequest, configurations);
        this.configurations = configurations;
    }

    @Override
    public void execute() throws Exception {
        try {
            this.processResult = shellCommandProcess.run(buildCommand());
            logger.info("process result: "+ JSONUtils.toJsonString(this.processResult));
        } catch (Exception e) {
            logger.error("yarn process failure", e);
            throw e;
        }
    }

    @Override
    public void after() throws Exception {

    }

    @Override
    public boolean isCancel() throws Exception {
        return this.cancel;
    }

    @Override
    public ProcessResult getProcessResult() {
        return this.processResult;
    }

    @Override
    public JobExecutionRequest getTaskRequest() {
        return this.jobExecutionRequest;
    }

    @Override
    protected String buildCommand() {

        SparkParameters sparkParameters = JSONUtils.parseObject(jobExecutionRequest.getEngineParameter(), SparkParameters.class);
        assert sparkParameters != null;

        String basePath = System.getProperty("user.dir").replace(File.separator + "bin", File.separator + "libs");
        sparkParameters.setMainJar(basePath + File.separator + configurations.getString("data.quality.jar.name"));

        String pluginDir = basePath.endsWith("libs") ?
                basePath.replace("libs","plugins"):
                basePath + File.separator + "plugins";

        logger.info("spark engine plugin dir : {}", pluginDir);

        if (FileUtils.isExist(pluginDir)) {
            List<String> filePathList = FileUtils.getFileList(pluginDir);
            if (CollectionUtils.isNotEmpty(filePathList)) {
                String jars = sparkParameters.getJars();
                if (StringUtils.isEmpty(jars)) {
                    jars = " --jars " + String.join(",", filePathList);
                } else {
                    if(jars.trim().contains("--jars")) {
                        jars += " " + String.join(",", filePathList);
                    } else {
                        filePathList.add(jars);
                        jars = " --jars " + String.join(",", filePathList);
                    }
                }

                sparkParameters.setJars(jars);
            }
        }

        DataVinesJobConfig configuration =
                JSONUtils.parseObject(jobExecutionRequest.getApplicationParameter(), DataVinesJobConfig.class);

        sparkParameters.setMainArgs("\""
                + StringUtils.replaceDoubleBrackets(StringUtils.escapeJava(JSONUtils.toJsonString(configuration))) + "\"");

        sparkParameters.setMainClass("io.datavines.engine.spark.core.SparkDataVinesBootstrap");

        String others = sparkParameters.getOthers();
        if (StringUtils.isNotEmpty(others)) {
            others += " --conf spark.yarn.tags="+ jobExecutionRequest.getJobExecutionUniqueId();
        } else {
            others = " --conf spark.yarn.tags="+ jobExecutionRequest.getJobExecutionUniqueId();
        }

        sparkParameters.setOthers(others);

        List<String> args = new ArrayList<>();

        args.add(SPARK2_COMMAND);

        args.addAll(SparkArgsUtils.buildArgs(sparkParameters));

        String command = String.join(" ", args);

        logger.info("data quality task command: {}", command);

        return command;
    }
}
