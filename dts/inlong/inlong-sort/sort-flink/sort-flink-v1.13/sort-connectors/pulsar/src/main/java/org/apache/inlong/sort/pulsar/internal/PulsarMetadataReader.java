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

package org.apache.inlong.sort.pulsar.internal;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.streaming.connectors.pulsar.internal.PulsarClientUtils;
import org.apache.flink.streaming.connectors.pulsar.internal.PulsarOptions;
import org.apache.flink.streaming.connectors.pulsar.internal.SerializableRange;
import org.apache.flink.streaming.connectors.pulsar.internal.SourceSinkUtils;
import org.apache.flink.streaming.connectors.pulsar.internal.TopicRange;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.StringUtils;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Range;
import org.apache.pulsar.client.impl.MessageIdImpl;
import org.apache.pulsar.client.impl.PulsarClientImpl;
import org.apache.pulsar.client.impl.conf.ClientConfigurationData;
import org.apache.pulsar.common.naming.TopicName;
import org.apache.pulsar.common.policies.data.PersistentTopicInternalStats;
import org.apache.pulsar.common.policies.data.SubscriptionStats;
import org.apache.pulsar.common.policies.data.TopicStats;
import org.apache.pulsar.shade.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.apache.flink.streaming.connectors.pulsar.internal.PulsarOptions.ENABLE_KEY_HASH_RANGE_KEY;

/**
 * Copy from io.streamnative.connectors:pulsar-flink-connector_2.11:1.13.6.1-rc9,
 * From {@link org.apache.flink.streaming.connectors.pulsar.internal.PulsarMetadataReader}
 * A Helper class that talks to Pulsar Admin API.
 * - getEarliest / Latest / Specific MessageIds
 * - guarantee message existence using subscription by setup, move and remove
 */
public class PulsarMetadataReader implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PulsarMetadataReader.class);

    public static final ConfigOption<String> AUTHENTICATION_TOKEN =
            ConfigOptions.key("authentication-token")
                    .noDefaultValue()
                    .withDescription("Authentication token for connection.");

    private final String serverUrl;

    private final ClientConfigurationData clientConf;

    private final String subscriptionName;

    private final Map<String, String> caseInsensitiveParams;

    private final int indexOfThisSubtask;

    private final int numParallelSubtasks;

    private final PulsarClient client;

    private volatile boolean closed = false;

    private Set<TopicRange> seenTopics = new HashSet<>();

    private final boolean useExternalSubscription;

    private final SerializableRange range;

    private PulsarAdmin admin;

    public PulsarMetadataReader(
            String adminUrl,
            String serverUrl,
            ClientConfigurationData clientConf,
            String subscriptionName,
            Map<String, String> caseInsensitiveParams,
            int indexOfThisSubtask,
            int numParallelSubtasks,
            boolean useExternalSubscription) throws PulsarClientException {
        this.serverUrl = serverUrl;
        this.clientConf = clientConf;
        this.subscriptionName = subscriptionName;
        this.caseInsensitiveParams = caseInsensitiveParams;
        this.indexOfThisSubtask = indexOfThisSubtask;
        this.numParallelSubtasks = numParallelSubtasks;
        this.useExternalSubscription = useExternalSubscription;
        this.client = buildPulsarClient(serverUrl, clientConf, caseInsensitiveParams.get(AUTHENTICATION_TOKEN.key()));
        this.range = buildRange(caseInsensitiveParams);
        if (adminUrl != null) {
            this.admin = PulsarClientUtils.newAdminFromConf(adminUrl, clientConf);
        }
    }

    private PulsarClient buildPulsarClient(
            String serviceUrl,
            ClientConfigurationData clientConf,
            String authentication) throws PulsarClientException {
        if (StringUtils.isNullOrWhitespaceOnly(authentication)) {
            return new PulsarClientImpl(clientConf);
        } else {
            return PulsarClient.builder()
                    .serviceUrl(serviceUrl)
                    .authentication(AuthenticationFactory.token(authentication)).build();
        }
    }

    private SerializableRange buildRange(Map<String, String> caseInsensitiveParams) {
        if (numParallelSubtasks <= 0 || indexOfThisSubtask < 0) {
            return SerializableRange.ofFullRange();
        }
        if (caseInsensitiveParams == null || caseInsensitiveParams.isEmpty()
                || !caseInsensitiveParams.containsKey(ENABLE_KEY_HASH_RANGE_KEY)) {
            return SerializableRange.ofFullRange();
        }
        final String enableKeyHashRange = caseInsensitiveParams.get(ENABLE_KEY_HASH_RANGE_KEY);
        if (!Boolean.parseBoolean(enableKeyHashRange)) {
            return SerializableRange.ofFullRange();
        }
        final Range range = SourceSinkUtils.distributeRange(numParallelSubtasks, indexOfThisSubtask);
        return SerializableRange.of(range);
    }

    private String subscriptionNameFrom(TopicRange topicRange) {
        return topicRange.isFullRange() ? subscriptionName : subscriptionName + topicRange.getPulsarRange();
    }

    @Override
    public void close() throws PulsarClientException {
        closed = true;
        client.close();
    }

    public Set<TopicRange> discoverTopicChanges() throws PulsarClientException, ClosedException {
        if (!closed) {
            Set<TopicRange> currentTopics = getTopicPartitionRanges();
            Set<TopicRange> addedTopics = Sets.difference(currentTopics, seenTopics);
            seenTopics = currentTopics;
            return addedTopics;
        } else {
            throw new ClosedException();
        }
    }

    public SerializableRange getRange() {
        return range;
    }

    /**
     * Get all TopicRange that should be consumed by the subTask.
     *
     * @return set of topic ranges this subTask should consume
     * @throws PulsarAdminException
     */
    public Set<TopicRange> getTopicPartitionRanges() throws PulsarClientException {
        Set<String> topics = getTopicPartitions();
        return topics.stream()
                .filter(
                        t -> SourceSinkUtils.belongsTo(
                                t, range, numParallelSubtasks, indexOfThisSubtask))
                .map(t -> new TopicRange(t, range.getPulsarRange()))
                .collect(Collectors.toSet());
    }

    /**
     * Get topic partitions all. If the topic does not exist, it is created automatically
     *
     * @return allTopicPartitions
     * @throws PulsarAdminException pulsarAdminException
     */
    public Set<String> getTopicPartitions() throws PulsarClientException {
        List<String> topics = getTopics();
        HashSet<String> allTopics = new HashSet<>();
        for (String topic : topics) {
            int partNum = 1;
            try {
                List<String> partitions = client.getPartitionsForTopic(topic).get();
                if (partitions.size() == 1 && partitions.get(0).equals(topic)) {
                    partNum = 0;
                } else {
                    partNum = partitions.size();
                }
            } catch (ExecutionException | InterruptedException e) {
                // TODO:here topic not found must not be wait forever, otherwise is a bug!
                log.info("topic<{}> is not exit or execute error.", topic, e);
                throw new PulsarClientException(e);
            }
            // pulsar still has the situation of getting 0 partitions, non-partitions topic.
            if (partNum == 0) {
                allTopics.add(topic);
            } else {
                for (int i = 0; i < partNum; i++) {
                    allTopics.add(topic + PulsarOptions.PARTITION_SUFFIX + i);
                }
            }
        }
        return allTopics;
    }

    private List<String> getTopics() {
        for (Map.Entry<String, String> e : caseInsensitiveParams.entrySet()) {
            if (PulsarOptions.TOPIC_OPTION_KEYS.contains(e.getKey())) {
                switch (e.getKey()) {
                    case PulsarOptions.TOPIC_SINGLE_OPTION_KEY:
                        return Collections.singletonList(TopicName.get(e.getValue()).toString());
                    case PulsarOptions.TOPIC_MULTI_OPTION_KEY:
                        return Arrays.asList(e.getValue().split(",")).stream()
                                .filter(s -> !s.isEmpty())
                                .map(t -> TopicName.get(t).toString())
                                .collect(Collectors.toList());
                    default:
                        throw new IllegalArgumentException(
                                "Unknown pulsar topic option: " + e.getKey());
                }
            }
        }
        return Collections.emptyList();
    }

    public void commitOffsetToCursor(Map<TopicRange, MessageId> offset) {
        Preconditions.checkNotNull(admin, "admin url should not be null");
        for (Map.Entry<TopicRange, MessageId> entry : offset.entrySet()) {
            TopicRange tp = entry.getKey();
            try {
                log.info("Committing offset {} to topic {}", entry.getValue(), tp);
                admin.topics().resetCursor(tp.getTopic(), subscriptionNameFrom(tp), entry.getValue(), true);
                log.info("Successfully committed offset {} to topic {}", entry.getValue(), tp);
            } catch (Throwable e) {
                if (e instanceof PulsarAdminException &&
                        (((PulsarAdminException) e).getStatusCode() == 404 ||
                                ((PulsarAdminException) e).getStatusCode() == 412)) {
                    log.info("Cannot commit cursor since the topic {} has been deleted during execution", tp);
                } else {
                    throw new RuntimeException(
                            String.format("Failed to commit cursor for %s", tp), e);
                }
            }
        }
    }

    public MessageId getPositionFromSubscription(TopicRange topic, MessageId defaultPosition) {
        try {
            String subscriptionName = subscriptionNameFrom(topic);
            TopicStats topicStats = admin.topics().getStats(topic.getTopic());
            if (topicStats.getSubscriptions().containsKey(subscriptionName)) {
                SubscriptionStats subStats = topicStats.getSubscriptions().get(subscriptionName);
                if (subStats.getConsumers().size() != 0) {
                    throw new IllegalStateException(
                            "Subscription been actively used by other consumers, "
                                    + "in this situation, the exactly-once semantics cannot be guaranteed.");
                } else {
                    String encodedSubName =
                            URLEncoder.encode(subscriptionName, StandardCharsets.UTF_8.toString());
                    PersistentTopicInternalStats.CursorStats c =
                            admin.topics()
                                    .getInternalStats(topic.getTopic()).cursors
                                            .get(encodedSubName);
                    String[] ids = c.markDeletePosition.split(":", 2);
                    long ledgerId = Long.parseLong(ids[0]);
                    long entryIdInMarkDelete = Long.parseLong(ids[1]);
                    // we are getting the next mid from sub position, if the entryId is -1,
                    // it denotes we haven't read data from the ledger before,
                    // therefore no need to skip the current entry for the next position
                    long entryId = entryIdInMarkDelete == -1 ? -1 : entryIdInMarkDelete + 1;
                    int partitionIdx = TopicName.getPartitionIndex(topic.getTopic());
                    return new MessageIdImpl(ledgerId, entryId, partitionIdx);
                }
            } else {
                // create sub on topic
                admin.topics()
                        .createSubscription(topic.getTopic(), subscriptionName, defaultPosition);
                return defaultPosition;
            }
        } catch (PulsarAdminException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to get stats for topic " + topic, e);
        }
    }

    /**
     * Designate the close of the metadata reader.
     */
    public static class ClosedException extends Exception {
    }
}