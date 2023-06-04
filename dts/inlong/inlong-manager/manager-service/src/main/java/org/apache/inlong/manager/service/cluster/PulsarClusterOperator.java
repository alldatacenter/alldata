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

package org.apache.inlong.manager.service.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterDTO;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterRequest;
import org.apache.inlong.manager.service.resource.queue.pulsar.PulsarUtils;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

/**
 * Pulsar cluster operator.
 */
@Service
public class PulsarClusterOperator extends AbstractClusterOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PulsarClusterOperator.class);

    private static final String SERVICE_URL_PREFIX = "pulsar://";

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String clusterType) {
        return getClusterType().equals(clusterType);
    }

    @Override
    public String getClusterType() {
        return ClusterType.PULSAR;
    }

    @Override
    public ClusterInfo getFromEntity(InlongClusterEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }

        PulsarClusterInfo pulsarInfo = new PulsarClusterInfo();
        CommonBeanUtils.copyProperties(entity, pulsarInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            PulsarClusterDTO dto = PulsarClusterDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, pulsarInfo);
        }

        return pulsarInfo;
    }

    @Override
    protected void setTargetEntity(ClusterRequest request, InlongClusterEntity targetEntity) {
        PulsarClusterRequest pulsarRequest = (PulsarClusterRequest) request;
        CommonBeanUtils.copyProperties(pulsarRequest, targetEntity, true);
        try {
            PulsarClusterDTO dto = PulsarClusterDTO.getFromRequest(pulsarRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
            LOGGER.debug("success to set entity for pulsar cluster");
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_INFO_INCORRECT,
                    String.format("serialize extParams of Pulsar Cluster failure: %s", e.getMessage()));
        }
    }

    @Override
    public Boolean testConnection(ClusterRequest request) {
        PulsarClusterRequest pulsarRequest = (PulsarClusterRequest) request;
        PulsarClusterInfo pulsarInfo = new PulsarClusterInfo();
        CommonBeanUtils.copyProperties(pulsarRequest, pulsarInfo);

        testConnectServiceUrl(pulsarInfo.getUrl());
        return testConnectAdminUrl(pulsarInfo);
    }

    /**
     * Test connect for Pulsar AdminUrl
     *
     * @param pulsarInfo pulsar cluster info
     * @return
     */
    private Boolean testConnectAdminUrl(PulsarClusterInfo pulsarInfo) {
        try (PulsarAdmin pulsarAdmin = PulsarUtils.getPulsarAdmin(pulsarInfo)) {
            // test connect for pulsar adminUrl
            pulsarAdmin.tenants().getTenants();
            return true;
        } catch (Exception e) {
            String errMsg = String.format("Pulsar connection failed for AdminUrl=%s", pulsarInfo.getAdminUrl());
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }
    }

    /**
     * Test connect for Pulsar ServiceUrl
     *
     * @param serviceUrl Pulsar serviceUrl, such as: pulsar://127.0.0.1:6650,127.0.0.2:6650
     */
    private void testConnectServiceUrl(String serviceUrl) {
        // test connect for Pulsar ServiceUrl
        Preconditions.expectNotNull(serviceUrl, "Pulsar ServiceUrl is empty");
        Preconditions.expectTrue(serviceUrl.startsWith(SERVICE_URL_PREFIX),
                String.format("Pulsar ServiceUrl=%s should starts with %s", serviceUrl, SERVICE_URL_PREFIX));

        String hostPortStr = serviceUrl.replaceAll(SERVICE_URL_PREFIX, "");

        boolean successConnect = Arrays.stream(hostPortStr.split(InlongConstants.COMMA))
                // Only when all addresses are reachable, it's considered successful
                .allMatch(hostPort -> {
                    String[] hostPortArr = hostPort.split(InlongConstants.COLON);
                    Preconditions.expectTrue(hostPortArr.length >= 2,
                            String.format("Pulsar ServiceUrl=%s should has ip and port, such as '127.0.0.1:6650'",
                                    serviceUrl));

                    String host = hostPortArr[0];
                    int port = Integer.parseInt(hostPortArr[1]);

                    try (Socket socket = new Socket()) {
                        SocketAddress socketAddress = new InetSocketAddress(host, port);
                        socket.connect(socketAddress, 30000);
                        return true;
                    } catch (IOException e) {
                        String errMsg = String.format("Pulsar connection failed for ServiceUrl=%s", hostPort);
                        LOGGER.error(errMsg, e);
                        return false;
                    }
                });

        Preconditions.expectTrue(successConnect,
                String.format("Pulsar connection failed for ServiceUrl=%s", hostPortStr));
    }
}
