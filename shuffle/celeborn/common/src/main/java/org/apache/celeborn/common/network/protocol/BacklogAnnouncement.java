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

package org.apache.celeborn.common.network.protocol;

import static org.apache.celeborn.common.network.protocol.Message.Type.BACKLOG_ANNOUNCEMENT;

import io.netty.buffer.ByteBuf;

// This RPC is sent to flink plugin to tell flink client to be ready for buffers.
public class BacklogAnnouncement extends RequestMessage {
  private long streamId;
  private int backlog;

  public BacklogAnnouncement(long streamId, int backlog) {
    this.streamId = streamId;
    this.backlog = backlog;
  }

  @Override
  public int encodedLength() {
    return 12;
  }

  @Override
  public void encode(ByteBuf buf) {
    buf.writeLong(streamId);
    buf.writeInt(backlog);
  }

  public static BacklogAnnouncement decode(ByteBuf in) {
    long streamId = in.readLong();
    int backlog = in.readInt();
    return new BacklogAnnouncement(streamId, backlog);
  }

  @Override
  public Type type() {
    return BACKLOG_ANNOUNCEMENT;
  }

  public long getStreamId() {
    return streamId;
  }

  public int getBacklog() {
    return backlog;
  }
}
