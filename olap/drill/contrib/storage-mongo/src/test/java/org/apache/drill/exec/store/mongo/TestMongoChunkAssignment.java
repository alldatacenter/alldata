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
package org.apache.drill.exec.store.mongo;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.drill.categories.MongoStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.mongo.common.ChunkInfo;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.test.BaseTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.mongodb.ServerAddress;

@Category({SlowTest.class, MongoStorageTest.class})
public class TestMongoChunkAssignment extends BaseTest {
  static final String HOST_A = "A";
  static final String HOST_B = "B";
  static final String HOST_C = "C";
  static final String HOST_D = "D";
  static final String HOST_E = "E";
  static final String HOST_F = "F";
  static final String HOST_G = "G";
  static final String HOST_H = "H";
  static final String HOST_I = "I";
  static final String HOST_J = "J";
  static final String HOST_K = "K";
  static final String HOST_L = "L";
  static final String HOST_M = "M";

  static final String HOST_X = "X";

  static final String dbName = "testDB";
  static final String collectionName = "testCollection";

  private Map<String, Set<ServerAddress>> chunksMapping;
  private Map<String, List<ChunkInfo>> chunksInverseMapping;
  private MongoGroupScan mongoGroupScan;

  @Before
  public void setUp() throws UnknownHostException {
    chunksMapping = Maps.newHashMap();
    chunksInverseMapping = Maps.newLinkedHashMap();

    // entry1
    Set<ServerAddress> hosts_A = Sets.newHashSet();
    hosts_A.add(new ServerAddress(HOST_A));
    chunksMapping.put(dbName + "." + collectionName + "-01", hosts_A);
    chunksMapping.put(dbName + "." + collectionName + "-05", hosts_A);

    ChunkInfo chunk1Info = new ChunkInfo(Arrays.asList(HOST_A), dbName + "."
        + collectionName + "-01");
    chunk1Info.setMinFilters(Collections.<String, Object> emptyMap());
    Map<String, Object> chunk1MaxFilters = Maps.newHashMap();
    chunk1MaxFilters.put("name", Integer.valueOf(5));
    chunk1Info.setMaxFilters(chunk1MaxFilters);

    ChunkInfo chunk5Info = new ChunkInfo(Arrays.asList(HOST_A), dbName + "."
        + collectionName + "-05");
    Map<String, Object> chunk5MinFilters = Maps.newHashMap();
    chunk5MinFilters.put("name", Integer.valueOf(25));
    chunk5Info.setMinFilters(chunk5MinFilters);
    Map<String, Object> chunk5MaxFilters = Maps.newHashMap();
    chunk5MaxFilters.put("name", Integer.valueOf(30));
    chunk5Info.setMaxFilters(chunk5MaxFilters);
    List<ChunkInfo> chunkList = Arrays.asList(chunk1Info, chunk5Info);
    chunksInverseMapping.put(HOST_A, chunkList);

    // entry2
    Set<ServerAddress> hosts_B = Sets.newHashSet();
    hosts_A.add(new ServerAddress(HOST_B));
    chunksMapping.put(dbName + "." + collectionName + "-02", hosts_B);

    ChunkInfo chunk2Info = new ChunkInfo(Arrays.asList(HOST_B), dbName + "."
        + collectionName + "-02");
    Map<String, Object> chunk2MinFilters = Maps.newHashMap();
    chunk2MinFilters.put("name", Integer.valueOf(5));
    chunk2Info.setMinFilters(chunk2MinFilters);
    Map<String, Object> chunk2MaxFilters = Maps.newHashMap();
    chunk2MaxFilters.put("name", Integer.valueOf(15));
    chunk2Info.setMaxFilters(chunk2MaxFilters);
    chunkList = Arrays.asList(chunk2Info);
    chunksInverseMapping.put(HOST_B, chunkList);

    // enty3
    Set<ServerAddress> hosts_C = Sets.newHashSet();
    hosts_A.add(new ServerAddress(HOST_C));
    chunksMapping.put(dbName + "." + collectionName + "-03", hosts_C);
    chunksMapping.put(dbName + "." + collectionName + "-06", hosts_C);

    ChunkInfo chunk3Info = new ChunkInfo(Arrays.asList(HOST_C), dbName + "."
        + collectionName + "-03");
    Map<String, Object> chunk3MinFilters = Maps.newHashMap();
    chunk5MinFilters.put("name", Integer.valueOf(15));
    chunk3Info.setMinFilters(chunk3MinFilters);
    Map<String, Object> chunk3MaxFilters = Maps.newHashMap();
    chunk3MaxFilters.put("name", Integer.valueOf(20));
    chunk3Info.setMaxFilters(chunk3MaxFilters);

    ChunkInfo chunk6Info = new ChunkInfo(Arrays.asList(HOST_C), dbName + "."
        + collectionName + "-06");
    Map<String, Object> chunk6MinFilters = Maps.newHashMap();
    chunk5MinFilters.put("name", Integer.valueOf(25));
    chunk6Info.setMinFilters(chunk6MinFilters);
    Map<String, Object> chunk6MaxFilters = Maps.newHashMap();
    chunk5MaxFilters.put("name", Integer.valueOf(30));
    chunk6Info.setMaxFilters(chunk6MaxFilters);
    chunkList = Arrays.asList(chunk3Info, chunk6Info);
    chunksInverseMapping.put(HOST_C, chunkList);

    // entry4
    Set<ServerAddress> hosts_D = Sets.newHashSet();
    hosts_A.add(new ServerAddress(HOST_D));
    chunksMapping.put(dbName + "." + collectionName + "-04", hosts_D);

    ChunkInfo chunk4Info = new ChunkInfo(Arrays.asList(HOST_D), dbName + "."
        + collectionName + "-04");
    Map<String, Object> chunk4MinFilters = Maps.newHashMap();
    chunk4MinFilters.put("name", Integer.valueOf(20));
    chunk4Info.setMinFilters(chunk4MinFilters);
    Map<String, Object> chunk4MaxFilters = Maps.newHashMap();
    chunk4MaxFilters.put("name", Integer.valueOf(25));
    chunk4Info.setMaxFilters(chunk4MaxFilters);
    chunkList = Arrays.asList(chunk4Info);
    chunksInverseMapping.put(HOST_D, chunkList);

    mongoGroupScan = new MongoGroupScan();
    mongoGroupScan.setChunksMapping(chunksMapping);
    mongoGroupScan.setInverseChunksMapping(chunksInverseMapping);
    MongoScanSpec scanSpec = new MongoScanSpec(dbName, collectionName);
    mongoGroupScan.setScanSpec(scanSpec);
  }

  @Test
  public void testMongoGroupScanAssignmentMix() throws UnknownHostException,
      ExecutionSetupException {
    final List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    final DrillbitEndpoint DB_A = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_A).setControlPort(1234).build();
    endpoints.add(DB_A);
    endpoints.add(DB_A);
    final DrillbitEndpoint DB_B = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_B).setControlPort(1234).build();
    endpoints.add(DB_B);
    final DrillbitEndpoint DB_D = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_D).setControlPort(1234).build();
    endpoints.add(DB_D);
    final DrillbitEndpoint DB_X = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_X).setControlPort(1234).build();
    endpoints.add(DB_X);

    mongoGroupScan.applyAssignments(endpoints);

    // assignments for chunks on host A, assign on drill bit A
    assertEquals(1, mongoGroupScan.getSpecificScan(0).getChunkScanSpecList()
        .size());
    // assignments for chunks on host A, assign on drill bit A
    assertEquals(1, mongoGroupScan.getSpecificScan(1).getChunkScanSpecList()
        .size());
    // assignments for chunks on host B, assign on drill bit B
    assertEquals(1, mongoGroupScan.getSpecificScan(2).getChunkScanSpecList()
        .size());
    // assignments for chunks on host D, assign on drill bit D
    assertEquals(1, mongoGroupScan.getSpecificScan(3).getChunkScanSpecList()
        .size());
    // assignments for chunks on host C, assign on drill bit X
    assertEquals(2, mongoGroupScan.getSpecificScan(4).getChunkScanSpecList()
        .size());
  }

  @Test
  public void testMongoGroupScanAssignmentAllAffinity()
      throws UnknownHostException, ExecutionSetupException {
    final List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    final DrillbitEndpoint DB_A = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_A).setControlPort(1234).build();
    endpoints.add(DB_A);
    final DrillbitEndpoint DB_B = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_B).setControlPort(1234).build();
    endpoints.add(DB_B);
    final DrillbitEndpoint DB_C = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_C).setControlPort(1234).build();
    endpoints.add(DB_C);
    final DrillbitEndpoint DB_D = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_D).setControlPort(1234).build();
    endpoints.add(DB_D);

    mongoGroupScan.applyAssignments(endpoints);

    // assignments for chunks on host A, assign on drill bit A
    assertEquals(2, mongoGroupScan.getSpecificScan(0).getChunkScanSpecList()
        .size());
    // assignments for chunks on host B, assign on drill bit B
    assertEquals(1, mongoGroupScan.getSpecificScan(1).getChunkScanSpecList()
        .size());
    // assignments for chunks on host C, assign on drill bit C
    assertEquals(2, mongoGroupScan.getSpecificScan(2).getChunkScanSpecList()
        .size());
    // assignments for chunks on host D, assign on drill bit D
    assertEquals(1, mongoGroupScan.getSpecificScan(3).getChunkScanSpecList()
        .size());
  }

  @Test
  public void testMongoGroupScanAssignmentNoAffinity()
      throws UnknownHostException, ExecutionSetupException {
    final List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    final DrillbitEndpoint DB_M = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_M).setControlPort(1234).build();
    endpoints.add(DB_M);
    endpoints.add(DB_M);
    final DrillbitEndpoint DB_L = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_L).setControlPort(1234).build();
    endpoints.add(DB_L);
    final DrillbitEndpoint DB_X = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_X).setControlPort(1234).build();
    endpoints.add(DB_X);

    mongoGroupScan.applyAssignments(endpoints);

    // assignments for chunks on host A, assign on drill bit M
    assertEquals(1, mongoGroupScan.getSpecificScan(0).getChunkScanSpecList()
        .size());
    // assignments for chunks on host B, assign on drill bit M
    assertEquals(2, mongoGroupScan.getSpecificScan(1).getChunkScanSpecList()
        .size());
    // assignments for chunks on host C, assign on drill bit L
    assertEquals(2, mongoGroupScan.getSpecificScan(2).getChunkScanSpecList()
        .size());
    // assignments for chunks on host D, assign on drill bit X
    assertEquals(1, mongoGroupScan.getSpecificScan(3).getChunkScanSpecList()
        .size());
  }

  @Test
  public void testMongoGroupScanAssignmentWhenOnlyOneDrillBit()
      throws UnknownHostException, ExecutionSetupException {
    final List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    final DrillbitEndpoint DB_A = DrillbitEndpoint.newBuilder()
        .setAddress(HOST_A).setControlPort(1234).build();
    endpoints.add(DB_A);

    mongoGroupScan.applyAssignments(endpoints);

    // All the assignments should be given to drill bit A.
    assertEquals(6, mongoGroupScan.getSpecificScan(0).getChunkScanSpecList()
        .size());
  }
}
