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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StorageTypeTest {

  @Test
  public void commonTest() {
    String type = "HDFS";
    assertEquals(StorageType.valueOf(type), StorageType.HDFS);

    StorageType storageType = StorageType.MEMORY;
    assertTrue(StorageType.withMemory(storageType));
    assertFalse(StorageType.withLocalfile(storageType));
    assertFalse(StorageType.withHDFS(storageType));

    storageType = StorageType.LOCALFILE;
    assertFalse(StorageType.withMemory(storageType));
    assertTrue(StorageType.withLocalfile(storageType));
    assertFalse(StorageType.withHDFS(storageType));

    storageType = StorageType.HDFS;
    assertFalse(StorageType.withMemory(storageType));
    assertFalse(StorageType.withLocalfile(storageType));
    assertTrue(StorageType.withHDFS(storageType));

    storageType = StorageType.MEMORY_HDFS;
    assertTrue(StorageType.withMemory(storageType));
    assertFalse(StorageType.withLocalfile(storageType));
    assertTrue(StorageType.withHDFS(storageType));

    storageType = StorageType.MEMORY_LOCALFILE;
    assertTrue(StorageType.withMemory(storageType));
    assertTrue(StorageType.withLocalfile(storageType));
    assertFalse(StorageType.withHDFS(storageType));

    storageType = StorageType.MEMORY_LOCALFILE_HDFS;
    assertTrue(StorageType.withMemory(storageType));
    assertTrue(StorageType.withLocalfile(storageType));
    assertTrue(StorageType.withHDFS(storageType));

    storageType = StorageType.LOCALFILE_HDFS;
    assertFalse(StorageType.withMemory(storageType));
    assertTrue(StorageType.withLocalfile(storageType));
    assertTrue(StorageType.withHDFS(storageType));
  }
}
