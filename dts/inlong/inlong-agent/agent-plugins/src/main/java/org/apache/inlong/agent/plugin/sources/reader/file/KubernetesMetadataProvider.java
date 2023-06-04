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

package org.apache.inlong.agent.plugin.sources.reader.file;

import com.google.gson.Gson;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.plugin.utils.MetaDataUtils;
import org.apache.inlong.agent.plugin.utils.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static org.apache.inlong.agent.constant.KubernetesConstants.CONTAINER_ID;
import static org.apache.inlong.agent.constant.KubernetesConstants.CONTAINER_NAME;
import static org.apache.inlong.agent.constant.KubernetesConstants.NAMESPACE;
import static org.apache.inlong.agent.constant.KubernetesConstants.POD_NAME;
import static org.apache.inlong.agent.constant.MetadataConstants.METADATA_CONTAINER_ID;
import static org.apache.inlong.agent.constant.MetadataConstants.METADATA_CONTAINER_NAME;
import static org.apache.inlong.agent.constant.MetadataConstants.METADATA_NAMESPACE;
import static org.apache.inlong.agent.constant.MetadataConstants.METADATA_POD_LABEL;
import static org.apache.inlong.agent.constant.MetadataConstants.METADATA_POD_NAME;
import static org.apache.inlong.agent.constant.MetadataConstants.METADATA_POD_UID;

/**
 * k8s file reader
 */
public final class KubernetesMetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(KubernetesMetadataProvider.class);
    private static final Gson GSON = new Gson();

    private KubernetesClient client;
    private FileReaderOperator fileReaderOperator;

    KubernetesMetadataProvider(FileReaderOperator fileReaderOperator) {
        this.fileReaderOperator = fileReaderOperator;
    }

    public void getData() {
        if (Objects.nonNull(client) && Objects.nonNull(fileReaderOperator.metadata)) {
            return;
        }
        try {
            client = PluginUtils.getKubernetesClient();
        } catch (IOException e) {
            log.error("get k8s client error: ", e);
        }
        getK8sMetadata(fileReaderOperator.jobConf);
    }

    /**
     * get PODS of kubernetes information
     */
    public PodList getPods() {
        if (Objects.isNull(client)) {
            return null;
        }
        MixedOperation<Pod, PodList, PodResource> pods = client.pods();
        return pods.list();
    }

    /**
     * get pod metadata by namespace and pod name
     */
    public void getK8sMetadata(JobProfile jobConf) {
        if (Objects.isNull(jobConf)) {
            return;
        }
        Map<String, String> k8sInfo = MetaDataUtils.getLogInfo(fileReaderOperator.file.getName());
        log.info("file name is: {}, k8s information size: {}", fileReaderOperator.file.getName(), k8sInfo.size());
        if (k8sInfo.isEmpty()) {
            return;
        }

        Map<String, String> metadata = fileReaderOperator.metadata;
        metadata.put(METADATA_NAMESPACE, k8sInfo.get(NAMESPACE));
        metadata.put(METADATA_CONTAINER_NAME, k8sInfo.get(CONTAINER_NAME));
        metadata.put(METADATA_CONTAINER_ID, k8sInfo.get(CONTAINER_ID));
        metadata.put(METADATA_POD_NAME, k8sInfo.get(POD_NAME));

        PodResource podResource = client.pods().inNamespace(k8sInfo.get(NAMESPACE))
                .withName(k8sInfo.get(POD_NAME));
        if (Objects.isNull(podResource)) {
            return;
        }
        Pod pod = podResource.get();
        PodList podList = client.pods().inNamespace(k8sInfo.get(NAMESPACE)).list();
        podList.getItems().forEach(data -> {
            if (data.equals(pod)) {
                metadata.put(METADATA_POD_UID, pod.getMetadata().getUid());
                metadata.put(METADATA_POD_LABEL, GSON.toJson(pod.getMetadata().getLabels()));
            }
        });
        return;
    }
}
