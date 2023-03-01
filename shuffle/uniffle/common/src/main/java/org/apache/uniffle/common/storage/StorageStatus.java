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

import org.apache.uniffle.proto.RssProtos.StorageInfo;

public enum StorageStatus {
  UNKNOWN(0),
  NORMAL(1),
  UNHEALTHY(2),
  OVERUSED(3);

  private final byte val;

  StorageStatus(int code) {
    assert (code >= -1 && code < 256);
    this.val = (byte) code;
  }

  public final byte getCode() {
    return val;
  }

  public StorageInfo.StorageStatus toProto() {
    switch (this) {
      case UNKNOWN:
        return StorageInfo.StorageStatus.STORAGE_STATUS_UNKNOWN;
      case NORMAL:
        return StorageInfo.StorageStatus.NORMAL;
      case UNHEALTHY:
        return StorageInfo.StorageStatus.UNHEALTHY;
      case OVERUSED:
        return StorageInfo.StorageStatus.OVERUSED;
      default:
        return StorageInfo.StorageStatus.UNRECOGNIZED;
    }
  }

  public static StorageStatus fromProto(StorageInfo.StorageStatus status) {
    switch (status) {
      case NORMAL:
        return StorageStatus.NORMAL;
      case UNHEALTHY:
        return StorageStatus.UNHEALTHY;
      case OVERUSED:
        return StorageStatus.OVERUSED;
      default:
        return StorageStatus.UNKNOWN;
    }
  }
}
