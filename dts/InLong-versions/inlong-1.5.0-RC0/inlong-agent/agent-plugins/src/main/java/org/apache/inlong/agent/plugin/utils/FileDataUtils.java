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
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static org.apache.inlong.agent.constant.KubernetesConstants.NAMESPACE;
import static org.apache.inlong.agent.constant.KubernetesConstants.POD_NAME;

/**
 * File job utils
 */
public class FileDataUtils {

    public static final String KUBERNETES_LOG = "log";
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDataUtils.class);
    private static final Gson GSON = new Gson();

    /**
     * Get standard log for k8s
     */
    public static String getK8sJsonLog(String log, Boolean isJson) {
        if (!StringUtils.isNoneBlank(log)) {
            return "";
        }
        if (!isJson) {
            return log;
        }
        Type type = new TypeToken<HashMap<String, String>>() {
        }.getType();
        Map<String, String> logJson = GSON.fromJson(log, type);
        return logJson.getOrDefault(KUBERNETES_LOG, log);
    }

    /**
     * To judge json
     */
    public static boolean isJSON(String json) {
        boolean isJson;
        try {
            JsonObject convertedObject = new Gson().fromJson(json, JsonObject.class);
            isJson = convertedObject.isJsonObject();
        } catch (Exception exception) {
            return false;
        }
        return isJson;
    }

    /**
     * Filter file by conditions
     */
    public static Collection<File> filterFile(Collection<File> allFiles, JobProfile jobConf) {
        // filter file by labels
        Collection<File> files = null;
        try {
            files = filterByLabels(allFiles, jobConf);
        } catch (IOException e) {
            LOGGER.error("filter file error: ", e);
        }
        return files;
    }

    /**
     * Filter file by labels if standard log for k8s
     */
    private static Collection<File> filterByLabels(Collection<File> allFiles, JobProfile jobConf) throws IOException {
        Map<String, String> labelsMap = MetaDataUtils.getPodLabels(jobConf);
        if (labelsMap.isEmpty()) {
            return allFiles;
        }
        Collection<File> standardK8sLogFiles = new ArrayList<>();
        Iterator<File> iterator = allFiles.iterator();
        KubernetesClient client = PluginUtils.getKubernetesClient();
        while (iterator.hasNext()) {
            File file = getFile(labelsMap, iterator.next(), client);
            if (file == null) {
                continue;
            }
            standardK8sLogFiles.add(file);
        }
        return standardK8sLogFiles;
    }

    private static File getFile(Map<String, String> labelsMap, File file, KubernetesClient client) {
        Map<String, String> logInfo = MetaDataUtils.getLogInfo(file.getName());
        if (logInfo.isEmpty()) {
            return null;
        }
        PodResource podResource = client.pods().inNamespace(logInfo.get(NAMESPACE))
                .withName(logInfo.get(POD_NAME));
        if (Objects.isNull(podResource)) {
            return null;
        }
        Pod pod = podResource.get();
        Map<String, String> podLabels = pod.getMetadata().getLabels();
        boolean filterLabelStatus = false;
        for (String key : labelsMap.keySet()) {
            if (podLabels.containsKey(key) && labelsMap.get(key).contains(podLabels.get(key))) {
                filterLabelStatus = true;
                continue;
            }
            if (podLabels.containsKey(key) && !labelsMap.get(key).contains(podLabels.get(key))) {
                filterLabelStatus = false;
                break;
            }
        }
        return filterLabelStatus ? file : null;
    }

}
