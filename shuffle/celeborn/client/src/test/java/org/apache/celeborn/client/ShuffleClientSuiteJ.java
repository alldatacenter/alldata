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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import org.apache.celeborn.client.compress.Compressor;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.client.TransportClientFactory;
import org.apache.celeborn.common.protocol.CompressionCodec;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.protocol.message.ControlMessages.*;
import org.apache.celeborn.common.protocol.message.StatusCode;
import org.apache.celeborn.common.rpc.RpcEndpointRef;

public class ShuffleClientSuiteJ {

  private ShuffleClientImpl shuffleClient;
  private final RpcEndpointRef endpointRef = mock(RpcEndpointRef.class);
  private final TransportClientFactory clientFactory = mock(TransportClientFactory.class);
  private final TransportClient client = mock(TransportClient.class);

  private static final String TEST_APPLICATION_ID = "testapp1";
  private static final int TEST_SHUFFLE_ID = 1;
  private static final int TEST_ATTEMPT_ID = 0;
  private static final int TEST_REDUCRE_ID = 0;

  private static final int MASTER_RPC_PORT = 1234;
  private static final int MASTER_PUSH_PORT = 1235;
  private static final int MASTER_FETCH_PORT = 1236;
  private static final int MASTER_REPLICATE_PORT = 1237;
  private static final int SLAVE_RPC_PORT = 4321;
  private static final int SLAVE_PUSH_PORT = 4322;
  private static final int SLAVE_FETCH_PORT = 4323;
  private static final int SLAVE_REPLICATE_PORT = 4324;
  private static final PartitionLocation masterLocation =
      new PartitionLocation(
          0,
          1,
          "localhost",
          MASTER_RPC_PORT,
          MASTER_PUSH_PORT,
          MASTER_FETCH_PORT,
          MASTER_REPLICATE_PORT,
          PartitionLocation.Mode.MASTER);
  private static final PartitionLocation slaveLocation =
      new PartitionLocation(
          0,
          1,
          "localhost",
          SLAVE_RPC_PORT,
          SLAVE_PUSH_PORT,
          SLAVE_FETCH_PORT,
          SLAVE_REPLICATE_PORT,
          PartitionLocation.Mode.SLAVE);

  private static final byte[] TEST_BUF1 = "hello world".getBytes(StandardCharsets.UTF_8);
  private final int BATCH_HEADER_SIZE = 4 * 4;

  @Test
  public void testPushData() throws IOException, InterruptedException {
    for (CompressionCodec codec : CompressionCodec.values()) {
      CelebornConf conf = setupEnv(codec);

      int pushDataLen =
          shuffleClient.pushData(
              TEST_APPLICATION_ID,
              TEST_SHUFFLE_ID,
              TEST_ATTEMPT_ID,
              TEST_ATTEMPT_ID,
              TEST_REDUCRE_ID,
              TEST_BUF1,
              0,
              TEST_BUF1.length,
              1,
              1);

      Compressor compressor = Compressor.getCompressor(conf);
      compressor.compress(TEST_BUF1, 0, TEST_BUF1.length);
      final int compressedTotalSize = compressor.getCompressedTotalSize();

      assert (pushDataLen == compressedTotalSize + BATCH_HEADER_SIZE);
    }
  }

  @Test
  public void testMergeData() throws IOException, InterruptedException {
    for (CompressionCodec codec : CompressionCodec.values()) {
      CelebornConf conf = setupEnv(codec);

      int mergeSize =
          shuffleClient.mergeData(
              TEST_APPLICATION_ID,
              TEST_SHUFFLE_ID,
              TEST_ATTEMPT_ID,
              TEST_ATTEMPT_ID,
              TEST_REDUCRE_ID,
              TEST_BUF1,
              0,
              TEST_BUF1.length,
              1,
              1);

      Compressor compressor = Compressor.getCompressor(conf);
      compressor.compress(TEST_BUF1, 0, TEST_BUF1.length);
      final int compressedTotalSize = compressor.getCompressedTotalSize();

      shuffleClient.mergeData(
          TEST_APPLICATION_ID,
          TEST_SHUFFLE_ID,
          TEST_ATTEMPT_ID,
          TEST_ATTEMPT_ID,
          TEST_REDUCRE_ID,
          TEST_BUF1,
          0,
          TEST_BUF1.length,
          1,
          1);

      assert (mergeSize == compressedTotalSize + BATCH_HEADER_SIZE);

      byte[] buf1k = RandomStringUtils.random(4000).getBytes(StandardCharsets.UTF_8);
      int largeMergeSize =
          shuffleClient.mergeData(
              TEST_APPLICATION_ID,
              TEST_SHUFFLE_ID,
              TEST_ATTEMPT_ID,
              TEST_ATTEMPT_ID,
              TEST_REDUCRE_ID,
              buf1k,
              0,
              buf1k.length,
              1,
              1);

      compressor = Compressor.getCompressor(conf);
      compressor.compress(buf1k, 0, buf1k.length);
      int compressedTotalSize1 = compressor.getCompressedTotalSize();

      assert (largeMergeSize == compressedTotalSize1 + BATCH_HEADER_SIZE);
    }
  }

  private CelebornConf setupEnv(CompressionCodec codec) throws IOException, InterruptedException {
    CelebornConf conf = new CelebornConf();
    conf.set("celeborn.shuffle.compression.codec", codec.name());
    conf.set("celeborn.push.retry.threads", "1");
    conf.set("celeborn.push.buffer.size", "1K");
    shuffleClient = new ShuffleClientImpl(conf, new UserIdentifier("mock", "mock"));

    masterLocation.setPeer(slaveLocation);
    when(endpointRef.askSync(any(), any(), any()))
        .thenAnswer(
            t ->
                RegisterShuffleResponse$.MODULE$.apply(
                    StatusCode.SUCCESS, new PartitionLocation[] {masterLocation}));

    shuffleClient.setupMetaServiceRef(endpointRef);

    ChannelFuture mockedFuture =
        new ChannelFuture() {
          @Override
          public Channel channel() {
            return null;
          }

          @Override
          public ChannelFuture addListener(
              GenericFutureListener<? extends Future<? super Void>> listener) {
            return null;
          }

          @SafeVarargs
          @Override
          public final ChannelFuture addListeners(
              GenericFutureListener<? extends Future<? super Void>>... listeners) {
            return null;
          }

          @Override
          public ChannelFuture removeListener(
              GenericFutureListener<? extends Future<? super Void>> listener) {
            return null;
          }

          @SafeVarargs
          @Override
          public final ChannelFuture removeListeners(
              GenericFutureListener<? extends Future<? super Void>>... listeners) {
            return null;
          }

          @Override
          public ChannelFuture sync() {
            return null;
          }

          @Override
          public ChannelFuture syncUninterruptibly() {
            return null;
          }

          @Override
          public ChannelFuture await() {
            return null;
          }

          @Override
          public ChannelFuture awaitUninterruptibly() {
            return null;
          }

          @Override
          public boolean isVoid() {
            return false;
          }

          @Override
          public boolean isSuccess() {
            return true;
          }

          @Override
          public boolean isCancellable() {
            return false;
          }

          @Override
          public Throwable cause() {
            return null;
          }

          @Override
          public boolean await(long timeout, TimeUnit unit) {
            return true;
          }

          @Override
          public boolean await(long timeoutMillis) {
            return true;
          }

          @Override
          public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return true;
          }

          @Override
          public boolean awaitUninterruptibly(long timeoutMillis) {
            return true;
          }

          @Override
          public Void getNow() {
            return null;
          }

          @Override
          public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
          }

          @Override
          public boolean isCancelled() {
            return false;
          }

          @Override
          public boolean isDone() {
            return true;
          }

          @Override
          public Void get() {
            return null;
          }

          @SuppressWarnings("NullableProblems")
          @Override
          public Void get(long timeout, TimeUnit unit) {
            return null;
          }
        };

    when(client.pushData(any(), anyLong(), any())).thenAnswer(t -> mockedFuture);
    when(clientFactory.createClient(
            masterLocation.getHost(), masterLocation.getPushPort(), TEST_REDUCRE_ID))
        .thenAnswer(t -> client);

    when(client.pushMergedData(any(), anyLong(), any())).thenAnswer(t -> mockedFuture);
    when(clientFactory.createClient(masterLocation.getHost(), masterLocation.getPushPort()))
        .thenAnswer(t -> client);

    shuffleClient.dataClientFactory = clientFactory;
    return conf;
  }
}
