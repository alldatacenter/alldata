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

package org.apache.uniffle.common;

import java.util.Arrays;
import java.util.Objects;

public class ShufflePartitionedBlock {

  private int length;
  private long crc;
  private long blockId;
  private int uncompressLength;
  private byte[] data;
  private long taskAttemptId;

  public ShufflePartitionedBlock(
      int length,
      int uncompressLength,
      long crc,
      long blockId,
      long taskAttemptId,
      byte[] data) {
    this.length = length;
    this.crc = crc;
    this.blockId = blockId;
    this.uncompressLength = uncompressLength;
    this.taskAttemptId = taskAttemptId;
    this.data = data;
  }

  // calculate the data size for this block in memory including metadata which are
  // blockId, crc, taskAttemptId, length, uncompressLength
  public long getSize() {
    return length + 3 * 8 + 2 * 4;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShufflePartitionedBlock that = (ShufflePartitionedBlock) o;
    return length == that.length
        && crc == that.crc
        && blockId == that.blockId
        && Arrays.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(length, crc, blockId, Arrays.hashCode(data));
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public long getCrc() {
    return crc;
  }

  public void setCrc(long crc) {
    this.crc = crc;
  }

  public long getBlockId() {
    return blockId;
  }

  public void setBlockId(long blockId) {
    this.blockId = blockId;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public int getUncompressLength() {
    return uncompressLength;
  }

  public long getTaskAttemptId() {
    return taskAttemptId;
  }

  public void setTaskAttemptId(long taskAttemptId) {
    this.taskAttemptId = taskAttemptId;
  }

  @Override
  public String toString() {
    return "ShufflePartitionedBlock{blockId[" + blockId + "], length[" + length
        + "], uncompressLength[" + uncompressLength + "], crc[" + crc + "], taskAttemptId[" + taskAttemptId + "]}";
  }

}
