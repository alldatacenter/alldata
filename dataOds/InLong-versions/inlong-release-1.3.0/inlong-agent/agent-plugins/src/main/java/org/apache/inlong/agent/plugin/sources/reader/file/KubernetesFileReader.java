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
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.CommonConstants;
import org.apache.inlong.agent.plugin.utils.MetaDataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.inlong.agent.constant.KubernetesConstants.CONTAINER_ID;
import static org.apache.inlong.agent.constant.KubernetesConstants.CONTAINER_NAME;
import static org.apache.inlong.agent.constant.KubernetesConstants.HTTPS;
import static org.apache.inlong.agent.constant.KubernetesConstants.KUBERNETES_SERVICE_HOST;
import static org.apache.inlong.agent.constant.KubernetesConstants.KUBERNETES_SERVICE_PORT;
import static org.apache.inlong.agent.constant.KubernetesConstants.METADATA_CONTAINER_ID;
import static org.apache.inlong.agent.constant.KubernetesConstants.METADATA_CONTAINER_NAME;
import static org.apache.inlong.agent.constant.KubernetesConstants.METADATA_NAMESPACE;
import static org.apache.inlong.agent.constant.KubernetesConstants.METADATA_POD_LABEL;
import static org.apache.inlong.agent.constant.KubernetesConstants.METADATA_POD_NAME;
import static org.apache.inlong.agent.constant.KubernetesConstants.METADATA_POD_UID;
import static org.apache.inlong.agent.constant.KubernetesConstants.NAMESPACE;
import static org.apache.inlong.agent.constant.KubernetesConstants.POD_NAME;

/**
 * k8s file reader
 */
public final class KubernetesFileReader extends AbstractFileReader {

    private static final Logger log = LoggerFactory.getLogger(KubernetesFileReader.class);
    private static final Gson GSON = new Gson();

    private KubernetesClient client;

    KubernetesFileReader(FileReaderOperator fileReaderOperator) {
        super.fileReaderOperator = fileReaderOperator;
    }

    public void getData() {
        if (Objects.nonNull(client) && Objects.nonNull(fileReaderOperator.metadata)) {
            return;
        }
        try {
            client = getKubernetesClient();
        } catch (IOException e) {
            log.error("get k8s client error: ", e);
        }
        fileReaderOperator.metadata = getK8sMetadata(fileReaderOperator.jobConf);
    }

    // TODO only support default config in the POD
    private KubernetesClient getKubernetesClient() throws IOException {
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
    public Map<String, String> getK8sMetadata(JobProfile jobConf) {
        if (Objects.isNull(jobConf)) {
            return null;
        }
        Map<String, String> k8sInfo = MetaDataUtils.getLogInfo(fileReaderOperator.file.getName());
        log.info("k8s information size:{}", k8sInfo.size());
        Map<String, String> metadata = new HashMap<>();
        if (k8sInfo.isEmpty()) {
            return metadata;
        }

        metadata.put(METADATA_NAMESPACE, k8sInfo.get(NAMESPACE));
        metadata.put(METADATA_CONTAINER_NAME, k8sInfo.get(CONTAINER_NAME));
        metadata.put(METADATA_CONTAINER_ID, k8sInfo.get(CONTAINER_ID));
        metadata.put(METADATA_POD_NAME, k8sInfo.get(POD_NAME));

        PodResource podResource = client.pods().inNamespace(k8sInfo.get(NAMESPACE))
                .withName(k8sInfo.get(POD_NAME));
        if (Objects.isNull(podResource)) {
            return metadata;
        }
        Pod pod = podResource.get();
        PodList podList = client.pods().inNamespace(k8sInfo.get(NAMESPACE))
                .withLabels(MetaDataUtils.getPodLabels(jobConf)).list();
        podList.getItems().forEach(data -> {
            if (data.equals(pod)) {
                metadata.put(METADATA_POD_UID, pod.getMetadata().getUid());
                metadata.put(METADATA_POD_LABEL, GSON.toJson(pod.getMetadata().getLabels()));
            }
        });
        return metadata;
    }
}
