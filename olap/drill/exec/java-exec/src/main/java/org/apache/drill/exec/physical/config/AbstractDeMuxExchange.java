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
package org.apache.drill.exec.physical.config;

import java.util.List;
import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.base.AbstractExchange;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.Sender;
import org.apache.drill.exec.planner.fragment.ParallelizationInfo;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DeMuxExchange is opposite of MuxExchange. It is used when the sender has overhead that is proportional to the
 * number of receivers. DeMuxExchange is run one instance per Drillbit endpoint which collects and distributes data
 * belonging to local receiving fragments running on the same Drillbit.
 *
 * Example:
 * On a 3 node cluster, if the sender has 10 receivers on each node each sender requires 30 buffers. By inserting
 * DeMuxExchange, we create one receiver per node which means total of 3 receivers for each sender. If the number of
 * senders is 10, we use 10*3 buffers instead of 10*30. DeMuxExchange has a overhead of buffer space that is equal to
 * number of local receivers. In this case each DeMuxExchange needs 10 buffers, so total of 3*10 buffers.
 */
public abstract class AbstractDeMuxExchange extends AbstractExchange {
  protected final LogicalExpression expr;

  // Ephemeral info used when creating execution fragments.
  protected Map<Integer, MinorFragmentEndpoint> receiverToSenderMapping;
  protected ArrayListMultimap<Integer, MinorFragmentEndpoint> senderToReceiversMapping;
  private boolean isSenderReceiverMappingCreated;

  public AbstractDeMuxExchange(@JsonProperty("child") PhysicalOperator child, @JsonProperty("expr") LogicalExpression expr) {
    super(child);
    this.expr = expr;
  }

  @JsonProperty("expr")
  public LogicalExpression getExpression(){
    return expr;
  }

  @Override
  public ParallelizationInfo getSenderParallelizationInfo(List<DrillbitEndpoint> receiverFragmentEndpoints) {
    Preconditions.checkArgument(receiverFragmentEndpoints != null && receiverFragmentEndpoints.size() > 0,
        "Receiver fragment endpoint list should not be empty");

    // We want to run one demux sender per Drillbit endpoint.
    // Identify the number of unique Drillbit endpoints in receiver fragment endpoints.
    List<DrillbitEndpoint> drillbitEndpoints = ImmutableSet.copyOf(receiverFragmentEndpoints).asList();

    List<EndpointAffinity> affinities = Lists.newArrayList();
    for(DrillbitEndpoint ep : drillbitEndpoints) {
      affinities.add(new EndpointAffinity(ep, Double.POSITIVE_INFINITY));
    }

    return ParallelizationInfo.create(affinities.size(), affinities.size(), affinities);
  }

  @Override
  public ParallelizationInfo getReceiverParallelizationInfo(List<DrillbitEndpoint> senderFragmentEndpoints) {
    return ParallelizationInfo.UNLIMITED_WIDTH_NO_ENDPOINT_AFFINITY;
  }

  @Override
  public Sender getSender(int minorFragmentId, PhysicalOperator child) {
    createSenderReceiverMapping();

    List<MinorFragmentEndpoint> receivers = senderToReceiversMapping.get(minorFragmentId);
    if (receivers == null || receivers.size() <= 0) {
      throw new IllegalStateException(String.format("Failed to find receivers for sender [%d]", minorFragmentId));
    }

    return new HashPartitionSender(receiverMajorFragmentId, child, expr, receivers);
  }

  /**
   * In DeMuxExchange, sender fragment parallelization and endpoint assignment depends on receiver fragment endpoint
   * assignments.
   */
  @Override
  public ParallelizationDependency getParallelizationDependency() {
    return ParallelizationDependency.SENDER_DEPENDS_ON_RECEIVER;
  }

  protected void createSenderReceiverMapping() {
    if (isSenderReceiverMappingCreated) {
      return;
    }

    senderToReceiversMapping = ArrayListMultimap.create();
    receiverToSenderMapping = Maps.newHashMap();

    // Find the list of receiver fragment ids assigned to each Drillbit endpoint
    ArrayListMultimap<DrillbitEndpoint, Integer> endpointReceiverList = ArrayListMultimap.create();

    int receiverFragmentId = 0;
    for(DrillbitEndpoint receiverLocation : receiverLocations) {
      endpointReceiverList.put(receiverLocation, receiverFragmentId);
      receiverFragmentId++;
    }

    int senderFragmentId = 0;
    for(DrillbitEndpoint senderLocation : senderLocations) {
      final List<Integer> receiverMinorFragmentIds = endpointReceiverList.get(senderLocation);

      for(Integer receiverId : receiverMinorFragmentIds) {
        receiverToSenderMapping.put(receiverId, new MinorFragmentEndpoint(senderFragmentId, senderLocation));

        senderToReceiversMapping.put(senderFragmentId,
            new MinorFragmentEndpoint(receiverId, receiverLocations.get(receiverId)));
      }
      senderFragmentId++;
    }

    isSenderReceiverMappingCreated = true;
  }
}
