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
package org.apache.drill.exec.work.filter;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.ops.AccountingDataTunnel;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.proto.BitData;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.proto.ExecProtos;
import org.apache.drill.exec.proto.UserBitShared;

import java.util.ArrayList;
import java.util.List;

/**
 * A reporter to send out the bloom filters to their receivers.
 */
public class RuntimeFilterReporter {

  private ExecutorFragmentContext context;

  public RuntimeFilterReporter(ExecutorFragmentContext context) {
    this.context = context;
  }

  public void sendOut(List<BloomFilter> bloomFilters, List<String> probeFields, RuntimeFilterDef runtimeFilterDef, int hashJoinOpId) {
    boolean sendToForeman = runtimeFilterDef.isSendToForeman();
    long rfIdentifier = runtimeFilterDef.getRuntimeFilterIdentifier();
    ExecProtos.FragmentHandle fragmentHandle = context.getHandle();
    DrillBuf[] data = new DrillBuf[bloomFilters.size()];
    List<Integer> bloomFilterSizeInBytes = new ArrayList<>();
    int i = 0;
    for (BloomFilter bloomFilter : bloomFilters) {
      DrillBuf bfContent = bloomFilter.getContent();
      data[i] = bfContent;
      bloomFilterSizeInBytes.add(bfContent.capacity());
      i++;
    }

    UserBitShared.QueryId queryId = fragmentHandle.getQueryId();
    int majorFragmentId = fragmentHandle.getMajorFragmentId();
    int minorFragmentId = fragmentHandle.getMinorFragmentId();
    BitData.RuntimeFilterBDef.Builder builder = BitData.RuntimeFilterBDef.newBuilder();
    for (String probeFiled : probeFields) {
      builder.addProbeFields(probeFiled);
    }
    BitData.RuntimeFilterBDef runtimeFilterB = builder
      .setQueryId(queryId)
      .setMajorFragmentId(majorFragmentId)
      .setMinorFragmentId(minorFragmentId)
      .setToForeman(sendToForeman)
      .setHjOpId(hashJoinOpId)
      .setRfIdentifier(rfIdentifier)
      .addAllBloomFilterSizeInBytes(bloomFilterSizeInBytes)
      .build();
    RuntimeFilterWritable runtimeFilterWritable = new RuntimeFilterWritable(runtimeFilterB, data);

    if (sendToForeman) {
      CoordinationProtos.DrillbitEndpoint foremanEndpoint = context.getForemanEndpoint();
      AccountingDataTunnel dataTunnel = context.getDataTunnel(foremanEndpoint);
      dataTunnel.sendRuntimeFilter(runtimeFilterWritable);
    } else {
      context.addRuntimeFilter(runtimeFilterWritable);
    }
  }
}
