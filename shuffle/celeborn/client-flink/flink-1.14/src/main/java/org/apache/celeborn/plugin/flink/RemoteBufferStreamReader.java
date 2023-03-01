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
package org.apache.celeborn.plugin.flink;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.network.protocol.BacklogAnnouncement;
import org.apache.celeborn.common.network.protocol.ReadAddCredit;
import org.apache.celeborn.common.network.protocol.RequestMessage;
import org.apache.celeborn.plugin.flink.buffer.CreditListener;
import org.apache.celeborn.plugin.flink.buffer.TransferBufferPool;
import org.apache.celeborn.plugin.flink.protocol.ReadData;
import org.apache.celeborn.plugin.flink.readclient.FlinkShuffleClientImpl;
import org.apache.celeborn.plugin.flink.readclient.RssBufferStream;

public class RemoteBufferStreamReader extends CreditListener {
  private static Logger logger = LoggerFactory.getLogger(RemoteBufferStreamReader.class);
  private TransferBufferPool bufferPool;
  private FlinkShuffleClientImpl client;
  private String applicationId;
  private int shuffleId;
  private int partitionId;
  private int subPartitionIndexStart;
  private int subPartitionIndexEnd;
  private boolean isOpen;
  private Consumer<ByteBuf> dataListener;
  private Consumer<Throwable> failureListener;
  private RssBufferStream bufferStream;
  private volatile boolean closed = false;
  private Consumer<RequestMessage> messageConsumer;

  public RemoteBufferStreamReader(
      FlinkShuffleClientImpl client,
      ShuffleResourceDescriptor shuffleDescriptor,
      String applicationId,
      int startSubIdx,
      int endSubIdx,
      TransferBufferPool bufferPool,
      Consumer<ByteBuf> dataListener,
      Consumer<Throwable> failureListener) {
    this.client = client;
    this.applicationId = applicationId;
    this.shuffleId = shuffleDescriptor.getShuffleId();
    this.partitionId = shuffleDescriptor.getPartitionId();
    this.bufferPool = bufferPool;
    this.subPartitionIndexStart = startSubIdx;
    this.subPartitionIndexEnd = endSubIdx;
    this.dataListener = dataListener;
    this.failureListener = failureListener;
    this.messageConsumer =
        requestMessage -> {
          if (requestMessage instanceof ReadData) {
            dataReceived((ReadData) requestMessage);
          } else if (requestMessage instanceof BacklogAnnouncement) {
            backlogReceived(((BacklogAnnouncement) requestMessage).getBacklog());
          }
        };
  }

  public void open(int initialCredit) throws IOException {
    try {
      this.bufferStream =
          client.readBufferedPartition(
              applicationId, shuffleId, partitionId, subPartitionIndexStart, subPartitionIndexEnd);
      bufferStream.open(
          RemoteBufferStreamReader.this::requestBuffer, initialCredit, client, messageConsumer);
    } catch (InterruptedException e) {
      throw new RuntimeException("Failed to openStream.", e);
    }
    isOpen = true;
  }

  public void close() {
    isOpen = false;
    // need set closed first before remove Handler
    closed = true;
    if (this.bufferStream != null) {
      client.getReadClientHandler().removeHandler(this.bufferStream.getStreamId());
      bufferStream.close();
    } else {
      logger.warn(
          "bufferStream is null when closed, shuffleId: {}, partitionId: {}",
          shuffleId,
          partitionId);
    }
  }

  public boolean isOpened() {
    return isOpen;
  }

  public void notifyAvailableCredits(int numCredits) {
    if (!closed) {
      ReadAddCredit addCredit = new ReadAddCredit(this.bufferStream.getStreamId(), numCredits);
      bufferStream.addCredit(addCredit);
    }
  }

  public ByteBuf requestBuffer() {
    return bufferPool.requestBuffer();
  }

  public void backlogReceived(int backlog) {
    if (!closed) {
      bufferPool.reserveBuffers(this, backlog);
    }
  }

  public void dataReceived(ReadData readData) {
    logger.debug(
        "Rss buffer stream reader get streamid {} received readable bytes {}.",
        readData.getStreamId(),
        readData.getFlinkBuffer().readableBytes());
    if (closed) {
      readData.body().release();
      return;
    }
    dataListener.accept(readData.getFlinkBuffer());
  }
}
