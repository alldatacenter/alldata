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

package org.apache.celeborn.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.network.client.RpcResponseCallback;
import org.apache.celeborn.common.protocol.CompressionCodec;
import org.apache.celeborn.common.protocol.message.StatusCode;

public class ShuffleClientImplSuiteJ extends ShuffleClientBaseSuiteJ {
  static int BufferSize = 64;
  static byte[] TEST_BUF1 = new byte[BufferSize];
  static CelebornConf conf;

  @Before
  public void setup() throws IOException, InterruptedException {
    conf = setupEnv(CompressionCodec.LZ4);
  }

  public ByteBuf createByteBuf() {
    for (int i = BATCH_HEADER_SIZE; i < BufferSize; i++) {
      TEST_BUF1[i] = 1;
    }
    ByteBuf byteBuf = Unpooled.wrappedBuffer(TEST_BUF1);
    byteBuf.writerIndex(BufferSize);
    return byteBuf;
  }

  @Test
  public void testPushDataByteBufSuccess() throws IOException {
    ByteBuf byteBuf = createByteBuf();
    when(client.pushData(any(), anyLong(), any()))
        .thenAnswer(
            t -> {
              RpcResponseCallback rpcResponseCallback =
                  t.getArgumentAt(1, RpcResponseCallback.class);
              ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[0]);
              rpcResponseCallback.onSuccess(byteBuffer);
              return mockedFuture;
            });

    int pushDataLen =
        shuffleClient.pushDataToLocation(
            TEST_APPLICATION_ID,
            TEST_SHUFFLE_ID,
            TEST_ATTEMPT_ID,
            TEST_ATTEMPT_ID,
            TEST_REDUCRE_ID,
            byteBuf,
            masterLocation,
            () -> {});
    Assert.assertEquals(BufferSize, pushDataLen);
  }

  @Test
  public void testPushDataByteBufHardSplit() throws IOException {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(TEST_BUF1);
    when(client.pushData(any(), anyLong(), any()))
        .thenAnswer(
            t -> {
              RpcResponseCallback rpcResponseCallback =
                  t.getArgumentAt(1, RpcResponseCallback.class);
              ByteBuffer byteBuffer =
                  ByteBuffer.wrap(new byte[] {StatusCode.HARD_SPLIT.getValue()});
              rpcResponseCallback.onSuccess(byteBuffer);
              return mockedFuture;
            });
    int pushDataLen =
        shuffleClient.pushDataToLocation(
            TEST_APPLICATION_ID,
            TEST_SHUFFLE_ID,
            TEST_ATTEMPT_ID,
            TEST_ATTEMPT_ID,
            TEST_REDUCRE_ID,
            byteBuf,
            masterLocation,
            () -> {});
  }

  @Test
  public void testPushDataByteBufFail() throws IOException {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(TEST_BUF1);
    when(client.pushData(any(), anyLong(), any(), any()))
        .thenAnswer(
            t -> {
              RpcResponseCallback rpcResponseCallback =
                  t.getArgumentAt(1, RpcResponseCallback.class);
              rpcResponseCallback.onFailure(new Exception("pushDataFailed"));
              return mockedFuture;
            });
    // first push just  set pushdata.exception
    shuffleClient.pushDataToLocation(
        TEST_APPLICATION_ID,
        TEST_SHUFFLE_ID,
        TEST_ATTEMPT_ID,
        TEST_ATTEMPT_ID,
        TEST_REDUCRE_ID,
        byteBuf,
        masterLocation,
        () -> {});

    boolean isFailed = false;
    // second push will throw exception
    try {
      shuffleClient.pushDataToLocation(
          TEST_APPLICATION_ID,
          TEST_SHUFFLE_ID,
          TEST_ATTEMPT_ID,
          TEST_ATTEMPT_ID,
          TEST_REDUCRE_ID,
          byteBuf,
          masterLocation,
          () -> {});
    } catch (IOException e) {
      isFailed = true;
    } finally {
      Assert.assertTrue("should failed", isFailed);
    }
  }
}
