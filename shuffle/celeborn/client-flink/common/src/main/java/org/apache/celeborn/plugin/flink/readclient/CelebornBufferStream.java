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

package org.apache.celeborn.plugin.flink.readclient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.network.client.RpcResponseCallback;
import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.protocol.*;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.plugin.flink.network.FlinkTransportClientFactory;

public class CelebornBufferStream {

  private static Logger logger = LoggerFactory.getLogger(CelebornBufferStream.class);
  private CelebornConf conf;
  private FlinkTransportClientFactory clientFactory;
  private String shuffleKey;
  private PartitionLocation[] locations;
  private int subIndexStart;
  private int subIndexEnd;
  private TransportClient client;
  private int currentLocationIndex = 0;
  private long streamId = 0;
  private FlinkShuffleClientImpl mapShuffleClient;
  private boolean isClosed;
  private boolean isOpenSuccess;
  private Object lock = new Object();

  public CelebornBufferStream() {}

  public CelebornBufferStream(
      FlinkShuffleClientImpl mapShuffleClient,
      CelebornConf conf,
      FlinkTransportClientFactory dataClientFactory,
      String shuffleKey,
      PartitionLocation[] locations,
      int subIndexStart,
      int subIndexEnd) {
    this.mapShuffleClient = mapShuffleClient;
    this.conf = conf;
    this.clientFactory = dataClientFactory;
    this.shuffleKey = shuffleKey;
    this.locations = locations;
    this.subIndexStart = subIndexStart;
    this.subIndexEnd = subIndexEnd;
  }

  public void open(
      Supplier<ByteBuf> supplier, int initialCredit, Consumer<RequestMessage> messageConsumer)
      throws IOException, InterruptedException {
    this.client =
        clientFactory.createClientWithRetry(
            locations[currentLocationIndex].getHost(),
            locations[currentLocationIndex].getFetchPort());
    String fileName = locations[currentLocationIndex].getFileName();
    OpenStreamWithCredit openBufferStream =
        new OpenStreamWithCredit(shuffleKey, fileName, subIndexStart, subIndexEnd, initialCredit);
    client.sendRpc(
        openBufferStream.toByteBuffer(),
        new RpcResponseCallback() {

          @Override
          public void onSuccess(ByteBuffer response) {
            StreamHandle streamHandle = (StreamHandle) Message.decode(response);
            CelebornBufferStream.this.streamId = streamHandle.streamId;
            synchronized (lock) {
              if (!isClosed) {
                clientFactory.registerSupplier(CelebornBufferStream.this.streamId, supplier);
                mapShuffleClient
                    .getReadClientHandler()
                    .registerHandler(streamId, messageConsumer, client);
                isOpenSuccess = true;
                logger.debug(
                    "open stream success from remote:{}, stream id:{}, fileName: {}",
                    client.getSocketAddress(),
                    streamId,
                    fileName);
              } else {
                logger.debug(
                    "open stream success from remote:{}, but stream reader is already closed, stream id:{}, fileName: {}",
                    client.getSocketAddress(),
                    streamId,
                    fileName);
                closeStream();
              }
            }
          }

          @Override
          public void onFailure(Throwable e) {
            messageConsumer.accept(new TransportableError(streamId, e));
          }
        });
  }

  public void addCredit(ReadAddCredit addCredit) {
    this.client
        .getChannel()
        .writeAndFlush(addCredit)
        .addListener(
            future -> {
              if (future.isSuccess()) {
                // Send ReadAddCredit do not expect response.
              } else {
                logger.warn(
                    "Send ReadAddCredit to {} failed, detail {}",
                    this.client.getSocketAddress().toString(),
                    future.cause());
              }
            });
  }

  public static CelebornBufferStream empty() {
    return EMPTY_CELEBORN_BUFFER_STREAM;
  }

  public long getStreamId() {
    return streamId;
  }

  public static CelebornBufferStream create(
      FlinkShuffleClientImpl client,
      CelebornConf conf,
      FlinkTransportClientFactory dataClientFactory,
      String shuffleKey,
      PartitionLocation[] locations,
      int subIndexStart,
      int subIndexEnd) {
    if (locations == null || locations.length == 0) {
      return empty();
    } else {
      return new CelebornBufferStream(
          client, conf, dataClientFactory, shuffleKey, locations, subIndexStart, subIndexEnd);
    }
  }

  private static final CelebornBufferStream EMPTY_CELEBORN_BUFFER_STREAM =
      new CelebornBufferStream();

  private void closeStream() {
    if (client != null && client.isActive()) {
      client.getChannel().writeAndFlush(new BufferStreamEnd(streamId));
    }
  }

  public void close() {
    synchronized (lock) {
      if (isOpenSuccess) {
        mapShuffleClient.getReadClientHandler().removeHandler(getStreamId());
        clientFactory.unregisterSupplier(this.getStreamId());
        closeStream();
      }
      isClosed = true;
    }
  }
}
