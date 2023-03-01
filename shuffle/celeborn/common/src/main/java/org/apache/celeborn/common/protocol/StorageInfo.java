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

package org.apache.celeborn.common.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StorageInfo implements Serializable {
  public static String UNKNOWN_DISK = "UNKNOWN_DISK";

  public enum Type {
    MEMORY(0),
    HDD(1),
    SSD(2),
    HDFS(3),
    OSS(4);
    private final int value;

    Type(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public static Map<Integer, Type> typesMap =
      new HashMap() {
        {
          for (Type type : Type.values()) {
            put(type.value, type);
          }
        }
      };

  // Default storage Type is MEMORY.
  private Type type = Type.MEMORY;
  private String mountPoint = UNKNOWN_DISK;
  // if a file is committed, field "finalResult" will be true
  private boolean finalResult = false;
  private String filePath;

  public StorageInfo() {}

  public StorageInfo(Type type, boolean isFinal, String filePath) {
    this.type = type;
    this.finalResult = isFinal;
    this.filePath = filePath;
  }

  public StorageInfo(String mountPoint) {
    this.mountPoint = mountPoint;
  }

  public StorageInfo(Type type, String mountPoint) {
    this.type = type;
    this.mountPoint = mountPoint;
  }

  public StorageInfo(Type type, String mountPoint, boolean finalResult) {
    this.type = type;
    this.mountPoint = mountPoint;
    this.finalResult = finalResult;
  }

  public StorageInfo(Type type, String mountPoint, boolean finalResult, String filePath) {
    this.type = type;
    this.mountPoint = mountPoint;
    this.finalResult = finalResult;
    this.filePath = filePath;
  }

  public boolean isFinalResult() {
    return finalResult;
  }

  public void setFinalResult(boolean finalResult) {
    this.finalResult = finalResult;
  }

  public String getMountPoint() {
    return mountPoint;
  }

  public void setMountPoint(String mountPoint) {
    this.mountPoint = mountPoint;
  }

  public Type getType() {
    return type;
  }

  public String getFilePath() {
    return filePath;
  }

  @Override
  public String toString() {
    return "StorageInfo{"
        + "type="
        + type
        + ", mountPoint='"
        + mountPoint
        + '\''
        + ", finalResult="
        + finalResult
        + ", filePath="
        + filePath
        + '}';
  }

  public static PbStorageInfo toPb(StorageInfo storageInfo) {
    String filePath = storageInfo.getFilePath();
    PbStorageInfo.Builder builder = PbStorageInfo.newBuilder();
    builder
        .setType(storageInfo.type.value)
        .setFinalResult(storageInfo.finalResult)
        .setMountPoint(storageInfo.mountPoint);
    if (filePath != null) {
      builder.setFilePath(filePath);
    }
    return builder.build();
  }

  public static StorageInfo fromPb(PbStorageInfo pbStorageInfo) {
    return new StorageInfo(
        typesMap.get(pbStorageInfo.getType()),
        pbStorageInfo.getMountPoint(),
        pbStorageInfo.getFinalResult(),
        pbStorageInfo.getFilePath());
  }
}
