/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.service.deploy.master;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.NET_TOPOLOGY_NODE_SWITCH_MAPPING_IMPL_KEY;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.NET_TOPOLOGY_TABLE_MAPPING_FILE_KEY;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import scala.Tuple2;

import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.net.TableMapping;
import org.apache.hadoop.shaded.com.google.common.base.Charsets;
import org.apache.hadoop.shaded.com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.meta.WorkerInfo;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.service.deploy.master.network.CelebornRackResolver;

public class SlotsAllocatorRackAwareSuiteJ {

  @Test
  public void offerSlotsRoundRobinWithRackAware() throws IOException {
    CelebornConf conf = new CelebornConf();
    conf.set(CelebornConf.CLIENT_RESERVE_SLOTS_RACKAWARE_ENABLED().key(), "true");

    File mapFile = File.createTempFile("testResolve1", ".txt");
    Files.asCharSink(mapFile, Charsets.UTF_8)
        .write(
            "host1 /default/rack1\nhost2 /default/rack1\nhost3 /default/rack1\n"
                + "host4 /default/rack2\nhost5 /default/rack2\nhost6 /default/rack2\n");
    mapFile.deleteOnExit();

    conf.set(
        "celeborn.hadoop." + NET_TOPOLOGY_NODE_SWITCH_MAPPING_IMPL_KEY,
        TableMapping.class.getName());
    conf.set("celeborn.hadoop." + NET_TOPOLOGY_TABLE_MAPPING_FILE_KEY, mapFile.getCanonicalPath());
    CelebornRackResolver resolver = new CelebornRackResolver(conf);

    List<Integer> partitionIds = new ArrayList<Integer>();
    partitionIds.add(0);
    partitionIds.add(1);
    partitionIds.add(2);

    List<WorkerInfo> workers = prepareWorkers(resolver);

    Map<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>> slots =
        SlotsAllocator.offerSlotsRoundRobin(workers, partitionIds, true, true);

    Consumer<PartitionLocation> assertCustomer =
        new Consumer<PartitionLocation>() {
          public void accept(PartitionLocation location) {
            Assert.assertNotEquals(
                resolver.resolve(location.getHost()).getNetworkLocation(),
                resolver.resolve(location.getPeer().getHost()).getNetworkLocation());
          }
        };
    slots.values().stream().map(Tuple2::_1).flatMap(Collection::stream).forEach(assertCustomer);
  }

  @Test
  public void offerSlotsRoundRobinWithRackAwareWithoutMappingFile() throws IOException {
    CelebornConf conf = new CelebornConf();
    conf.set(CelebornConf.CLIENT_RESERVE_SLOTS_RACKAWARE_ENABLED().key(), "true");

    File mapFile = File.createTempFile("testResolve1", ".txt");
    mapFile.delete();

    conf.set(
        "celeborn.hadoop." + NET_TOPOLOGY_NODE_SWITCH_MAPPING_IMPL_KEY,
        TableMapping.class.getName());
    conf.set("celeborn.hadoop." + NET_TOPOLOGY_TABLE_MAPPING_FILE_KEY, mapFile.getCanonicalPath());
    CelebornRackResolver resolver = new CelebornRackResolver(conf);

    List<Integer> partitionIds = new ArrayList<Integer>();
    partitionIds.add(0);
    partitionIds.add(1);
    partitionIds.add(2);

    List<WorkerInfo> workers = prepareWorkers(resolver);

    Map<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>> slots =
        SlotsAllocator.offerSlotsRoundRobin(workers, partitionIds, true, true);

    Consumer<PartitionLocation> assertConsumer =
        new Consumer<PartitionLocation>() {
          public void accept(PartitionLocation location) {
            Assert.assertEquals(
                NetworkTopology.DEFAULT_RACK,
                resolver.resolve(location.getHost()).getNetworkLocation());
            Assert.assertEquals(
                NetworkTopology.DEFAULT_RACK,
                resolver.resolve(location.getPeer().getHost()).getNetworkLocation());
          }
        };
    slots.values().stream().map(Tuple2::_1).flatMap(Collection::stream).forEach(assertConsumer);
  }

  private List<WorkerInfo> prepareWorkers(CelebornRackResolver resolver) {
    ArrayList<WorkerInfo> workers = new ArrayList<>(3);
    workers.add(new WorkerInfo("host1", 9, 10, 110, 113, new HashMap<>(), null));
    workers.add(new WorkerInfo("host2", 9, 11, 111, 114, new HashMap<>(), null));
    workers.add(new WorkerInfo("host3", 9, 12, 112, 115, new HashMap<>(), null));
    workers.add(new WorkerInfo("host4", 9, 10, 110, 113, new HashMap<>(), null));
    workers.add(new WorkerInfo("host5", 9, 11, 111, 114, new HashMap<>(), null));
    workers.add(new WorkerInfo("host6", 9, 12, 112, 115, new HashMap<>(), null));

    workers.forEach(
        new Consumer<WorkerInfo>() {
          public void accept(WorkerInfo workerInfo) {
            workerInfo.networkLocation_$eq(
                resolver.resolve(workerInfo.host()).getNetworkLocation());
          }
        });
    return workers;
  }
}
