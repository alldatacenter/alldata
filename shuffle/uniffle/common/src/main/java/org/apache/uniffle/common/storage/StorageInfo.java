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

package org.apache.uniffle.common.storage;

import java.util.Objects;

import org.apache.uniffle.proto.RssProtos;

public class StorageInfo {
  private String mountPoint;
  private StorageMedia type;
  private long capacity;
  private long usedBytes;
  // -1 indicates these field is not used and shall not be serialized to proto.
  private long writingSpeed1M;
  private long writingSpeed5M;
  private long writingSpeed1H;
  private long numberOfWritingFailures;
  private StorageStatus status;

  public StorageInfo(
      String mountPoint,
      StorageMedia type,
      long capacity,
      long usedBytes,
      StorageStatus status) {
    this.mountPoint = mountPoint;
    this.type = type;
    this.capacity = capacity;
    this.usedBytes = usedBytes;
    this.writingSpeed1M = -1;
    this.writingSpeed5M = -1;
    this.writingSpeed1H = -1;
    this.numberOfWritingFailures = -1;
    this.status = status;
  }

  public StorageInfo(
      String mountPoint,
      StorageMedia type,
      long capacity,
      long usedBytes,
      long writingSpeed1M,
      long writingSpeed5M,
      long writingSpeed1H,
      long numberOfWritingFailures,
      StorageStatus status) {
    this.mountPoint = mountPoint;
    this.type = type;
    this.capacity = capacity;
    this.usedBytes = usedBytes;
    this.writingSpeed1M = writingSpeed1M;
    this.writingSpeed5M = writingSpeed5M;
    this.writingSpeed1H = writingSpeed1H;
    this.numberOfWritingFailures = numberOfWritingFailures;
    this.status = status;
  }


  public RssProtos.StorageInfo toProto() {
    RssProtos.StorageInfo.Builder builder = RssProtos.StorageInfo.newBuilder()
        .setMountPoint(mountPoint)
        .setStorageMedia(type.toProto())
        .setCapacity(capacity)
        .setUsedBytes(usedBytes)
        .setStatus(status.toProto());
    if (writingSpeed1M >= 0) {
      builder.setWritingSpeed1M(writingSpeed1M);
      builder.setWritingSpeed5M(writingSpeed5M);
      builder.setWritingSpeed1H(writingSpeed1H);
    }

    if (numberOfWritingFailures >= 0) {
      builder.setNumOfWritingFailures(numberOfWritingFailures);
    }

    return builder.build();
  }

  public StorageStatus getStatus() {
    return status;
  }

  public StorageMedia getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StorageInfo that = (StorageInfo) o;
    return Objects.equals(mountPoint, that.mountPoint) && type == that.type
        && capacity == that.capacity
        && usedBytes == that.usedBytes
        && writingSpeed1M == that.writingSpeed1M
        && writingSpeed5M == that.writingSpeed5M
        && writingSpeed1H == that.writingSpeed1H
        && numberOfWritingFailures == that.numberOfWritingFailures
        && status == that.status;

  }

  @Override
  public int hashCode() {
    int hash = 41;
    hash = (37 * hash) + Objects.hashCode(mountPoint);
    hash = (19 * hash) + Objects.hashCode(type);
    hash = (37 * hash) + (int) capacity;
    hash = (37 * hash) + (int) usedBytes;
    hash = (37 * hash) + (int) writingSpeed1M;
    hash = (37 * hash) + (int) writingSpeed5M;
    hash = (37 * hash) + (int) writingSpeed1H;
    hash = (37 * hash) + (int) numberOfWritingFailures;
    hash = (19 * hash) + Objects.hashCode(status);
    return hash;
  }
}
