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

package org.apache.inlong.manager.service.mq.util;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.conversion.ConversionHandle;
import org.apache.inlong.manager.common.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.common.pojo.pulsar.PulsarTopicBean;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.service.cluster.InlongClusterServiceImpl;
import org.apache.pulsar.client.admin.Namespaces;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.common.policies.data.PersistencePolicies;
import org.apache.pulsar.common.policies.data.RetentionPolicies;
import org.apache.pulsar.common.policies.data.TenantInfoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pulsar operator, supports creating topics and creating subscription.
 */
@Service
public class PulsarOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongClusterServiceImpl.class);
    private static final String PULSAR_QUEUE_TYPE_SERIAL = "SERIAL";

    @Autowired
    private ConversionHandle conversionHandle;

    public void createTenant(PulsarAdmin pulsarAdmin, String tenant) throws PulsarAdminException {
        LOGGER.info("begin to create pulsar tenant={}", tenant);

        Preconditions.checkNotEmpty(tenant, "Tenant cannot be empty");
        try {
            List<String> clusters = PulsarUtils.getPulsarClusters(pulsarAdmin);
            boolean exists = this.tenantIsExists(pulsarAdmin, tenant);
            if (exists) {
                LOGGER.warn("pulsar tenant={} already exists, skip to create", tenant);
                return;
            }
            TenantInfoImpl tenantInfo = new TenantInfoImpl();
            tenantInfo.setAllowedClusters(Sets.newHashSet(clusters));
            tenantInfo.setAdminRoles(Sets.newHashSet());
            pulsarAdmin.tenants().createTenant(tenant, tenantInfo);
            LOGGER.info("success to create pulsar tenant={}", tenant);
        } catch (PulsarAdminException e) {
            LOGGER.error("failed to create pulsar tenant=" + tenant, e);
            throw e;
        }
    }

    public void createNamespace(PulsarAdmin pulsarAdmin, InlongPulsarInfo pulsarInfo,
            String tenant, String namespace) throws PulsarAdminException {
        Preconditions.checkNotNull(tenant, "pulsar tenant cannot be empty during create namespace");
        Preconditions.checkNotNull(namespace, "pulsar namespace cannot be empty during create namespace");

        String namespaceName = tenant + "/" + namespace;
        LOGGER.info("begin to create namespace={}", namespaceName);

        try {
            // Check whether the namespace exists, and create it if it does not exist
            boolean isExists = this.namespacesIsExists(pulsarAdmin, tenant, namespace);
            if (isExists) {
                LOGGER.warn("namespace={} already exists, skip to create", namespaceName);
                return;
            }

            List<String> clusters = PulsarUtils.getPulsarClusters(pulsarAdmin);
            Namespaces namespaces = pulsarAdmin.namespaces();
            namespaces.createNamespace(namespaceName, Sets.newHashSet(clusters));

            // Configure message TTL
            Integer ttl = pulsarInfo.getTtl();
            if (ttl > 0) {
                namespaces.setNamespaceMessageTTL(namespaceName, conversionHandle.handleConversion(ttl,
                        pulsarInfo.getTtlUnit().toLowerCase() + "_seconds"));
            }

            // retentionTimeInMinutes retentionSizeInMB
            Integer retentionTime = pulsarInfo.getRetentionTime();
            if (retentionTime > 0) {
                retentionTime = conversionHandle.handleConversion(retentionTime,
                        pulsarInfo.getRetentionTimeUnit().toLowerCase() + "_minutes");
            }
            Integer retentionSize = pulsarInfo.getRetentionSize();
            if (retentionSize > 0) {
                retentionSize = conversionHandle.handleConversion(retentionSize,
                        pulsarInfo.getRetentionSizeUnit().toLowerCase() + "_mb");
            }

            // Configure retention policies
            RetentionPolicies retentionPolicies = new RetentionPolicies(retentionTime, retentionSize);
            namespaces.setRetention(namespaceName, retentionPolicies);

            // Configure persistence policies
            PersistencePolicies persistencePolicies = new PersistencePolicies(pulsarInfo.getEnsemble(),
                    pulsarInfo.getWriteQuorum(), pulsarInfo.getAckQuorum(), 0);
            namespaces.setPersistence(namespaceName, persistencePolicies);
            LOGGER.info("success to create namespace={}", namespaceName);
        } catch (PulsarAdminException e) {
            LOGGER.error("failed to create namespace=" + namespaceName, e);
            throw e;
        }
    }

    public void createTopic(PulsarAdmin pulsarAdmin, PulsarTopicBean topicBean) throws PulsarAdminException {
        Preconditions.checkNotNull(topicBean, "pulsar topic info cannot be empty");

        String tenant = topicBean.getTenant();
        String namespace = topicBean.getNamespace();
        String topic = topicBean.getTopicName();
        String topicFullName = tenant + "/" + namespace + "/" + topic;

        // Topic will be returned if it exists, and created if it does not exist
        if (topicIsExists(pulsarAdmin, tenant, namespace, topic)) {
            LOGGER.warn("pulsar topic={} already exists in {}", topicFullName, pulsarAdmin.getServiceUrl());
            return;
        }

        try {
            String queueModule = topicBean.getQueueModule();
            // create partition topic
            if (PULSAR_QUEUE_TYPE_SERIAL.equalsIgnoreCase(queueModule)) {
                pulsarAdmin.topics().createPartitionedTopic(topicFullName, 1);
            } else {
                List<String> clusters = PulsarUtils.getPulsarClusters(pulsarAdmin);
                // The number of brokers as the default value of topic partition
                List<String> brokers = pulsarAdmin.brokers().getActiveBrokers(clusters.get(0));
                Integer numPartitions = brokers.size();
                if (topicBean.getNumPartitions() > 0) {
                    numPartitions = topicBean.getNumPartitions();
                }
                pulsarAdmin.topics().createPartitionedTopic(topicFullName, numPartitions);
            }

            LOGGER.info("success to create topic={}", topicFullName);
        } catch (Exception e) {
            LOGGER.error("failed to create topic=" + topicFullName, e);
            throw e;
        }
    }

    public void createSubscription(PulsarAdmin pulsarAdmin, PulsarTopicBean topicBean, String subscription)
            throws PulsarAdminException {
        Preconditions.checkNotNull(topicBean, "can not find tenant information to create subscription");
        Preconditions.checkNotNull(subscription, "subscription cannot be empty during creating subscription");

        String topicName = topicBean.getTenant() + "/" + topicBean.getNamespace() + "/" + topicBean.getTopicName();
        LOGGER.info("begin to create pulsar subscription={} for topic={}", subscription, topicName);

        try {
            boolean isExists = this.subscriptionIsExists(pulsarAdmin, topicName, subscription);
            if (!isExists) {
                pulsarAdmin.topics().createSubscription(topicName, subscription, MessageId.latest);
                LOGGER.info("success to create subscription={}", subscription);
            } else {
                LOGGER.warn("pulsar subscription={} already exists, skip to create", subscription);
            }
        } catch (Exception e) {
            LOGGER.error("failed to create pulsar subscription=" + subscription, e);
            throw e;
        }
    }

    public void createSubscriptions(PulsarAdmin pulsarAdmin, String subscription, PulsarTopicBean topicBean,
            List<String> topicList) throws PulsarAdminException {
        for (String topic : topicList) {
            topicBean.setTopicName(topic);
            this.createSubscription(pulsarAdmin, topicBean, subscription);
        }
        LOGGER.info("success to create subscription={} for multiple topics={}", subscription, topicList);
    }

    /**
     * Check if Pulsar tenant exists
     */
    private boolean tenantIsExists(PulsarAdmin pulsarAdmin, String tenant) throws PulsarAdminException {
        List<String> tenantList = pulsarAdmin.tenants().getTenants();
        return tenantList.contains(tenant);
    }

    /**
     * Check whether the specified Namespace exists under the specified Tenant
     */
    private boolean namespacesIsExists(PulsarAdmin pulsarAdmin, String tenant, String namespace)
            throws PulsarAdminException {
        List<String> namespaceList = pulsarAdmin.namespaces().getNamespaces(tenant);
        return namespaceList.contains(tenant + "/" + namespace);
    }

    /**
     * Verify whether the specified Topic exists under the specified Tenant/Namespace
     *
     * @apiNote cannot compare whether the string contains, otherwise it may be misjudged, such as:
     *         Topic "ab" does not exist, but if "abc" exists, "ab" will be mistakenly judged to exist
     */
    public boolean topicIsExists(PulsarAdmin pulsarAdmin, String tenant, String namespace, String topic)
            throws PulsarAdminException {
        if (StringUtils.isBlank(topic)) {
            return true;
        }

        // persistent://tenant/namespace/topic
        List<String> topicList = pulsarAdmin.topics().getPartitionedTopicList(tenant + "/" + namespace);
        for (String t : topicList) {
            t = t.substring(t.lastIndexOf("/") + 1); // Cannot contain /
            if (topic.equals(t)) {
                return true;
            }
        }
        return false;
    }

    public boolean subscriptionIsExists(PulsarAdmin pulsarAdmin, String topic, String subscription) {
        try {
            List<String> subscriptionList = pulsarAdmin.topics().getSubscriptions(topic);
            return subscriptionList.contains(subscription);
        } catch (PulsarAdminException e) {
            LOGGER.error("failed to check the subscription=" + subscription + " exists for topic=" + topic, e);
            return false;
        }
    }
}
