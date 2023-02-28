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
package io.datavines.server.dqc.coordinator.log;

import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.repository.entity.JobExecution;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.datavines.common.entity.LogResult;
import io.datavines.common.utils.IOUtils;
import io.datavines.server.repository.service.JobExecutionService;

@Component
public class LogService {

    private final Logger logger = LoggerFactory.getLogger(LogService.class);

    @Autowired
    private JobExecutionService jobExecutionService;

    public LogResult queryLog(long jobExecutionId, int offsetLine){
        return this.queryLog(jobExecutionId,offsetLine,10000);
    }

    public LogResult queryLog(long jobExecutionId, int offsetLine, int limit){

        JobExecution jobExecution = getExecutionJob(jobExecutionId);

        List<String> contents = readPartFileContent(jobExecution.getLogPath(), offsetLine, limit);
        StringBuilder msg = new StringBuilder();

        if (CollectionUtils.isNotEmpty(contents)) {
            for (String line:contents) {
                msg.append(line).append("\r\n");
            }
            offsetLine = offsetLine + contents.size();
        }

        return new LogResult(msg.toString(), offsetLine);
    }

    public LogResult queryWholeLog(long jobExecutionId){
        JobExecution jobExecution = getExecutionJob(jobExecutionId);
        return new LogResult(readWholeFileContent(jobExecution.getLogPath()),0);
    }

    public byte[] getLogBytes(long jobExecutionId){
        JobExecution jobExecution = getExecutionJob(jobExecutionId);
        return getFileContentBytes(jobExecution.getLogPath());
    }

    private JobExecution getExecutionJob(long jobExecutionId) {
        JobExecution jobExecution = jobExecutionService.getById(jobExecutionId);
        if (null == jobExecution) {
            logger.info("jobExecution {} is not exist", jobExecutionId);
            throw new DataVinesServerException(Status.TASK_NOT_EXIST_ERROR, jobExecutionId);
        }
        if (StringUtils.isEmpty(jobExecution.getLogPath())) {
            logger.info("jobExecution log path {} is not exist", jobExecutionId);
            throw new DataVinesServerException(Status.TASK_LOG_PATH_NOT_EXIST_ERROR, jobExecutionId);
        }
        return jobExecution;
    }

    /**
     * read part file content，can skip any line and read some lines
     *
     * @param filePath file path
     * @param skipLine skip line
     * @param limit read lines limit
     * @return part file content
     */
    private List<String> readPartFileContent(String filePath,
                                             int skipLine,
                                             int limit){
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            return stream.skip(skipLine).limit(limit).collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("read file error",e);
        }
        return Collections.emptyList();
    }

    /**
     * read whole file content
     *
     * @param filePath file path
     * @return whole file content
     */
    private String readWholeFileContent(String filePath) {
        BufferedReader br = null;
        String line;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
            while ((line = br.readLine()) != null){
                sb.append(line).append("\r\n");
            }
            return sb.toString();
        } catch (IOException e) {
            logger.error("read file error",e);
        } finally {
            IOUtils.closeQuietly(br);
        }
        return "";
    }

    /**
     * get files content bytes，for down load file
     *
     * @param filePath file path
     * @return byte array of file
     */
    private byte[] getFileContentBytes(String filePath) {
        InputStream in = null;
        ByteArrayOutputStream bos = null;
        try {
            in = new FileInputStream(filePath);
            bos  = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            logger.error("get file bytes error",e);
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(in);
        }
        return new byte[0];
    }
}
