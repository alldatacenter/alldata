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
package io.datavines.engine.executor.core.executor;

import io.datavines.common.config.Configurations;
import io.datavines.common.entity.JobExecutionRequest;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

public class ShellCommandProcess extends BaseCommandProcess {

    private static final String SH = "sh";

    public ShellCommandProcess(Consumer<List<String>> logHandler,
                               Logger logger,
                               JobExecutionRequest jobExecutionRequest,
                               Configurations configurations){
        super(logHandler, logger, jobExecutionRequest, configurations);
    }

    @Override
    protected String buildCommandFilePath() {
        return String.format("%s/%s.command", jobExecutionRequest.getExecuteFilePath(), jobExecutionRequest.getJobExecutionId());
    }

    @Override
    protected void createCommandFileIfNotExists(String execCommand, String commandFile) throws IOException {
        logger.info("tenant {},job dir:{}" , jobExecutionRequest.getTenantCode(), jobExecutionRequest.getExecuteFilePath());

        if(Files.exists(Paths.get(commandFile))){
            Files.delete(Paths.get(commandFile));
        }

        logger.info("create command file:{}",commandFile);

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/sh\n");
        sb.append("BASEDIR=$(cd `dirname $0`; pwd)\n");
        sb.append("cd $BASEDIR\n");

        if (jobExecutionRequest.getEnv() != null) {
            sb.append(jobExecutionRequest.getEnv()).append("\n");
        }

        sb.append("\n");
        sb.append(execCommand);
        logger.info("command : {}",sb.toString());

        // write data to file
        FileUtils.writeStringToFile(new File(commandFile), sb.toString(), StandardCharsets.UTF_8);
    }

    @Override
    protected String commandInterpreter() {
        return SH;
    }
}
