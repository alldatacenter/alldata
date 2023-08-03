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

import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.plugin.flink.dto.FlinkConfig;
import org.apache.inlong.manager.plugin.flink.dto.FlinkInfo;
import org.apache.inlong.manager.plugin.flink.dto.StopWithSavepointRequest;
import org.apache.inlong.manager.plugin.flink.enums.Constants;
import org.apache.inlong.manager.plugin.util.FlinkConfiguration;
import org.apache.inlong.manager.plugin.util.FlinkServiceUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.client.deployment.StandaloneClusterId;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.PackagedProgramUtils;
import org.apache.flink.client.program.rest.RestClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;
import org.apache.flink.runtime.rest.messages.job.JobDetailsInfo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Flink service, such as save or get flink config info, etc.
 */
@Slf4j
public class FlinkService {

    private static final Pattern IP_PORT_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+)");

    private final FlinkConfig flinkConfig;
    private final Integer parallelism;
    private final String savepointDirectory;
    private final Configuration configuration;
    private final FlinkClientService clientService;

    /**
     * Constructor of FlinkService.
     */
    public FlinkService(String endpoint) throws Exception {
        FlinkConfiguration flinkConfiguration = new FlinkConfiguration();
        flinkConfig = flinkConfiguration.getFlinkConfig();
        parallelism = flinkConfig.getParallelism();
        savepointDirectory = flinkConfig.getSavepointDirectory();

        configuration = new Configuration();
        Integer jobManagerPort = flinkConfig.getJobManagerPort();
        configuration.setInteger(JobManagerOptions.PORT, jobManagerPort);

        Integer port;
        String address;
        if (StringUtils.isEmpty(endpoint)) {
            address = flinkConfig.getAddress();
            port = flinkConfig.getPort();
        } else {
            Map<String, String> ipPort = translateFromEndpoint(endpoint);
            if (ipPort.isEmpty()) {
                throw new BusinessException("get address:port failed from endpoint " + endpoint);
            }
            address = ipPort.get("address");
            port = Integer.valueOf(ipPort.get("port"));
        }
        configuration.setString(JobManagerOptions.ADDRESS, address);
        configuration.setInteger(RestOptions.PORT, port);

        clientService = (FlinkClientService) FlinkServiceUtils.getFlinkClientService(configuration, flinkConfig);
    }

    /**
     * Translate the Endpoint to address & port
     */
    private Map<String, String> translateFromEndpoint(String endpoint) throws Exception {
        Map<String, String> map = new HashMap<>(2);
        Matcher matcher = IP_PORT_PATTERN.matcher(endpoint);
        if (matcher.find()) {
            map.put("address", matcher.group(1));
            map.put("port", matcher.group(2));
            return map;
        } else {
            throw new Exception("endpoint [" + endpoint + "] was not match address:port");
        }
    }

    /**
     * Get Flink config.
     */
    public FlinkConfig getFlinkConfig() {
        return flinkConfig;
    }

    /**
     * Get the job status by the given job id.
     */
    public JobStatus getJobStatus(String jobId) throws Exception {
        return clientService.getJobStatus(jobId);
    }

    /**
     * Get job detail by the given job id.
     */
    public JobDetailsInfo getJobDetail(String jobId) throws Exception {
        return clientService.getJobDetail(jobId);
    }

    /**
     * Submit the Flink job.
     */
    public String submit(FlinkInfo flinkInfo) throws Exception {
        try {
            SavepointRestoreSettings settings = SavepointRestoreSettings.none();
            return submitJobBySavepoint(flinkInfo, settings);
        } catch (Exception e) {
            log.error("submit job from info {} failed: ", flinkInfo, e);
            throw new Exception("submit job failed: " + e.getMessage());
        }
    }

    /**
     * Restore the Flink job.
     */
    public String restore(FlinkInfo flinkInfo) throws Exception {
        try {
            if (StringUtils.isNotEmpty(flinkInfo.getSavepointPath())) {
                SavepointRestoreSettings settings = SavepointRestoreSettings.forPath(savepointDirectory, false);
                return submitJobBySavepoint(flinkInfo, settings);
            } else {
                log.warn("skip to restore as the savepoint path was empty " + flinkInfo);
                return null;
            }
        } catch (Exception e) {
            log.error("restore job from info {} failed: ", flinkInfo, e);
            throw new Exception("restore job failed: " + e.getMessage());
        }
    }

    /**
     * Submit the job with the savepoint settings.
     */
    private String submitJobBySavepoint(FlinkInfo flinkInfo, SavepointRestoreSettings settings) throws Exception {
        String localJarPath = flinkInfo.getLocalJarPath();
        final File jarFile = new File(localJarPath);
        final String[] programArgs = genProgramArgs(flinkInfo, flinkConfig);

        List<URL> connectorJars = flinkInfo.getConnectorJarPaths().stream().map(p -> {
            try {
                return new File(p).toURI().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        PackagedProgram program = PackagedProgram.newBuilder()
                .setConfiguration(configuration)
                .setEntryPointClassName(Constants.ENTRYPOINT_CLASS)
                .setJarFile(jarFile)
                .setUserClassPaths(connectorJars)
                .setArguments(programArgs)
                .setSavepointRestoreSettings(settings).build();
        JobGraph jobGraph = PackagedProgramUtils.createJobGraph(program, configuration, parallelism, false);
        jobGraph.addJars(connectorJars);

        RestClusterClient<StandaloneClusterId> client = clientService.getFlinkClient();
        CompletableFuture<JobID> result = client.submitJob(jobGraph);
        return result.get().toString();
    }

    /**
     * Stop the Flink job with the savepoint.
     */
    public String stopJob(String jobId, StopWithSavepointRequest request) throws Exception {
        return clientService.stopJob(jobId, request.isDrain(), request.getTargetDirectory());
    }

    /**
     * Cancel the Flink job.
     */
    public void cancelJob(String jobId) throws Exception {
        clientService.cancelJob(jobId);
    }

    /**
     * Build the program of the Flink job.
     */
    private String[] genProgramArgs(FlinkInfo flinkInfo, FlinkConfig flinkConfig) {
        List<String> list = new ArrayList<>();
        list.add("-cluster-id");
        list.add(flinkInfo.getJobName());
        list.add("-job.name");
        list.add(flinkInfo.getJobName());
        list.add("-group.info.file");
        list.add(flinkInfo.getLocalConfPath());
        list.add("-checkpoint.interval");
        list.add("60000");
        list.add("-metrics.audit.proxy.hosts");
        list.add(flinkConfig.getAuditProxyHosts());
        return list.toArray(new String[0]);
    }

}
