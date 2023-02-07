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

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.conversion.ConversionHandle;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.pojo.queue.pulsar.PulsarTopicInfo;
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
import java.util.Map;

/**
 * Pulsar operator, supports creating topics and creating subscription.
 */
@Service
public class PulsarOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongClusterServiceImpl.class);
    private static final int MAX_PARTITION = 100;
    private static final int RETRY_TIMES = 3;
    private static final int DELAY_SECONDS = 5;

    @Autowired
    private ConversionHandle conversionHandle;

    /**
     * Create Pulsar tenant
     */
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

    /**
     * Create Pulsar namespace
     */
    public void createNamespace(PulsarAdmin pulsarAdmin, InlongPulsarInfo pulsarInfo, String tenant, String namespace)
            throws PulsarAdminException {
        Preconditions.checkNotNull(tenant, "pulsar tenant cannot be empty during create namespace");
        Preconditions.checkNotNull(namespace, "pulsar namespace cannot be empty during create namespace");

        String namespaceName = tenant + "/" + namespace;
        LOGGER.info("begin to create namespace={}", namespaceName);
        try {
            // Check whether the namespace exists, and create it if it does not exist
            boolean isExists = this.namespaceExists(pulsarAdmin, tenant, namespace);
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

    /**
     * Create Pulsar topic
     */
    public void createTopic(PulsarAdmin pulsarAdmin, PulsarTopicInfo topicInfo) throws PulsarAdminException {
        Preconditions.checkNotNull(topicInfo, "pulsar topic info cannot be empty");
        String tenant = topicInfo.getTenant();
        String namespace = topicInfo.getNamespace();
        String topicName = topicInfo.getTopicName();
        String fullTopicName = tenant + "/" + namespace + "/" + topicName;

        // Topic will be returned if it exists, and created if it does not exist
        if (topicExists(pulsarAdmin, tenant, namespace, topicName,
                InlongConstants.PULSAR_QUEUE_TYPE_PARALLEL.equals(topicInfo.getQueueModule()))) {
            LOGGER.warn("pulsar topic={} already exists in {}", fullTopicName, pulsarAdmin.getServiceUrl());
            return;
        }

        try {
            if (InlongConstants.PULSAR_QUEUE_TYPE_SERIAL.equals(topicInfo.getQueueModule())) {
                pulsarAdmin.topics().createNonPartitionedTopic(fullTopicName);
                String res = pulsarAdmin.lookups().lookupTopic(fullTopicName);
                LOGGER.info("success to create topic={}, lookup result is {}", fullTopicName, res);
            } else {
                // The number of brokers as the default value of topic partition
                List<String> clusters = PulsarUtils.getPulsarClusters(pulsarAdmin);
                Integer numPartitions = topicInfo.getNumPartitions();
                if (numPartitions < 0 || numPartitions <= MAX_PARTITION) {
                    List<String> brokers = pulsarAdmin.brokers().getActiveBrokers(clusters.get(0));
                    numPartitions = brokers.size();
                }

                pulsarAdmin.topics().createPartitionedTopic(fullTopicName, numPartitions);
                Map<String, String> res = pulsarAdmin.lookups().lookupPartitionedTopic(fullTopicName);
                // if lookup failed (res.size not equals the partition number)
                if (res.keySet().size() != numPartitions) {
                    // look up partition failed, retry to get partition numbers
                    for (int i = 0; (i < RETRY_TIMES && res.keySet().size() != numPartitions); i++) {
                        res = pulsarAdmin.lookups().lookupPartitionedTopic(fullTopicName);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            LOGGER.error("Thread has been interrupted");
                        }
                    }
                }
                if (numPartitions != res.keySet().size()) {
                    throw new PulsarAdminException("The number of partitions not equal to lookupPartitionedTopic");
                }
                LOGGER.info("success to create topic={}", fullTopicName);
            }
        } catch (PulsarAdminException e) {
            LOGGER.error("failed to create topic=" + fullTopicName, e);
            throw e;
        }
    }

    /**
     * Force delete Pulsar topic
     */
    public void forceDeleteTopic(PulsarAdmin pulsarAdmin, PulsarTopicInfo topicInfo) throws PulsarAdminException {
        Preconditions.checkNotNull(topicInfo, "pulsar topic info cannot be empty");

        String tenant = topicInfo.getTenant();
        String namespace = topicInfo.getNamespace();
        String topic = topicInfo.getTopicName();
        String fullTopicName = tenant + "/" + namespace + "/" + topic;

        // Topic will be returned if it not exists
        if (topicExists(pulsarAdmin, tenant, namespace, topic,
                InlongConstants.PULSAR_QUEUE_TYPE_PARALLEL.equals(topicInfo.getQueueModule()))) {
            LOGGER.warn("pulsar topic={} already delete", fullTopicName);
            return;
        }

        try {
            pulsarAdmin.topics().delete(fullTopicName, true);
            LOGGER.info("success to delete topic={}", fullTopicName);
        } catch (PulsarAdminException e) {
            LOGGER.error("failed to delete topic=" + fullTopicName, e);
            throw e;
        }
    }

    /**
     * Create a Pulsar subscription for the given topic
     */
    public void createSubscription(PulsarAdmin pulsarAdmin, String fullTopicName, String queueModule,
            String subscription) throws PulsarAdminException {
        LOGGER.info("begin to create pulsar subscription={} for topic={}", subscription, fullTopicName);
        try {
            boolean isExists = this.subscriptionExists(pulsarAdmin, fullTopicName, subscription,
                    InlongConstants.PULSAR_QUEUE_TYPE_PARALLEL.equals(queueModule));
            if (isExists) {
                LOGGER.warn("pulsar subscription={} already exists, skip to create", subscription);
                return;
            }
            pulsarAdmin.topics().createSubscription(fullTopicName, subscription, MessageId.latest);
            LOGGER.info("success to create subscription={}", subscription);
        } catch (PulsarAdminException e) {
            LOGGER.error("failed to create pulsar subscription=" + subscription, e);
            throw e;
        }
    }

    /**
     * Create a Pulsar subscription for the specified topic list
     */
    public void createSubscriptions(PulsarAdmin pulsarAdmin, String subscription, PulsarTopicInfo topicInfo,
            List<String> topicList) throws PulsarAdminException {
        for (String topic : topicList) {
            topicInfo.setTopicName(topic);
            String fullTopicName = topicInfo.getTenant() + "/" + topicInfo.getNamespace() + "/" + topic;
            this.createSubscription(pulsarAdmin, fullTopicName, topicInfo.getQueueModule(), subscription);
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
     * Check whether the Pulsar namespace exists under the specified tenant
     */
    private boolean namespaceExists(PulsarAdmin pulsarAdmin, String tenant, String namespace)
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
    public boolean topicExists(PulsarAdmin pulsarAdmin, String tenant, String namespace, String topicName,
            boolean isPartitioned) {
        if (StringUtils.isBlank(topicName)) {
            return true;
        }

        // persistent://tenant/namespace/topic
        List<String> topicList;
        boolean topicExists = false;
        try {
            if (isPartitioned) {
                topicList = pulsarAdmin.topics().getPartitionedTopicList(tenant + "/" + namespace);
            } else {
                topicList = pulsarAdmin.topics().getList(tenant + "/" + namespace);
            }
            for (String t : topicList) {
                t = t.substring(t.lastIndexOf("/") + 1); // not contains /
                if (!isPartitioned) {
                    int suffixIndex = t.lastIndexOf("-partition-");
                    if (suffixIndex > 0) {
                        t = t.substring(0, suffixIndex);
                    }
                }
                if (topicName.equals(t)) {
                    topicExists = true;
                    break;
                }
            }
        } catch (PulsarAdminException pe) {
            LOGGER.error("check if the pulsar topic={} exists error, begin retry", topicName, pe);
            int count = 0;
            try {
                while (!topicExists && ++count <= RETRY_TIMES) {
                    LOGGER.info("check whether the pulsar topic={} exists error, try count={}", topicName, count);
                    Thread.sleep(DELAY_SECONDS);

                    topicList = pulsarAdmin.topics().getPartitionedTopicList(tenant + "/" + namespace);
                    for (String t : topicList) {
                        t = t.substring(t.lastIndexOf("/") + 1);
                        if (topicName.equals(t)) {
                            topicExists = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("after retry, check if the pulsar topic={} exists still error", topicName, pe);
            }
        }
        return topicExists;
    }

    /**
     * Check whether the Pulsar topic exists.
     */
    private boolean subscriptionExists(PulsarAdmin pulsarAdmin, String topic, String subscription,
            boolean isPartitioned) {
        int count = 0;
        while (++count <= RETRY_TIMES) {
            try {
                LOGGER.info("check whether the subscription exists for topic={}, try count={}", topic, count);
                Thread.sleep(DELAY_SECONDS);

                // first lookup to load the topic, and then query whether the subscription exists
                if (isPartitioned) {
                    Map<String, String> topicMap = pulsarAdmin.lookups().lookupPartitionedTopic(topic);
                    if (topicMap.isEmpty()) {
                        LOGGER.error("result of lookups topic={} is empty, continue retry", topic);
                        continue;
                    }
                } else {
                    String lookupTopic = pulsarAdmin.lookups().lookupTopic(topic);
                    if (StringUtils.isBlank(lookupTopic)) {
                        LOGGER.error("result of lookups topic={} is empty, continue retry", topic);
                        continue;
                    }
                }

                List<String> subscriptionList = pulsarAdmin.topics().getSubscriptions(topic);
                return subscriptionList.contains(subscription);
            } catch (PulsarAdminException | InterruptedException e) {
                LOGGER.error("check if the subscription exists for topic={} error, continue retry", topic, e);
                if (count == RETRY_TIMES) {
                    LOGGER.error("after {} times retry, still check subscription exception for topic {}", count, topic);
                    throw new BusinessException("check if the subscription exists error: " + e.getMessage());
                }
            }
        }
        return false;
    }

}
