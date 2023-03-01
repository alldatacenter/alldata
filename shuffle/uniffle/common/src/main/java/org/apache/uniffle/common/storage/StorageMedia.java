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

public enum StorageMedia {
  UNKNOWN(0),
  HDD(1),
  SSD(2),
  HDFS(3),
  OBJECT_STORE(4);

  private final byte val;

  StorageMedia(int code) {
    assert (code >= -1 && code < 256);
    this.val = (byte) code;
  }

  public StorageInfo.StorageMedia toProto() {
    switch (this) {
      case UNKNOWN:
        return StorageInfo.StorageMedia.STORAGE_TYPE_UNKNOWN;
      case HDD:
        return StorageInfo.StorageMedia.HDD;
      case SSD:
        return StorageInfo.StorageMedia.SSD;
      case HDFS:
        return StorageInfo.StorageMedia.HDFS;
      case OBJECT_STORE:
        return StorageInfo.StorageMedia.OBJECT_STORE;
      default:
        return StorageInfo.StorageMedia.UNRECOGNIZED;
    }
  }

  public static StorageMedia fromProto(StorageInfo.StorageMedia storageMedia) {
    switch (storageMedia) {
      case HDD:
        return StorageMedia.HDD;
      case SSD:
        return StorageMedia.SSD;
      case HDFS:
        return StorageMedia.HDFS;
      case OBJECT_STORE:
        return StorageMedia.OBJECT_STORE;
      default:
        return StorageMedia.UNKNOWN;
    }
  }
}
