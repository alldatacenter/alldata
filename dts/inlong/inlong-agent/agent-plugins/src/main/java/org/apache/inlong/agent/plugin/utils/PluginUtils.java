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

package org.apache.inlong.agent.plugin.utils;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.CommonConstants;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.plugin.trigger.PathPattern;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.pulsar.client.api.CompressionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.inlong.agent.constant.CommonConstants.AGENT_COLON;
import static org.apache.inlong.agent.constant.CommonConstants.AGENT_NIX_OS;
import static org.apache.inlong.agent.constant.CommonConstants.AGENT_NUX_OS;
import static org.apache.inlong.agent.constant.CommonConstants.AGENT_OS_NAME;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_FILE_MAX_NUM;
import static org.apache.inlong.agent.constant.CommonConstants.FILE_MAX_NUM;
import static org.apache.inlong.agent.constant.JobConstants.JOB_DIR_FILTER_BLACKLIST;
import static org.apache.inlong.agent.constant.JobConstants.JOB_DIR_FILTER_PATTERNS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_TIME_OFFSET;
import static org.apache.inlong.agent.constant.JobConstants.JOB_RETRY_TIME;
import static org.apache.inlong.agent.constant.KubernetesConstants.HTTPS;
import static org.apache.inlong.agent.constant.KubernetesConstants.KUBERNETES_SERVICE_HOST;
import static org.apache.inlong.agent.constant.KubernetesConstants.KUBERNETES_SERVICE_PORT;

/**
 * Utils for plugin package.
 */
@Slf4j
public class PluginUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtils.class);

    /**
     * convert string compress type to enum compress type
     */
    public static CompressionType convertType(String type) {
        switch (type) {
            case "lz4":
                return CompressionType.LZ4;
            case "zlib":
                return CompressionType.ZLIB;
            case "zstd":
                return CompressionType.ZSTD;
            case "snappy":
                return CompressionType.SNAPPY;
            case "none":
            default:
                return CompressionType.NONE;
        }
    }

    /**
     * scan and return files based on job dir conf
     */
    public static Collection<File> findSuitFiles(JobProfile jobConf) {
        Set<String> dirPatterns = Stream.of(
                jobConf.get(JOB_DIR_FILTER_PATTERNS).split(",")).collect(Collectors.toSet());
        Set<String> blackList = Stream.of(
                jobConf.get(JOB_DIR_FILTER_BLACKLIST, "").split(","))
                .filter(black -> !StringUtils.isBlank(black))
                .collect(Collectors.toSet());
        LOGGER.info("start to find files with dir pattern {}", dirPatterns);

        Set<PathPattern> pathPatterns =
                PathPattern.buildPathPattern(dirPatterns, jobConf.get(JOB_FILE_TIME_OFFSET, null), blackList);
        updateRetryTime(jobConf, pathPatterns);
        int maxFileNum = jobConf.getInt(FILE_MAX_NUM, DEFAULT_FILE_MAX_NUM);
        LOGGER.info("dir pattern {}, max file num {}", dirPatterns, maxFileNum);
        Collection<File> allFiles = new ArrayList<>();
        pathPatterns.forEach(pathPattern -> allFiles.addAll(pathPattern.walkSuitableFiles(maxFileNum)));
        return allFiles;
    }

    /**
     * if the job is retry job, the date is determined
     */
    public static void updateRetryTime(JobProfile jobConf, Collection<PathPattern> patterns) {
        if (jobConf.hasKey(JOB_RETRY_TIME)) {
            LOGGER.info("job {} is retry job with specific time, update file time to {}"
                    + "", jobConf.toJsonStr(), jobConf.get(JOB_RETRY_TIME));
            patterns.forEach(pattern -> pattern.updateDateFormatRegex(jobConf.get(JOB_RETRY_TIME)));
        }
    }

    /**
     * convert a file of trigger dir to a subtask JobProfile of TriggerProfile
     */
    public static JobProfile copyJobProfile(TriggerProfile triggerProfile, File pendingFile) {
        JobProfile copiedProfile = TriggerProfile.parseJsonStr(triggerProfile.toJsonStr());
        String md5 = AgentUtils.getFileMd5(pendingFile);
        copiedProfile.set(pendingFile.getAbsolutePath() + ".md5", md5);
        copiedProfile.set(JobConstants.JOB_TRIGGER, null); // del trigger id
        copiedProfile.set(JobConstants.JOB_DIR_FILTER_PATTERNS, pendingFile.getAbsolutePath());
        return copiedProfile;
    }

    public static List<String> getLocalIpList() {
        List<String> allIps = new ArrayList<>();
        try {
            String os = System.getProperty(AGENT_OS_NAME).toLowerCase();
            if (os.contains(AGENT_NIX_OS) || os.contains(AGENT_NUX_OS)) {
                /* Deal with linux platform. */
                Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = nis.nextElement();
                    addIp(allIps, ni);
                }
            } else {
                /* Deal with windows platform. */
                allIps.add(InetAddress.getLocalHost().getHostAddress());
            }
        } catch (Exception e) {
            LOGGER.error("get local ip list fail with ex:", e);
        }
        return allIps;
    }

    private static void addIp(List<String> allIps, NetworkInterface ni) {
        Enumeration<InetAddress> ias = ni.getInetAddresses();
        while (ias.hasMoreElements()) {
            InetAddress ia = ias.nextElement();
            if (!ia.isLoopbackAddress() && ia.getHostAddress().contains(AGENT_COLON)) {
                allIps.add(ia.getHostAddress());
            }
        }
    }

    // TODO only support default config in the POD
    public static KubernetesClient getKubernetesClient() throws IOException {
        String ip = System.getenv(KUBERNETES_SERVICE_HOST);
        String port = System.getenv(KUBERNETES_SERVICE_PORT);
        if (Objects.isNull(ip) && Objects.isNull(port)) {
            throw new RuntimeException("get k8s client error,k8s env ip and port is null");
        }
        String maserUrl = HTTPS.concat(ip).concat(CommonConstants.AGENT_COLON).concat(port);
        Config config = new ConfigBuilder()
                .withMasterUrl(maserUrl)
                .withCaCertFile(Config.KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH)
                .withOauthToken(new String(
                        Files.readAllBytes((new File(Config.KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH)).toPath())))
                .build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }

}
