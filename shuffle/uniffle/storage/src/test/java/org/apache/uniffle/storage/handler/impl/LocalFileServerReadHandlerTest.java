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

package org.apache.uniffle.storage.handler.impl;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.api.ShuffleServerClient;
import org.apache.uniffle.client.request.RssGetShuffleDataRequest;
import org.apache.uniffle.client.response.RssGetShuffleDataResponse;
import org.apache.uniffle.client.response.RssGetShuffleIndexResponse;
import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.common.rpc.StatusCode;
import org.apache.uniffle.storage.common.FileBasedShuffleSegment;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalFileServerReadHandlerTest {
  @Test
  public void testDataInconsistent() throws Exception {
    Map<Long, byte[]> expectedData = Maps.newHashMap();
    int expectTotalBlockNum = 4;
    int blockSize = 7;

    ByteBuffer byteBuffer = ByteBuffer.allocate(expectTotalBlockNum * 40);
    Roaring64NavigableMap expectBlockIds = Roaring64NavigableMap.bitmapOf();

    // We simulate the generation of 4 block index files and 3 block data files to test LocalFileClientReadHandler
    List<ShufflePartitionedBlock> blocks = LocalFileHandlerTestBase.generateBlocks(expectTotalBlockNum, blockSize);
    LocalFileHandlerTestBase.writeTestData(blocks, shuffleBlocks -> {
      int offset = 0;
      for (ShufflePartitionedBlock block : shuffleBlocks) {
        FileBasedShuffleSegment segment = new FileBasedShuffleSegment(
            block.getBlockId(), offset, block.getLength(), block.getUncompressLength(),
            block.getCrc(), block.getTaskAttemptId());
        offset += block.getLength();
        LocalFileHandlerTestBase.writeIndex(byteBuffer, segment);
      }
    }, expectedData, new HashSet<>());

    blocks.forEach(block -> expectBlockIds.addLong(block.getBlockId()));

    String appId = "app1";
    int shuffleId = 1;
    int partitionId = 1;
    ShuffleServerClient mockShuffleServerClient = Mockito.mock(ShuffleServerClient.class);

    int actualWriteDataBlock = expectTotalBlockNum - 1;
    int actualFileLen = blockSize * actualWriteDataBlock;
    RssGetShuffleIndexResponse response = new RssGetShuffleIndexResponse(StatusCode.SUCCESS,
        byteBuffer.array(), actualFileLen);
    Mockito.doReturn(response).when(mockShuffleServerClient).getShuffleIndex(Mockito.any());

    int readBufferSize = 13;
    int bytesPerSegment = ((readBufferSize / blockSize) + 1) * blockSize;
    List<Long> actualWriteBlockIds = blocks.stream().map(ShufflePartitionedBlock::getBlockId)
        .limit(actualWriteDataBlock).collect(Collectors.toList());
    List<byte[]> segments = LocalFileHandlerTestBase.calcSegmentBytes(expectedData,
        bytesPerSegment, actualWriteBlockIds);

    // first segment include 2 blocks
    ArgumentMatcher<RssGetShuffleDataRequest> segment1Match =
        (request) -> request.getOffset() == 0 && request.getLength() == bytesPerSegment;
    // second segment include 1 block
    ArgumentMatcher<RssGetShuffleDataRequest> segment2Match =
        (request) -> request.getOffset() == bytesPerSegment && request.getLength() == blockSize;
    RssGetShuffleDataResponse segment1Response =
        new RssGetShuffleDataResponse(StatusCode.SUCCESS, segments.get(0));
    RssGetShuffleDataResponse segment2Response =
        new RssGetShuffleDataResponse(StatusCode.SUCCESS, segments.get(1));

    Mockito.doReturn(segment1Response).when(mockShuffleServerClient).getShuffleData(Mockito.argThat(segment1Match));
    Mockito.doReturn(segment2Response).when(mockShuffleServerClient).getShuffleData(Mockito.argThat(segment2Match));

    Roaring64NavigableMap processBlockIds =  Roaring64NavigableMap.bitmapOf();
    LocalFileClientReadHandler handler = new LocalFileClientReadHandler(appId, partitionId, shuffleId, -1, 1, 1,
        readBufferSize, expectBlockIds, processBlockIds, mockShuffleServerClient,
        ShuffleDataDistributionType.NORMAL, Roaring64NavigableMap.bitmapOf());
    int totalSegment = ((blockSize * actualWriteDataBlock) / bytesPerSegment) + 1;
    int readBlocks = 0;
    for (int i = 0; i < totalSegment; i++) {
      ShuffleDataResult result = handler.readShuffleData();
      LocalFileHandlerTestBase.checkData(result, expectedData);
      readBlocks += result.getBufferSegments().size();
    }
    assertEquals(actualWriteDataBlock, readBlocks);
  }

}
