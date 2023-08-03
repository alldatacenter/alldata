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

package org.apache.celeborn.common.protocol;

import org.junit.Test;
import org.roaringbitmap.RoaringBitmap;

public class PartitionLocationSuiteJ {

  private final int partitionId = 0;
  private final int epoch = 0;
  private final String host = "localhost";
  private final int rpcPort = 3;
  private final int pushPort = 1;
  private final int fetchPort = 2;
  private final int replicatePort = 4;
  private final PartitionLocation.Mode mode = PartitionLocation.Mode.PRIMARY;
  private final PartitionLocation peer =
      new PartitionLocation(
          partitionId,
          epoch,
          host,
          rpcPort,
          pushPort,
          fetchPort,
          replicatePort,
          PartitionLocation.Mode.REPLICA);

  @Test
  public void testGetCorrectMode() {
    byte primaryMode = 0;
    byte replicaMode = 1;

    assert PartitionLocation.getMode(primaryMode) == PartitionLocation.Mode.PRIMARY;
    assert PartitionLocation.getMode(replicaMode) == PartitionLocation.Mode.REPLICA;

    for (int i = 2; i < 255; ++i) {
      byte otherMode = (byte) i;
      // Should we return replica mode when the parameter passed in is neither 0 or 1?
      assert PartitionLocation.getMode(otherMode) == PartitionLocation.Mode.REPLICA;
    }
  }

  @Test
  public void testPartitionIdNotEqualMakePartitionLocationDifferent() {
    PartitionLocation location1 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    PartitionLocation location2 =
        new PartitionLocation(
            partitionId + 1, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    checkEqual(location1, location2, false);
  }

  @Test
  public void testEpochNotEqualMakePartitionLocationDifferent() {
    PartitionLocation location1 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    PartitionLocation location2 =
        new PartitionLocation(
            partitionId, epoch + 1, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    checkEqual(location1, location2, false);
  }

  @Test
  public void testHostNotEqualMakePartitionLocationDifferent() {
    PartitionLocation location1 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    PartitionLocation location2 =
        new PartitionLocation(
            partitionId,
            epoch,
            "remoteHost",
            rpcPort,
            pushPort,
            fetchPort,
            replicatePort,
            mode,
            peer);
    checkEqual(location1, location2, false);
  }

  @Test
  public void testPushPortNotEqualMakePartitionLocationDifferent() {
    PartitionLocation location1 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    PartitionLocation location2 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort + 1, fetchPort, replicatePort, mode, peer);
    checkEqual(location1, location2, false);
  }

  @Test
  public void testFetchPortNotEqualMakePartitionLocationDifferent() {
    PartitionLocation location1 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    PartitionLocation location2 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort + 1, replicatePort, mode, peer);
    checkEqual(location1, location2, false);
  }

  @Test
  public void testModeNotEqualNeverMakePartitionLocationDifferent() {
    PartitionLocation location1 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    PartitionLocation location2 =
        new PartitionLocation(
            partitionId,
            epoch,
            host,
            rpcPort,
            pushPort,
            fetchPort,
            replicatePort,
            PartitionLocation.Mode.REPLICA,
            peer);
    PartitionLocation location3 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    checkEqual(location1, location2, true);
    checkEqual(location1, location3, true);
    checkEqual(location2, location3, true);
  }

  @Test
  public void testPeerNotEqualNeverMakePartitionLocationDifferent() {
    PartitionLocation location1 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    PartitionLocation location2 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, location1);
    PartitionLocation location3 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    checkEqual(location1, location2, true);
    checkEqual(location1, location3, true);
    checkEqual(location2, location3, true);
  }

  @Test
  public void testAllFieldEqualMakePartitionLocationEqual() {
    PartitionLocation location1 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    PartitionLocation location2 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    checkEqual(location1, location2, true);
  }

  @Test
  public void testToStringOutput() {
    PartitionLocation location1 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode);
    PartitionLocation location2 =
        new PartitionLocation(
            partitionId, epoch, host, rpcPort, pushPort, fetchPort, replicatePort, mode, peer);
    StorageInfo storageInfo = new StorageInfo(StorageInfo.Type.MEMORY, "/mnt/disk/0");
    RoaringBitmap bitmap = new RoaringBitmap();
    bitmap.add(1);
    bitmap.add(2);
    bitmap.add(3);

    int partitionId = 1000;
    PartitionLocation location3 =
        new PartitionLocation(
            partitionId,
            epoch,
            host,
            rpcPort,
            pushPort,
            fetchPort,
            replicatePort,
            mode,
            peer,
            storageInfo,
            bitmap);

    String exp1 =
        "PartitionLocation[\n"
            + "  id-epoch:0-0\n"
            + "  host-rpcPort-pushPort-fetchPort-replicatePort:localhost-3-1-2-4\n"
            + "  mode:PRIMARY\n"
            + "  peer:(empty)\n"
            + "  storage hint:StorageInfo{type=MEMORY, mountPoint='UNKNOWN_DISK', finalResult=false, filePath=null}\n"
            + "  mapIdBitMap:{}]";
    String exp2 =
        "PartitionLocation[\n"
            + "  id-epoch:0-0\n"
            + "  host-rpcPort-pushPort-fetchPort-replicatePort:localhost-3-1-2-4\n"
            + "  mode:PRIMARY\n"
            + "  peer:(host-rpcPort-pushPort-fetchPort-replicatePort:localhost-3-1-2-4)\n"
            + "  storage hint:StorageInfo{type=MEMORY, mountPoint='UNKNOWN_DISK', finalResult=false, filePath=null}\n"
            + "  mapIdBitMap:{}]";
    String exp3 =
        "PartitionLocation[\n"
            + "  id-epoch:1000-0\n"
            + "  host-rpcPort-pushPort-fetchPort-replicatePort:localhost-3-1-2-4\n"
            + "  mode:PRIMARY\n"
            + "  peer:(host-rpcPort-pushPort-fetchPort-replicatePort:localhost-3-1-2-4)\n"
            + "  storage hint:StorageInfo{type=MEMORY, mountPoint='/mnt/disk/0', finalResult=false, filePath=null}\n"
            + "  mapIdBitMap:{1,2,3}]";
    assert exp1.equals(location1.toString());
    assert exp2.equals(location2.toString());
    assert exp3.equals(location3.toString());
  }

  private void checkEqual(
      PartitionLocation location1, PartitionLocation location2, boolean shouldEqual) {
    String errorMessage =
        "Need location1 "
            + location1
            + " and location2 "
            + location2
            + " are "
            + (shouldEqual ? "" : "not ")
            + "equal, but "
            + (shouldEqual ? "not " : "")
            + "equal.";
    assert location1.equals(location2) == shouldEqual : errorMessage;
  }
}
