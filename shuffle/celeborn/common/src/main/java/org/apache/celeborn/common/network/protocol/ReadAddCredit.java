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

import java.util.Objects;

import io.netty.buffer.ByteBuf;

public class ReadAddCredit extends RequestMessage {
  private long streamId;
  private int credit;

  public ReadAddCredit(long streamId, int credit) {
    this.streamId = streamId;
    this.credit = credit;
  }

  @Override
  public int encodedLength() {
    return 8 + 4;
  }

  @Override
  public void encode(ByteBuf buf) {
    buf.writeLong(streamId);
    buf.writeInt(credit);
  }

  public static ReadAddCredit decode(ByteBuf buf) {
    long streamId = buf.readLong();
    int credit = buf.readInt();
    return new ReadAddCredit(streamId, credit);
  }

  public long getStreamId() {
    return streamId;
  }

  public int getCredit() {
    return credit;
  }

  @Override
  public Type type() {
    return Type.READ_ADD_CREDIT;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReadAddCredit that = (ReadAddCredit) o;
    return streamId == that.streamId && credit == that.credit;
  }

  @Override
  public int hashCode() {
    return Objects.hash(streamId, credit);
  }

  @Override
  public String toString() {
    return "ReadAddCredit{" + "streamId=" + streamId + ", credit=" + credit + '}';
  }
}
