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

package org.apache.celeborn.plugin.flink.protocol;

import java.util.Objects;

import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;

import org.apache.celeborn.common.network.protocol.RequestMessage;

public final class ReadData extends RequestMessage {
  private final long streamId;
  private final int backlog;
  private final long offset;
  private ByteBuf flinkBuffer;

  @Override
  public boolean needCopyOut() {
    return true;
  }

  public ReadData(long streamId, int backlog, long offset) {
    this.streamId = streamId;
    this.backlog = backlog;
    this.offset = offset;
  }

  @Override
  public int encodedLength() {
    return 8 + 4 + 8;
  }

  // This method will not be called because ReadData won't be created at flink client.
  @Override
  public void encode(io.netty.buffer.ByteBuf buf) {
    buf.writeLong(streamId);
    buf.writeInt(backlog);
    buf.writeLong(offset);
  }

  public long getStreamId() {
    return streamId;
  }

  public int getBacklog() {
    return backlog;
  }

  public long getOffset() {
    return offset;
  }

  @Override
  public Type type() {
    return Type.READ_DATA;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReadData readData = (ReadData) o;
    return streamId == readData.streamId
        && backlog == readData.backlog
        && offset == readData.offset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(streamId, backlog, offset);
  }

  @Override
  public String toString() {
    return "ReadData{"
        + "streamId="
        + streamId
        + ", backlog="
        + backlog
        + ", offset="
        + offset
        + '}';
  }

  public ByteBuf getFlinkBuffer() {
    return flinkBuffer;
  }

  public void setFlinkBuffer(ByteBuf flinkBuffer) {
    this.flinkBuffer = flinkBuffer;
  }
}
