/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.flink.client;

import com.qlangtech.tis.plugins.flink.client.util.JarArgUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.PackagedProgramUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;
import org.apache.flink.runtime.jobmaster.JobResult;
import org.apache.flink.util.function.FunctionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-10 17:47
 **/
public class FlinkClient {

    private static final Logger logger = LoggerFactory.getLogger(FlinkClient.class);

    public JobID submitJar(ClusterClient clusterClient, JarSubmitFlinkRequest request) throws Exception {
        logger.trace("start submit jar request,entryClass:{}", request.getEntryClass());
        // try {
        File jarFile = new File(request.getDependency()); //jarLoader.downLoad(request.getDependency(), request.isCache());
        if (!jarFile.exists()) {
            throw new IllegalArgumentException("file is not exist:" + jarFile.getAbsolutePath());
        }
        List<String> programArgs = JarArgUtil.tokenizeArguments(request.getProgramArgs());

        PackagedProgram.Builder programBuilder = PackagedProgram.newBuilder();
        programBuilder.setEntryPointClassName(request.getEntryClass());
        programBuilder.setJarFile(jarFile);


        if (CollectionUtils.isNotEmpty(request.getUserClassPaths())) {
            programBuilder.setUserClassPaths(request.getUserClassPaths());
        }

        if (programArgs.size() > 0) {
            programBuilder.setArguments(programArgs.toArray(new String[programArgs.size()]));
        }

        final SavepointRestoreSettings savepointSettings;
        String savepointPath = request.getSavepointPath();
        if (StringUtils.isNotEmpty(savepointPath)) {
            Boolean allowNonRestoredOpt = request.getAllowNonRestoredState();
            boolean allowNonRestoredState = allowNonRestoredOpt != null && allowNonRestoredOpt.booleanValue();
            savepointSettings = SavepointRestoreSettings.forPath(savepointPath, allowNonRestoredState);
        } else {
            savepointSettings = SavepointRestoreSettings.none();
        }

        programBuilder.setSavepointRestoreSettings(savepointSettings);
        PackagedProgram program = programBuilder.build();
        JobGraph jobGraph = PackagedProgramUtils.createJobGraph(program, new Configuration(), request.getParallelism(), false);
        try {

            CompletableFuture<JobID> submissionResult = clusterClient.submitJob(jobGraph);
            return submissionResult.thenApplyAsync(
                    FunctionUtils.uncheckedFunction(
                            jobId -> {
                                org.apache.flink.client.ClientUtils
                                        .waitUntilJobInitializationFinished(
                                                () -> (JobStatus) clusterClient.getJobStatus(jobId).get(),
                                                () -> (JobResult) clusterClient.requestJobResult(jobId).get(),
                                                program.getUserCodeClassLoader());
                                return jobId;
                            })).get();

            //submissionResult.complete()
//            submissionResult.get();
//            return jobId;
        } catch (Exception e) {
            logger.error(" submit sql request fail", e);
            throw new RuntimeException(e);
        }
    }

}
