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

import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;

public final class RegionStart extends RequestMessage {

  // 0 for master, 1 for slave, see PartitionLocation.Mode
  public final byte mode;

  public final String shuffleKey;
  public final String partitionUniqueId;
  public final int attemptId;
  public int currentRegionIndex;
  public Boolean isBroadcast;

  public RegionStart(
      byte mode,
      String shuffleKey,
      String partitionUniqueId,
      int attemptId,
      int currentRegionIndex,
      Boolean isBroadcast) {
    this.mode = mode;
    this.shuffleKey = shuffleKey;
    this.partitionUniqueId = partitionUniqueId;
    this.attemptId = attemptId;
    this.currentRegionIndex = currentRegionIndex;
    this.isBroadcast = isBroadcast;
  }

  @Override
  public Type type() {
    return Type.REGION_START;
  }

  @Override
  public int encodedLength() {
    return 1
        + Encoders.Strings.encodedLength(shuffleKey)
        + Encoders.Strings.encodedLength(partitionUniqueId)
        + 4
        + 4
        + 1;
  }

  @Override
  public void encode(ByteBuf buf) {
    buf.writeByte(mode);
    Encoders.Strings.encode(buf, shuffleKey);
    Encoders.Strings.encode(buf, partitionUniqueId);
    buf.writeInt(attemptId);
    buf.writeInt(currentRegionIndex);
    buf.writeBoolean(isBroadcast);
  }

  public static RegionStart decode(ByteBuf buf) {
    byte mode = buf.readByte();
    String shuffleKey = Encoders.Strings.decode(buf);
    String partitionUniqueId = Encoders.Strings.decode(buf);
    int attemptId = buf.readInt();
    int currentRegionIndex = buf.readInt();
    boolean isBroadCast = buf.readBoolean();
    return new RegionStart(
        mode, shuffleKey, partitionUniqueId, attemptId, currentRegionIndex, isBroadCast);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        mode, shuffleKey, partitionUniqueId, attemptId, currentRegionIndex, isBroadcast);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof RegionStart) {
      RegionStart o = (RegionStart) other;
      return mode == o.mode
          && shuffleKey.equals(o.shuffleKey)
          && partitionUniqueId.equals((o.partitionUniqueId))
          && attemptId == o.attemptId
          && currentRegionIndex == o.currentRegionIndex
          && isBroadcast == o.isBroadcast
          && super.equals(o);
    }
    return false;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("mode", mode)
        .add("shuffleKey", shuffleKey)
        .add("partitionUniqueId", partitionUniqueId)
        .add("attemptId", attemptId)
        .add("currentRegionIndex", currentRegionIndex)
        .add("isBroadcast", isBroadcast)
        .toString();
  }
}
