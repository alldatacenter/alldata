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
package org.apache.drill.hbase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;

import org.apache.drill.categories.HbaseStorageTest;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.hbase.HBaseGroupScan;
import org.apache.drill.exec.store.hbase.HBaseScanSpec;
import org.apache.drill.categories.SlowTest;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class, HbaseStorageTest.class})
public class TestHBaseRegionScanAssignments extends BaseHBaseTest {
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

  static final String PORT_AND_STARTTIME = ",60020,1400265190186";

  static final ServerName SERVER_A = ServerName.valueOf(HOST_A + PORT_AND_STARTTIME);
  static final ServerName SERVER_B = ServerName.valueOf(HOST_B + PORT_AND_STARTTIME);
  static final ServerName SERVER_C = ServerName.valueOf(HOST_C + PORT_AND_STARTTIME);
  static final ServerName SERVER_D = ServerName.valueOf(HOST_D + PORT_AND_STARTTIME);
  static final ServerName SERVER_E = ServerName.valueOf(HOST_E + PORT_AND_STARTTIME);
  static final ServerName SERVER_F = ServerName.valueOf(HOST_F + PORT_AND_STARTTIME);
  static final ServerName SERVER_G = ServerName.valueOf(HOST_G + PORT_AND_STARTTIME);
  static final ServerName SERVER_H = ServerName.valueOf(HOST_H + PORT_AND_STARTTIME);
  static final ServerName SERVER_I = ServerName.valueOf(HOST_I + PORT_AND_STARTTIME);

  static final ServerName SERVER_X = ServerName.valueOf(HOST_X + PORT_AND_STARTTIME);

  static final byte[][] splits = {{},
    "10".getBytes(), "15".getBytes(), "20".getBytes(), "25".getBytes(), "30".getBytes(), "35".getBytes(),
    "40".getBytes(), "45".getBytes(), "50".getBytes(), "55".getBytes(), "60".getBytes(), "65".getBytes(),
    "70".getBytes(), "75".getBytes(), "80".getBytes(), "85".getBytes(), "90".getBytes(), "95".getBytes()};

  static final String TABLE_NAME_STR = "TestTable";
  static final TableName TABLE_NAME = TableName.valueOf(TABLE_NAME_STR);

  /**
   * Has the same name as the {@link BeforeClass} method of the parent so that
   * we do not start MiniHBase cluster as it is not required for these tests.
   * @throws Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // do nothing
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    // do nothing
  }

  @Test
  public void testHBaseGroupScanAssignmentMix() throws Exception {
    NavigableMap<HRegionInfo,ServerName> regionsToScan = Maps.newTreeMap();
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[0], splits[1]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[1], splits[2]), SERVER_B);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[2], splits[3]), SERVER_B);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[3], splits[4]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[4], splits[5]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[5], splits[6]), SERVER_D);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[6], splits[7]), SERVER_C);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[7], splits[0]), SERVER_D);

    final List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    final DrillbitEndpoint DB_A = DrillbitEndpoint.newBuilder().setAddress(HOST_A).setControlPort(1234).build();
    endpoints.add(DB_A);
    endpoints.add(DB_A);
    final DrillbitEndpoint DB_B = DrillbitEndpoint.newBuilder().setAddress(HOST_B).setControlPort(1234).build();
    endpoints.add(DB_B);
    final DrillbitEndpoint DB_D = DrillbitEndpoint.newBuilder().setAddress(HOST_D).setControlPort(1234).build();
    endpoints.add(DB_D);
    final DrillbitEndpoint DB_X = DrillbitEndpoint.newBuilder().setAddress(HOST_X).setControlPort(1234).build();
    endpoints.add(DB_X);

    HBaseGroupScan scan = new HBaseGroupScan();
    scan.setRegionsToScan(regionsToScan);
    scan.setHBaseScanSpec(new HBaseScanSpec(TABLE_NAME_STR, splits[0], splits[0], null));
    scan.applyAssignments(endpoints);

    int i = 0;
    assertEquals(2, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'A'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'A'
    assertEquals(2, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'B'
    assertEquals(2, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'D'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'X'
    testParallelizationWidth(scan, i);
  }

  @Test
  public void testHBaseGroupScanAssignmentSomeAfinedWithOrphans() throws Exception {
    NavigableMap<HRegionInfo,ServerName> regionsToScan = Maps.newTreeMap();
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[0], splits[1]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[1], splits[2]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[2], splits[3]), SERVER_B);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[3], splits[4]), SERVER_B);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[4], splits[5]), SERVER_C);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[5], splits[6]), SERVER_C);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[6], splits[7]), SERVER_D);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[7], splits[8]), SERVER_D);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[8], splits[9]), SERVER_E);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[9], splits[10]), SERVER_E);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[10], splits[11]), SERVER_F);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[11], splits[12]), SERVER_F);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[12], splits[13]), SERVER_G);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[13], splits[14]), SERVER_G);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[14], splits[15]), SERVER_H);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[15], splits[16]), SERVER_H);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[16], splits[17]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[17], splits[0]), SERVER_A);

    final List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_A).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_B).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_C).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_D).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_E).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_F).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_G).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_H).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_I).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_J).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_K).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_L).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_M).setControlPort(1234).build());

    HBaseGroupScan scan = new HBaseGroupScan();
    scan.setRegionsToScan(regionsToScan);
    scan.setHBaseScanSpec(new HBaseScanSpec(TABLE_NAME_STR, splits[0], splits[0], null));
    scan.applyAssignments(endpoints);

    LinkedList<Integer> sizes = Lists.newLinkedList();
    Collections.addAll(sizes, 1, 1, 1, 1, 1, 1, 1, 1);
    Collections.addAll(sizes, 2, 2, 2, 2, 2);

    for (int i = 0; i < endpoints.size(); i++) {
      assertTrue(sizes.remove((Integer)scan.getSpecificScan(i).getRegionScanSpecList().size()));
    }
    assertEquals(0, sizes.size());
    testParallelizationWidth(scan, endpoints.size());
  }

  @Test
  public void testHBaseGroupScanAssignmentOneEach() throws Exception {
    NavigableMap<HRegionInfo,ServerName> regionsToScan = Maps.newTreeMap();
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[0], splits[1]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[1], splits[2]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[2], splits[3]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[3], splits[4]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[4], splits[5]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[5], splits[6]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[6], splits[7]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[7], splits[8]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[8], splits[0]), SERVER_A);

    final List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_A).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_A).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_B).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_C).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_D).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_E).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_F).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_G).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_H).setControlPort(1234).build());

    HBaseGroupScan scan = new HBaseGroupScan();
    scan.setRegionsToScan(regionsToScan);
    scan.setHBaseScanSpec(new HBaseScanSpec(TABLE_NAME_STR, splits[0], splits[0], null));
    scan.applyAssignments(endpoints);

    int i = 0;
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'A'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'A'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'B'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'C'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'D'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'E'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'F'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'G'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'H'
    testParallelizationWidth(scan, i);
  }

  @Test
  public void testHBaseGroupScanAssignmentNoAfinity() throws Exception {
    NavigableMap<HRegionInfo,ServerName> regionsToScan = Maps.newTreeMap();
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[0], splits[1]), SERVER_X);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[1], splits[2]), SERVER_X);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[2], splits[3]), SERVER_X);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[3], splits[4]), SERVER_X);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[4], splits[5]), SERVER_X);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[5], splits[6]), SERVER_X);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[6], splits[7]), SERVER_X);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[7], splits[0]), SERVER_X);

    final List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_A).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_B).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_C).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_D).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_E).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_F).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_G).setControlPort(1234).build());
    endpoints.add(DrillbitEndpoint.newBuilder().setAddress(HOST_H).setControlPort(1234).build());

    HBaseGroupScan scan = new HBaseGroupScan();
    scan.setRegionsToScan(regionsToScan);
    scan.setHBaseScanSpec(new HBaseScanSpec(TABLE_NAME_STR, splits[0], splits[0], null));
    scan.applyAssignments(endpoints);

    int i = 0;
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'A'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'B'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'C'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'D'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'E'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'F'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'G'
    assertEquals(1, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'H'
    testParallelizationWidth(scan, i);
  }

  @Test
  public void testHBaseGroupScanAssignmentAllPreferred() throws Exception {
    NavigableMap<HRegionInfo,ServerName> regionsToScan = Maps.newTreeMap();
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[0], splits[1]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[1], splits[2]), SERVER_A);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[2], splits[3]), SERVER_B);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[3], splits[4]), SERVER_B);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[4], splits[5]), SERVER_C);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[5], splits[6]), SERVER_C);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[6], splits[7]), SERVER_D);
    regionsToScan.put(new HRegionInfo(TABLE_NAME, splits[7], splits[0]), SERVER_D);

    final List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    final DrillbitEndpoint DB_A = DrillbitEndpoint.newBuilder().setAddress(HOST_A).setControlPort(1234).build();
    endpoints.add(DB_A);
    final DrillbitEndpoint DB_B = DrillbitEndpoint.newBuilder().setAddress(HOST_B).setControlPort(1234).build();
    endpoints.add(DB_B);
    final DrillbitEndpoint DB_D = DrillbitEndpoint.newBuilder().setAddress(HOST_C).setControlPort(1234).build();
    endpoints.add(DB_D);
    final DrillbitEndpoint DB_X = DrillbitEndpoint.newBuilder().setAddress(HOST_D).setControlPort(1234).build();
    endpoints.add(DB_X);

    HBaseGroupScan scan = new HBaseGroupScan();
    scan.setRegionsToScan(regionsToScan);
    scan.setHBaseScanSpec(new HBaseScanSpec(TABLE_NAME_STR, splits[0], splits[0], null));
    scan.applyAssignments(endpoints);

    int i = 0;
    assertEquals(2, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'A'
    assertEquals(2, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'B'
    assertEquals(2, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'C'
    assertEquals(2, scan.getSpecificScan(i++).getRegionScanSpecList().size()); // 'D'
    testParallelizationWidth(scan, i);
  }

  private void testParallelizationWidth(HBaseGroupScan scan, int i) {
    try {
      scan.getSpecificScan(i);
      fail("Should not have " + i + "th assignment or you have not enabled Java assertion.");
    } catch (AssertionError e) { }
  }
}
