/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.kafka;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.ScanStats.GroupScanProperty;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.schedule.AffinityCreator;
import org.apache.drill.exec.store.schedule.AssignmentCreator;
import org.apache.drill.exec.store.schedule.CompleteWork;
import org.apache.drill.exec.store.schedule.EndpointByteMap;
import org.apache.drill.exec.store.schedule.EndpointByteMapImpl;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

@JsonTypeName("kafka-scan")
public class KafkaGroupScan extends AbstractGroupScan {
  private static final Logger logger = LoggerFactory.getLogger(KafkaGroupScan.class);

  // Assuming default average topic message size as 1KB, which will be used to
  // compute the stats and work assignments
  private static final long MSG_SIZE = 1024;

  private final KafkaStoragePlugin kafkaStoragePlugin;
  private final KafkaScanSpec kafkaScanSpec;

  private List<SchemaPath> columns;
  private ListMultimap<Integer, PartitionScanWork> assignments;
  private List<EndpointAffinity> affinities;

  private Map<TopicPartition, PartitionScanWork> partitionWorkMap;

  @JsonCreator
  public KafkaGroupScan(@JsonProperty("userName") String userName,
                        @JsonProperty("kafkaStoragePluginConfig") KafkaStoragePluginConfig kafkaStoragePluginConfig,
                        @JsonProperty("columns") List<SchemaPath> columns,
                        @JsonProperty("kafkaScanSpec") KafkaScanSpec scanSpec,
                        @JacksonInject StoragePluginRegistry pluginRegistry) throws ExecutionSetupException {
    this(userName,
        pluginRegistry.resolve(kafkaStoragePluginConfig, KafkaStoragePlugin.class),
        columns,
        scanSpec);
  }

  public KafkaGroupScan(KafkaStoragePlugin kafkaStoragePlugin, KafkaScanSpec kafkaScanSpec, List<SchemaPath> columns) {
    super(StringUtils.EMPTY);
    this.kafkaStoragePlugin = kafkaStoragePlugin;
    this.columns = columns;
    this.kafkaScanSpec = kafkaScanSpec;
    init();
  }

  public KafkaGroupScan(String userName,
                        KafkaStoragePlugin kafkaStoragePlugin,
                        List<SchemaPath> columns,
                        KafkaScanSpec kafkaScanSpec) {
    super(userName);
    this.kafkaStoragePlugin = kafkaStoragePlugin;
    this.columns = columns;
    this.kafkaScanSpec = kafkaScanSpec;
    init();
  }

  public KafkaGroupScan(KafkaGroupScan that) {
    super(that);
    this.kafkaStoragePlugin = that.kafkaStoragePlugin;
    this.columns = that.columns;
    this.kafkaScanSpec = that.kafkaScanSpec;
    this.assignments = that.assignments;
    this.partitionWorkMap = that.partitionWorkMap;
  }

  public static class PartitionScanWork implements CompleteWork {

    private final EndpointByteMapImpl byteMap;
    private final KafkaPartitionScanSpec partitionScanSpec;

    public PartitionScanWork(EndpointByteMap byteMap, KafkaPartitionScanSpec partitionScanSpec) {
      this.byteMap = (EndpointByteMapImpl)byteMap;
      this.partitionScanSpec = partitionScanSpec;
    }

    @Override
    public int compareTo(CompleteWork o) {
      return Long.compare(getTotalBytes(), o.getTotalBytes());
    }

    @Override
    public long getTotalBytes() {
      return (partitionScanSpec.getEndOffset() - partitionScanSpec.getStartOffset()) * MSG_SIZE;
    }

    @Override
    public EndpointByteMap getByteMap() {
      return byteMap;
    }

    public KafkaPartitionScanSpec getPartitionScanSpec() {
      return partitionScanSpec;
    }
  }

  /**
   * Computes work per topic partition, based on start and end offset of each
   * corresponding topicPartition
   */
  private void init() {
    partitionWorkMap = Maps.newHashMap();
    Collection<DrillbitEndpoint> endpoints = kafkaStoragePlugin.getContext().getBits();
    Map<String, DrillbitEndpoint> endpointMap = endpoints.stream()
      .collect(Collectors.toMap(
        DrillbitEndpoint::getAddress,
        Function.identity(),
        (o, n) -> n));

    Map<TopicPartition, Long> startOffsetsMap = Maps.newHashMap();
    Map<TopicPartition, Long> endOffsetsMap = Maps.newHashMap();
    List<PartitionInfo> topicPartitions;
    String topicName = kafkaScanSpec.getTopicName();

    KafkaConsumer<?, ?> kafkaConsumer = null;
    try {
      kafkaConsumer = new KafkaConsumer<>(kafkaStoragePlugin.getConfig().getKafkaConsumerProps(),
        new ByteArrayDeserializer(), new ByteArrayDeserializer());
      if (!kafkaConsumer.listTopics().containsKey(topicName)) {
        throw UserException.dataReadError()
            .message("Table '%s' does not exist", topicName)
            .build(logger);
      }
      kafkaConsumer.subscribe(Collections.singletonList(topicName));
      // based on KafkaConsumer JavaDoc, seekToBeginning/seekToEnd functions
      // evaluates lazily, seeking to the first/last offset in all partitions only
      // when poll(long) or
      // position(TopicPartition) are called
      kafkaConsumer.poll(Duration.ofSeconds(5));
      Set<TopicPartition> assignments = waitForConsumerAssignment(kafkaConsumer);
      topicPartitions = kafkaConsumer.partitionsFor(topicName);

      // fetch start offsets for each topicPartition
      kafkaConsumer.seekToBeginning(assignments);
      for (TopicPartition topicPartition : assignments) {
        startOffsetsMap.put(topicPartition, kafkaConsumer.position(topicPartition));
      }

      // fetch end offsets for each topicPartition
      kafkaConsumer.seekToEnd(assignments);
      for (TopicPartition topicPartition : assignments) {
        endOffsetsMap.put(topicPartition, kafkaConsumer.position(topicPartition));
      }
    } catch (Exception e) {
      throw UserException.dataReadError(e)
        .message("Failed to fetch start/end offsets of the topic %s", topicName)
        .addContext(e.getMessage())
        .build(logger);
    } finally {
      kafkaStoragePlugin.registerToClose(kafkaConsumer);
    }

    // computes work for each end point
    for (PartitionInfo partitionInfo : topicPartitions) {
      TopicPartition topicPartition = new TopicPartition(partitionInfo.topic(), partitionInfo.partition());
      long lastCommittedOffset = startOffsetsMap.get(topicPartition);
      long latestOffset = endOffsetsMap.get(topicPartition);
      logger.debug("Latest offset of {} is {}", topicPartition, latestOffset);
      logger.debug("Last committed offset of {} is {}", topicPartition, lastCommittedOffset);
      KafkaPartitionScanSpec partitionScanSpec = new KafkaPartitionScanSpec(topicPartition.topic(), topicPartition.partition(), lastCommittedOffset, latestOffset);
      PartitionScanWork work = new PartitionScanWork(new EndpointByteMapImpl(), partitionScanSpec);
      Node[] inSyncReplicas = partitionInfo.inSyncReplicas();
      for (Node isr : inSyncReplicas) {
        String host = isr.host();
        DrillbitEndpoint ep = endpointMap.get(host);
        if (ep != null) {
          work.getByteMap().add(ep, work.getTotalBytes());
        }
      }
      partitionWorkMap.put(topicPartition, work);
    }
  }


  /** Workaround for Kafka > 2.0 version due to KIP-505.
   * It can be replaced with Kafka implementation once it will be introduced.
   * @param consumer Kafka consumer whom need to get assignments
   * @return
   * @throws InterruptedException
   */
  private Set<TopicPartition> waitForConsumerAssignment(Consumer consumer) throws InterruptedException {
    Set<TopicPartition> assignments = consumer.assignment();

    long waitingForAssigmentTimeout = kafkaStoragePlugin.getContext().getOptionManager().getLong(ExecConstants.KAFKA_POLL_TIMEOUT);
    long timeout = 0;

    while (assignments.isEmpty() && timeout < waitingForAssigmentTimeout) {
      Thread.sleep(500);
      timeout += 500;
      assignments = consumer.assignment();
    }

    if (timeout >= waitingForAssigmentTimeout) {
      throw UserException.dataReadError()
        .message("Consumer assignment wasn't completed within the timeout %s", waitingForAssigmentTimeout)
        .build(logger);
    }

    return assignments;
  }

  @Override
  public void applyAssignments(List<DrillbitEndpoint> incomingEndpoints) {
    assignments = AssignmentCreator.getMappings(incomingEndpoints, Lists.newArrayList(partitionWorkMap.values()));
  }

  @Override
  public KafkaSubScan getSpecificScan(int minorFragmentId) {
    List<PartitionScanWork> workList = assignments.get(minorFragmentId);

    List<KafkaPartitionScanSpec> scanSpecList = workList.stream()
      .map(PartitionScanWork::getPartitionScanSpec)
      .collect(Collectors.toList());

    return new KafkaSubScan(getUserName(), kafkaStoragePlugin, columns, scanSpecList);
  }

  @Override
  public int getMaxParallelizationWidth() {
    return partitionWorkMap.values().size();
  }

  @Override
  public ScanStats getScanStats() {
    long messageCount = 0;
    for (PartitionScanWork work : partitionWorkMap.values()) {
      messageCount += (work.getPartitionScanSpec().getEndOffset() - work.getPartitionScanSpec().getStartOffset());
    }
    return new ScanStats(GroupScanProperty.EXACT_ROW_COUNT, messageCount, 1, messageCount * MSG_SIZE);
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new KafkaGroupScan(this);
  }

  @Override
  public List<EndpointAffinity> getOperatorAffinity() {
    if (affinities == null) {
      affinities = AffinityCreator.getAffinityMap(Lists.newArrayList(partitionWorkMap.values()));
    }
    return affinities;
  }

  @Override
  @JsonIgnore
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return true;
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    KafkaGroupScan clone = new KafkaGroupScan(this);
    clone.columns = columns;
    return clone;
  }

  public GroupScan cloneWithNewSpec(List<KafkaPartitionScanSpec> partitionScanSpecList) {
    KafkaGroupScan clone = new KafkaGroupScan(this);
    HashSet<TopicPartition> partitionsInSpec = Sets.newHashSet();

    for (KafkaPartitionScanSpec scanSpec : partitionScanSpecList) {
      TopicPartition tp = new TopicPartition(scanSpec.getTopicName(), scanSpec.getPartitionId());
      partitionsInSpec.add(tp);

      PartitionScanWork newScanWork = new PartitionScanWork(partitionWorkMap.get(tp).getByteMap(), scanSpec);
      clone.partitionWorkMap.put(tp, newScanWork);
    }

    //Remove unnecessary partitions from partitionWorkMap
    clone.partitionWorkMap.keySet().removeIf(tp -> !partitionsInSpec.contains(tp));
    return clone;
  }

  @JsonProperty
  public KafkaStoragePluginConfig getKafkaStoragePluginConfig() {
    return kafkaStoragePlugin.getConfig();
  }

  @Override
  @JsonProperty
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty
  public KafkaScanSpec getKafkaScanSpec() {
    return kafkaScanSpec;
  }

  @JsonIgnore
  public KafkaStoragePlugin getStoragePlugin() {
    return kafkaStoragePlugin;
  }

  @Override
  public String toString() {
    return String.format("KafkaGroupScan [KafkaScanSpec=%s, columns=%s]", kafkaScanSpec, columns);
  }

  @JsonIgnore
  public List<KafkaPartitionScanSpec> getPartitionScanSpecList() {
    return partitionWorkMap.values().stream()
      .map(work -> work.partitionScanSpec.clone())
      .collect(Collectors.toList());
  }
}
