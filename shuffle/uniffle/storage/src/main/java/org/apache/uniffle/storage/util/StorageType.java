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

package org.apache.uniffle.storage.util;

public enum StorageType {
  MEMORY(1),
  LOCALFILE(2),
  HDFS(4),
  LOCALFILE_HDFS(6),
  MEMORY_LOCALFILE(3),
  MEMORY_HDFS(5),
  MEMORY_LOCALFILE_HDFS(7);

  private int val;

  StorageType(int val) {
    this.val = val;
  }

  private int getVal() {
    return val;
  }

  public static boolean withMemory(StorageType storageType) {
    return (storageType.getVal() & MEMORY.getVal()) != 0;
  }

  public static boolean withLocalfile(StorageType storageType) {
    return (storageType.getVal() & LOCALFILE.getVal()) != 0;
  }

  public static boolean withHDFS(StorageType storageType) {
    return (storageType.getVal() & HDFS.getVal()) != 0;
  }
}
