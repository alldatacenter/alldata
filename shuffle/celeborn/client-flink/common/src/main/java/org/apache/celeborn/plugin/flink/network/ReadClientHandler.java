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

package org.apache.celeborn.plugin.flink.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.protocol.BacklogAnnouncement;
import org.apache.celeborn.common.network.protocol.RequestMessage;
import org.apache.celeborn.common.network.server.BaseMessageHandler;
import org.apache.celeborn.plugin.flink.protocol.ReadData;

public class ReadClientHandler extends BaseMessageHandler {
  private static Logger logger = LoggerFactory.getLogger(ReadClientHandler.class);
  private ConcurrentHashMap<Long, Consumer<RequestMessage>> streamHandlers =
      new ConcurrentHashMap<>();
  private ConcurrentHashMap<Long, TransportClient> streamClients = new ConcurrentHashMap<>();

  public void registerHandler(
      long streamId, Consumer<RequestMessage> handle, TransportClient client) {
    streamHandlers.put(streamId, handle);
    streamClients.put(streamId, client);
  }

  public void removeHandler(long streamId) {
    streamHandlers.remove(streamId);
    streamClients.remove(streamId);
  }

  @Override
  public void receive(TransportClient client, RequestMessage msg) {
    long streamId = 0;
    switch (msg.type()) {
      case READ_DATA:
        ReadData readData = (ReadData) msg;
        streamId = readData.getStreamId();
        if (streamHandlers.containsKey(streamId)) {
          logger.debug(
              "received streamId: {}, readData size:{}",
              streamId,
              readData.getFlinkBuffer().readableBytes());
          streamHandlers.get(streamId).accept(msg);
        } else {
          logger.warn("Unexpected streamId received: {}", streamId);
        }
        break;
      case BACKLOG_ANNOUNCEMENT:
        BacklogAnnouncement backlogAnnouncement = (BacklogAnnouncement) msg;
        streamId = backlogAnnouncement.getStreamId();
        Consumer<RequestMessage> consumer = streamHandlers.get(streamId);
        if (consumer != null) {
          logger.debug(
              "received streamId: {}, backlog: {}", streamId, backlogAnnouncement.getBacklog());
          consumer.accept(msg);
        } else {
          logger.warn("Unexpected streamId received: {}", streamId);
        }
        break;
      case ONE_WAY_MESSAGE:
        // ignore it.
        break;
      default:
        logger.error("Unexpected msg type {} content {}", msg.type(), msg);
    }
  }

  @Override
  public boolean checkRegistered() {
    return true;
  }

  @Override
  public void channelInactive(TransportClient client) {
    streamClients.forEach(
        (streamId, savedClient) -> {
          if (savedClient == client) {
            logger.warn("Client {} is lost, remove related stream {}", savedClient, streamId);
          }
        });
  }

  @Override
  public void exceptionCaught(Throwable cause, TransportClient client) {
    logger.warn("exception caught {}", client.getSocketAddress(), cause);
  }

  public void close() {
    streamHandlers.clear();
    for (TransportClient value : streamClients.values()) {
      value.close();
    }
    streamClients.clear();
  }
}
