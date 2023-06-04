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

package org.apache.inlong.manager.service.resource.queue.pulsar;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.PulsarClientException;

import java.util.List;

/**
 * Pulsar connection utils
 */
@Slf4j
public class PulsarUtils {

    private PulsarUtils() {
    }

    /**
     * Get pulsar admin info
     */
    public static PulsarAdmin getPulsarAdmin(PulsarClusterInfo pulsarCluster) throws PulsarClientException {
        Preconditions.expectNotBlank(pulsarCluster.getAdminUrl(), ErrorCodeEnum.INVALID_PARAMETER,
                "Pulsar adminUrl cannot be empty");
        PulsarAdmin pulsarAdmin;
        if (StringUtils.isEmpty(pulsarCluster.getToken())) {
            pulsarAdmin = getPulsarAdmin(pulsarCluster.getAdminUrl());
        } else {
            pulsarAdmin = getPulsarAdmin(pulsarCluster.getAdminUrl(), pulsarCluster.getToken());
        }
        return pulsarAdmin;
    }

    /**
     * Get the pulsar admin from the given service URL.
     *
     * @apiNote It must be closed after use.
     */
    public static PulsarAdmin getPulsarAdmin(String serviceHttpUrl) throws PulsarClientException {
        return PulsarAdmin.builder().serviceHttpUrl(serviceHttpUrl).build();
    }

    /**
     * Get the pulsar admin from the given service URL and token.
     * <p/>
     * Currently only token is supported as an authentication type.
     *
     * @apiNote It must be closed after use.
     */
    private static PulsarAdmin getPulsarAdmin(String serviceHttpUrl, String token) throws PulsarClientException {
        return PulsarAdmin.builder().serviceHttpUrl(serviceHttpUrl)
                .authentication(AuthenticationFactory.token(token)).build();
    }

    /**
     * Get pulsar cluster info list.
     */
    public static List<String> getPulsarClusters(PulsarAdmin pulsarAdmin) throws PulsarAdminException {
        return pulsarAdmin.clusters().getClusters();
    }

    /**
     * Get pulsar cluster service url.
     */
    public static String getServiceUrl(PulsarAdmin pulsarAdmin, String pulsarCluster) throws PulsarAdminException {
        return pulsarAdmin.clusters().getCluster(pulsarCluster).getServiceUrl();
    }

}
