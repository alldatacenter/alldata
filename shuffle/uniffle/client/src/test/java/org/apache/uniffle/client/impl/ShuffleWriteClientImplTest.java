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

package org.apache.uniffle.client.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.apache.uniffle.client.api.ShuffleServerClient;
import org.apache.uniffle.client.response.RssSendShuffleDataResponse;
import org.apache.uniffle.client.response.SendShuffleDataResult;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.rpc.StatusCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShuffleWriteClientImplTest {

  @Test
  public void testAbandonEventWhenTaskFailed() {
    ShuffleWriteClientImpl shuffleWriteClient =
        new ShuffleWriteClientImpl("GRPC", 3, 2000, 4, 1, 1, 1, true, 1, 1, 10, 10);
    ShuffleServerClient mockShuffleServerClient = mock(ShuffleServerClient.class);
    ShuffleWriteClientImpl spyClient = Mockito.spy(shuffleWriteClient);
    doReturn(mockShuffleServerClient).when(spyClient).getShuffleServerClient(any());

    when(mockShuffleServerClient.sendShuffleData(any())).thenAnswer((Answer<String>) invocation -> {
      Thread.sleep(50000);
      return "ABCD1234";
    });

    List<ShuffleServerInfo> shuffleServerInfoList =
        Lists.newArrayList(new ShuffleServerInfo("id", "host", 0));
    List<ShuffleBlockInfo> shuffleBlockInfoList = Lists.newArrayList(new ShuffleBlockInfo(
        0, 0, 10, 10, 10, new byte[]{1}, shuffleServerInfoList, 10, 100, 0));

    // It should directly exit and wont do rpc request.
    Awaitility.await().timeout(1, TimeUnit.SECONDS).until(() -> {
      spyClient.sendShuffleData("appId", shuffleBlockInfoList, () -> true);
      return true;
    });
  }

  @Test
  public void testSendData() {
    ShuffleWriteClientImpl shuffleWriteClient =
        new ShuffleWriteClientImpl("GRPC", 3, 2000, 4, 1, 1, 1, true, 1, 1, 10, 10);
    ShuffleServerClient mockShuffleServerClient = mock(ShuffleServerClient.class);
    ShuffleWriteClientImpl spyClient = Mockito.spy(shuffleWriteClient);
    doReturn(mockShuffleServerClient).when(spyClient).getShuffleServerClient(any());
    when(mockShuffleServerClient.sendShuffleData(any())).thenReturn(
        new RssSendShuffleDataResponse(StatusCode.NO_BUFFER));

    List<ShuffleServerInfo> shuffleServerInfoList =
        Lists.newArrayList(new ShuffleServerInfo("id", "host", 0));
    List<ShuffleBlockInfo> shuffleBlockInfoList = Lists.newArrayList(new ShuffleBlockInfo(
        0, 0, 10, 10, 10, new byte[]{1}, shuffleServerInfoList, 10, 100, 0));
    SendShuffleDataResult result = spyClient.sendShuffleData("appId", shuffleBlockInfoList, () -> false);

    assertTrue(result.getFailedBlockIds().contains(10L));
  }

  @Test
  public void testRegisterAndUnRegisterShuffleServer() {
    ShuffleWriteClientImpl shuffleWriteClient =
        new ShuffleWriteClientImpl("GRPC", 3, 2000, 4, 1, 1, 1, true, 1, 1, 10, 10);
    String appId1 = "testRegisterAndUnRegisterShuffleServer-1";
    String appId2 = "testRegisterAndUnRegisterShuffleServer-2";
    ShuffleServerInfo server1 = new ShuffleServerInfo("host1-0", "host1", 0);
    ShuffleServerInfo server2 = new ShuffleServerInfo("host2-0", "host2", 0);
    ShuffleServerInfo server3 = new ShuffleServerInfo("host3-0", "host3", 0);
    shuffleWriteClient.addShuffleServer(appId1, 0, server1);
    shuffleWriteClient.addShuffleServer(appId1, 1, server2);
    shuffleWriteClient.addShuffleServer(appId2, 1, server3);
    assertEquals(2, shuffleWriteClient.getAllShuffleServers(appId1).size());
    assertEquals(1, shuffleWriteClient.getAllShuffleServers(appId2).size());
    shuffleWriteClient.addShuffleServer(appId1, 1, server1);
    shuffleWriteClient.unregisterShuffle(appId1, 1);
    assertEquals(1, shuffleWriteClient.getAllShuffleServers(appId1).size());
  }

  @Test
  public void testSendDataWithDefectiveServers() {
    ShuffleWriteClientImpl shuffleWriteClient =
        new ShuffleWriteClientImpl("GRPC", 3, 2000, 4, 3, 2, 2, true, 1, 1, 10, 10);
    ShuffleServerClient mockShuffleServerClient = mock(ShuffleServerClient.class);
    ShuffleWriteClientImpl spyClient = Mockito.spy(shuffleWriteClient);
    doReturn(mockShuffleServerClient).when(spyClient).getShuffleServerClient(any());
    when(mockShuffleServerClient.sendShuffleData(any())).thenReturn(
        new RssSendShuffleDataResponse(StatusCode.NO_BUFFER),
        new RssSendShuffleDataResponse(StatusCode.SUCCESS),
        new RssSendShuffleDataResponse(StatusCode.SUCCESS));

    String appId = "testSendDataWithDefectiveServers_appId";
    ShuffleServerInfo ssi1 = new ShuffleServerInfo("127.0.0.1", 0);
    ShuffleServerInfo ssi2 = new ShuffleServerInfo("127.0.0.1", 1);
    ShuffleServerInfo ssi3 = new ShuffleServerInfo("127.0.0.1", 2);
    List<ShuffleServerInfo> shuffleServerInfoList =
        Lists.newArrayList(ssi1, ssi2, ssi3);
    List<ShuffleBlockInfo> shuffleBlockInfoList = Lists.newArrayList(new ShuffleBlockInfo(
        0, 0, 10, 10, 10, new byte[]{1}, shuffleServerInfoList, 10, 100, 0));
    SendShuffleDataResult result = spyClient.sendShuffleData(appId, shuffleBlockInfoList, () -> false);
    assertEquals(0, result.getFailedBlockIds().size());

    // Send data for the second time, the first shuffle server will be moved to the last.
    when(mockShuffleServerClient.sendShuffleData(any())).thenReturn(
        new RssSendShuffleDataResponse(StatusCode.SUCCESS),
        new RssSendShuffleDataResponse(StatusCode.SUCCESS));
    List<ShuffleServerInfo> excludeServers = new ArrayList<>();
    spyClient.genServerToBlocks(shuffleBlockInfoList.get(0), shuffleServerInfoList,
        2, excludeServers, Maps.newHashMap(), Maps.newHashMap(), true);
    assertEquals(2, excludeServers.size());
    assertEquals(ssi2, excludeServers.get(0));
    assertEquals(ssi3, excludeServers.get(1));
    spyClient.genServerToBlocks(shuffleBlockInfoList.get(0), shuffleServerInfoList,
        1, excludeServers, Maps.newHashMap(), Maps.newHashMap(), false);
    assertEquals(3, excludeServers.size());
    assertEquals(ssi1, excludeServers.get(2));
    result = spyClient.sendShuffleData(appId, shuffleBlockInfoList, () -> false);
    assertEquals(0, result.getFailedBlockIds().size());

    // Send data for the third time, the first server will be removed from the defectiveServers
    // and the second server will be added to the defectiveServers.
    when(mockShuffleServerClient.sendShuffleData(any())).thenReturn(
        new RssSendShuffleDataResponse(StatusCode.NO_BUFFER),
        new RssSendShuffleDataResponse(StatusCode.SUCCESS),
        new RssSendShuffleDataResponse(StatusCode.SUCCESS));
    List<ShuffleServerInfo> shuffleServerInfoList2 = Lists.newArrayList(ssi2, ssi1, ssi3);
    List<ShuffleBlockInfo> shuffleBlockInfoList2 = Lists.newArrayList(new ShuffleBlockInfo(0, 0, 10, 10, 10,
        new byte[]{1}, shuffleServerInfoList2, 10, 100, 0));
    result = spyClient.sendShuffleData(appId, shuffleBlockInfoList2, () -> false);
    assertEquals(0, result.getFailedBlockIds().size());
    assertEquals(1, spyClient.getDefectiveServers().size());
    assertEquals(ssi2, spyClient.getDefectiveServers().toArray()[0]);
    excludeServers = new ArrayList<>();
    spyClient.genServerToBlocks(shuffleBlockInfoList.get(0), shuffleServerInfoList,
        2, excludeServers, Maps.newHashMap(), Maps.newHashMap(), true);
    assertEquals(2, excludeServers.size());
    assertEquals(ssi1, excludeServers.get(0));
    assertEquals(ssi3, excludeServers.get(1));
    spyClient.genServerToBlocks(shuffleBlockInfoList.get(0), shuffleServerInfoList,
        1, excludeServers, Maps.newHashMap(), Maps.newHashMap(), false);
    assertEquals(3, excludeServers.size());
    assertEquals(ssi2, excludeServers.get(2));

    // Check whether it is normal when two shuffle servers in defectiveServers
    spyClient.getDefectiveServers().add(ssi1);
    assertEquals(2, spyClient.getDefectiveServers().size());
    excludeServers = new ArrayList<>();
    spyClient.genServerToBlocks(shuffleBlockInfoList.get(0), shuffleServerInfoList,
        2, excludeServers, Maps.newHashMap(), Maps.newHashMap(), true);
    assertEquals(2, excludeServers.size());
    assertEquals(ssi3, excludeServers.get(0));
    assertEquals(ssi1, excludeServers.get(1));
  }

}
