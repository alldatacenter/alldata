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

package org.apache.uniffle.storage.common;

import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.storage.StorageMedia;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultStorageMediaProviderTest {
  @Test
  public void testStorageProvider() {
    StorageMediaProvider provider = new DefaultStorageMediaProvider();
    // hdfs file should report hdfs type
    assertEquals(StorageMedia.HDFS, provider.getStorageMediaFor("hdfs://nn1/path/to/base"));
    // object store files
    assertEquals(StorageMedia.OBJECT_STORE, provider.getStorageMediaFor("s3://bucket-name/a/path"));
    assertEquals(StorageMedia.OBJECT_STORE, provider.getStorageMediaFor("cos://bucket-name/b/path"));

    // by default, the local file should report as HDD
    assertEquals(StorageMedia.HDD, provider.getStorageMediaFor("/path/to/base/dir"));
    assertEquals(StorageMedia.HDD, provider.getStorageMediaFor("file:///path/to/a/dir"));

    // invalid uri should also be reported as HDD
    assertEquals(StorageMedia.HDD, provider.getStorageMediaFor("file@xx:///path/to/a"));
  }

  @Test
  public void getGetDeviceName() {
    assertEquals("rootfs", DefaultStorageMediaProvider.getDeviceName("rootfs"));
    assertEquals("sda", DefaultStorageMediaProvider.getDeviceName("/dev/sda1"));
    assertEquals("cl-home", DefaultStorageMediaProvider.getDeviceName("/dev/mapper/cl-home"));
  }
}
