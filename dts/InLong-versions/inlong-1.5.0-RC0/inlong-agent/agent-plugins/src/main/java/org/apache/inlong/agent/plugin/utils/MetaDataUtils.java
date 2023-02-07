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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.CommonConstants;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_META_FILTER_BY_LABELS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_PROPERTIES;
import static org.apache.inlong.agent.constant.KubernetesConstants.CONTAINER_ID;
import static org.apache.inlong.agent.constant.KubernetesConstants.CONTAINER_NAME;
import static org.apache.inlong.agent.constant.KubernetesConstants.NAMESPACE;
import static org.apache.inlong.agent.constant.KubernetesConstants.POD_NAME;

/**
 * Metadata utils
 */
public class MetaDataUtils {

    private static final Gson GSON = new Gson();

    private static final String LOG_MARK = ".log";

    // standard log path for k8s
    private static final String FILE_NAME_PATTERN = "(^[-a-zA-Z0-9]+)_([a-zA-Z0-9-]+)_([a-zA-Z0-9-]+)(.log)";

    private static final Pattern PATTERN = Pattern.compile(FILE_NAME_PATTERN);

    /**
     * standard log for k8s
     *
     * get pod_name,namespace,container_name,container_id
     */
    public static Map<String, String> getLogInfo(String fileName) {
        Matcher matcher = PATTERN.matcher(fileName);
        Map<String, String> podInf = new HashMap<>();
        if (StringUtils.isBlank(fileName) || !matcher.matches()) {
            return podInf;
        }
        // file name example: /var/log/containers/<pod_name>_<namespace>_<container_name>-<continer_id>.log
        String[] str = fileName.split(CommonConstants.DELIMITER_UNDERLINE);
        podInf.put(POD_NAME, str[0]);
        podInf.put(NAMESPACE, str[1]);
        String[] containerInfo = str[2].split(CommonConstants.DELIMITER_HYPHEN);
        String containerId = containerInfo[containerInfo.length - 1].replace(LOG_MARK, "");
        String containerName = "";
        for (int i = 0; i < containerInfo.length - 1; i++) {
            if (i == containerInfo.length - 2) {
                containerName = containerName.concat(containerInfo[i]);
                break;
            }
            containerName = containerName.concat(containerInfo[i]).concat(CommonConstants.DELIMITER_HYPHEN);
        }
        podInf.put(CONTAINER_NAME, containerName);
        podInf.put(CONTAINER_ID, containerId);
        return podInf;
    }

    /**
     * standard log for k8s
     *
     * get labels of pod
     */
    public static Map<String, String> getPodLabels(JobProfile jobProfile) {
        if (Objects.isNull(jobProfile) || !jobProfile.hasKey(JOB_FILE_META_FILTER_BY_LABELS)) {
            return new HashMap<>();
        }
        String labels = jobProfile.get(JOB_FILE_META_FILTER_BY_LABELS);
        Type type = new TypeToken<HashMap<String, String>>() {
        }.getType();
        return GSON.fromJson(labels, type);
    }

    public static List<String> getNamespace(JobProfile jobProfile) {
        if (Objects.isNull(jobProfile) || !jobProfile.hasKey(JOB_FILE_PROPERTIES)) {
            return null;
        }
        String property = jobProfile.get(JOB_FILE_PROPERTIES);
        Type type = new TypeToken<HashMap<Integer, String>>() {
        }.getType();
        Map<String, String> properties = GSON.fromJson(property, type);
        return properties.keySet().stream().map(data -> {
            if (data.contains(NAMESPACE)) {
                return properties.get(data);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * standard log for k8s
     *
     * get name of pod
     */
    public static String getPodName(JobProfile jobProfile) {
        if (Objects.isNull(jobProfile) || !jobProfile.hasKey(JOB_FILE_PROPERTIES)) {
            return null;
        }
        String property = jobProfile.get(JOB_FILE_PROPERTIES);
        Type type = new TypeToken<HashMap<Integer, String>>() {
        }.getType();
        Map<String, String> properties = GSON.fromJson(property, type);
        List<String> podName = properties.keySet().stream().map(data -> {
            if (data.contains(POD_NAME)) {
                return properties.get(data);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return podName.isEmpty() ? null : podName.get(0);
    }
}
