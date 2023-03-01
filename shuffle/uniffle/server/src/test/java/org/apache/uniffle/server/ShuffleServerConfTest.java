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

package org.apache.uniffle.server;

import java.io.File;

import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.util.ByteUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

public class ShuffleServerConfTest {

  private static final String confFile = ClassLoader.getSystemResource("confTest.conf").getFile();

  @Test
  public void defaultConfTest() {
    ShuffleServerConf shuffleServerConf = new ShuffleServerConf();
    assertFalse(shuffleServerConf.loadConfFromFile(null));
    assertEquals("GRPC", shuffleServerConf.getString(ShuffleServerConf.RPC_SERVER_TYPE));
    assertEquals(256, shuffleServerConf.getInteger(ShuffleServerConf.JETTY_CORE_POOL_SIZE));
    assertEquals(0, shuffleServerConf.getLong(ShuffleServerConf.SERVER_SHUFFLE_FLUSH_THRESHOLD));
  }

  @Test
  public void envConfTest() throws Exception {
    withEnvironmentVariables("RSS_HOME", (new File(confFile)).getParent()).execute(() -> {
      ShuffleServerConf shuffleServerConf = new ShuffleServerConf(null);
      assertEquals(1234, shuffleServerConf.getInteger(ShuffleServerConf.RPC_SERVER_PORT));
      assertEquals("HDFS", shuffleServerConf.getString(ShuffleServerConf.RSS_STORAGE_TYPE));
      assertEquals("/var/tmp/test", shuffleServerConf.get(ShuffleServerConf.RSS_STORAGE_BASE_PATH).get(0));
      withEnvironmentVariables("RSS_HOME", (new File(confFile)).getParent() + "/wrong_dir/")
          .execute(() -> assertFalse(shuffleServerConf.loadConfFromFile(null)));
    });
  }

  @Test
  public void confTest() {
    ShuffleServerConf shuffleServerConf = new ShuffleServerConf(confFile);
    assertEquals(1234, shuffleServerConf.getInteger(ShuffleServerConf.RPC_SERVER_PORT));
    assertEquals("FILE", shuffleServerConf.getString(ShuffleServerConf.RSS_STORAGE_TYPE));
    assertEquals("/var/tmp/test", shuffleServerConf.get(ShuffleServerConf.RSS_STORAGE_BASE_PATH).get(0));
    assertFalse(shuffleServerConf.loadConfFromFile("/var/tmp/null"));
    assertEquals(2, shuffleServerConf.getLong(ShuffleServerConf.SERVER_BUFFER_CAPACITY));
    assertEquals("value1", shuffleServerConf.getString("rss.server.hadoop.a.b", ""));
    assertEquals("", shuffleServerConf.getString("rss.server.had.a.b", ""));
    assertEquals("GRPC", shuffleServerConf.getString(ShuffleServerConf.RPC_SERVER_TYPE));
  }

  @Test
  public void confByStringTest() {
    final long GB = (long)ByteUnit.GiB.toBytes(1L);
    final long MB = (long)ByteUnit.MiB.toBytes(1L);
    final long KB = (long)ByteUnit.KiB.toBytes(1L);
    final String confBySizeStringFile = ClassLoader.getSystemResource("confBySizeStringTest.conf").getFile();
    ShuffleServerConf shuffleServerConf = new ShuffleServerConf(confBySizeStringFile);

    // test read conf from file
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_BUFFER_CAPACITY), 10 * KB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_READ_BUFFER_CAPACITY), 32 * KB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_WRITE_SLOW_THRESHOLD), 10 * GB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L1), 45 * MB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L2), 90 * MB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L3), 120 * MB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.DISK_CAPACITY), 120 * GB);

    // set conf in memory
    shuffleServerConf.setSizeAsBytes(ShuffleServerConf.SERVER_BUFFER_CAPACITY, "10KB");
    shuffleServerConf.setSizeAsBytes(ShuffleServerConf.SERVER_READ_BUFFER_CAPACITY, "32kb");
    shuffleServerConf.setSizeAsBytes(ShuffleServerConf.SERVER_WRITE_SLOW_THRESHOLD, "10GB");
    shuffleServerConf.setSizeAsBytes(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L1, "45MB");
    shuffleServerConf.setSizeAsBytes(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L2, "90MB");
    shuffleServerConf.setSizeAsBytes(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L3, "120MB");
    shuffleServerConf.setSizeAsBytes(ShuffleServerConf.DISK_CAPACITY, "120GB");

    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_BUFFER_CAPACITY), 10 * KB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_READ_BUFFER_CAPACITY), 32 * KB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_WRITE_SLOW_THRESHOLD), 10 * GB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L1), 45 * MB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L2), 90 * MB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L3), 120 * MB);
    assertEquals(shuffleServerConf.getSizeAsBytes(ShuffleServerConf.DISK_CAPACITY), 120 * GB);

    // test the compatibility of old style
    assertEquals(shuffleServerConf.getLong(ShuffleServerConf.SERVER_BUFFER_CAPACITY), 10 * KB);
    assertEquals(shuffleServerConf.getLong(ShuffleServerConf.SERVER_READ_BUFFER_CAPACITY), 32 * KB);
    assertEquals(shuffleServerConf.getLong(ShuffleServerConf.SERVER_WRITE_SLOW_THRESHOLD), 10 * GB);
    assertEquals(shuffleServerConf.getLong(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L1), 45 * MB);
    assertEquals(shuffleServerConf.getLong(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L2), 90 * MB);
    assertEquals(shuffleServerConf.getLong(ShuffleServerConf.SERVER_EVENT_SIZE_THRESHOLD_L3), 120 * MB);
    assertEquals(shuffleServerConf.getLong(ShuffleServerConf.DISK_CAPACITY), 120 * GB);
  }
}
