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

package org.apache.uniffle.common.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.config.RssBaseConf;
import org.apache.uniffle.common.config.RssConf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

public class RssUtilsTest {

  @Test
  public void testGetPropertiesFromFile() {
    final String filePath = Objects.requireNonNull(
        getClass().getClassLoader().getResource("rss-defaults.conf")).getFile();
    Map<String, String> properties = RssUtils.getPropertiesFromFile(filePath);
    assertEquals("12121", properties.get("rss.coordinator.port"));
    assertEquals("155", properties.get("rss.server.heartbeat.interval"));
    assertEquals("true", properties.get("rss.x.y.z"));
    assertEquals("-XX:+PrintGCDetails-Dkey=value-Dnumbers=\"one two three\"",
        properties.get("rss.a.b.c.extraJavaOptions"));
  }

  @Test
  public void testGetHostIp() {
    try {
      String realIp = RssUtils.getHostIp();
      InetAddress ia = InetAddress.getByName(realIp);
      assertTrue(ia instanceof Inet4Address);
      assertFalse(ia.isLinkLocalAddress() || ia.isAnyLocalAddress() || ia.isLoopbackAddress());
      assertNotNull(NetworkInterface.getByInetAddress(ia));
      assertTrue(ia.isReachable(5000));
      withEnvironmentVariable("RSS_IP", "8.8.8.8")
          .execute(() -> assertEquals("8.8.8.8", RssUtils.getHostIp()));
      withEnvironmentVariable("RSS_IP", "xxxx").execute(() -> {
        boolean isException = false;
        try {
          RssUtils.getHostIp();
        } catch (Exception e) {
          isException = true;
        }
        assertTrue(isException);
      });
      withEnvironmentVariable("RSS_IP", realIp).execute(RssUtils::getHostIp);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testSerializeBitmap() throws Exception {
    Roaring64NavigableMap bitmap1 = Roaring64NavigableMap.bitmapOf(1, 2, 100, 10000);
    byte[] bytes = RssUtils.serializeBitMap(bitmap1);
    Roaring64NavigableMap bitmap2 = RssUtils.deserializeBitMap(bytes);
    assertEquals(bitmap1, bitmap2);
    assertEquals(Roaring64NavigableMap.bitmapOf(), RssUtils.deserializeBitMap(new byte[]{}));
  }

  @Test
  public void testCloneBitmap() {
    Roaring64NavigableMap bitmap1 = Roaring64NavigableMap.bitmapOf(1, 2, 100, 10000);
    Roaring64NavigableMap bitmap2 = RssUtils.cloneBitMap(bitmap1);
    assertNotSame(bitmap1, bitmap2);
    assertEquals(bitmap1, bitmap2);
  }

  @Test
  public void getMetricNameForHostNameTest() {
    assertEquals("a_b_c", RssUtils.getMetricNameForHostName("a.b.c"));
    assertEquals("a_b_c", RssUtils.getMetricNameForHostName("a-b-c"));
    assertEquals("a_b_c", RssUtils.getMetricNameForHostName("a.b-c"));
  }

  @Test
  public void testLoadExtentions() {
    List<String> exts = Collections.singletonList("Dummy");
    try {
      RssUtils.loadExtensions(RssUtilTestDummy.class, exts, 1);
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().startsWith("java.lang.ClassNotFoundException: Dummy"));
    }
    exts = Collections.singletonList("org.apache.uniffle.common.util.RssUtilsTest$RssUtilTestDummyFailNotSub");
    try {
      RssUtils.loadExtensions(RssUtilTestDummy.class, exts, 1);
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("RssUtilTestDummyFailNotSub is not subclass of "
          + "org.apache.uniffle.common.util.RssUtilsTest$RssUtilTestDummy"));
    }
    exts = Collections.singletonList("org.apache.uniffle.common.util.RssUtilsTest$RssUtilTestDummyNoConstructor");
    try {
      RssUtils.loadExtensions(RssUtilTestDummy.class, exts, "Test");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("RssUtilTestDummyNoConstructor.<init>()"));
    }
    exts = Collections.singletonList("org.apache.uniffle.common.util.RssUtilsTest$RssUtilTestDummySuccess");
    String testStr = String.valueOf(new Random().nextInt());
    List<RssUtilTestDummy> extsObjs = RssUtils.loadExtensions(RssUtilTestDummy.class, exts, testStr);
    assertEquals(1, extsObjs.size());
    assertEquals(testStr, extsObjs.get(0).get());
  }

  @Test
  public void testShuffleBitmapToPartitionBitmap() {
    Roaring64NavigableMap partition1Bitmap = Roaring64NavigableMap.bitmapOf(
        getBlockId(0, 0, 0),
        getBlockId(0, 0, 1),
        getBlockId(0, 1, 0),
        getBlockId(0, 1, 1));
    Roaring64NavigableMap partition2Bitmap = Roaring64NavigableMap.bitmapOf(
        getBlockId(1, 0, 0),
        getBlockId(1, 0, 1),
        getBlockId(1, 1, 0),
        getBlockId(1, 1, 1));
    Roaring64NavigableMap shuffleBitmap = Roaring64NavigableMap.bitmapOf();
    shuffleBitmap.or(partition1Bitmap);
    shuffleBitmap.or(partition2Bitmap);
    assertEquals(8, shuffleBitmap.getLongCardinality());
    Map<Integer, Roaring64NavigableMap> toPartitionBitmap =
        RssUtils.generatePartitionToBitmap(shuffleBitmap, 0, 2);
    assertEquals(2, toPartitionBitmap.size());
    assertEquals(partition1Bitmap, toPartitionBitmap.get(0));
    assertEquals(partition2Bitmap, toPartitionBitmap.get(1));
  }

  @Test
  public void testGenerateServerToPartitions() {
    Map<Integer, List<ShuffleServerInfo>> partitionToServers = Maps.newHashMap();
    ShuffleServerInfo server1 = new ShuffleServerInfo("server1", "0.0.0.1", 100);
    ShuffleServerInfo server2 = new ShuffleServerInfo("server2", "0.0.0.2", 200);
    ShuffleServerInfo server3 = new ShuffleServerInfo("server3", "0.0.0.3", 300);
    ShuffleServerInfo server4 = new ShuffleServerInfo("server4", "0.0.0.4", 400);
    partitionToServers.put(1, Lists.newArrayList(server1, server2));
    partitionToServers.put(2, Lists.newArrayList(server3, server4));
    partitionToServers.put(3, Lists.newArrayList(server1, server2));
    partitionToServers.put(4, Lists.newArrayList(server3, server4));
    Map<ShuffleServerInfo, Set<Integer>> serverToPartitions = RssUtils.generateServerToPartitions(partitionToServers);
    assertEquals(4, serverToPartitions.size());
    assertEquals(serverToPartitions.get(server1), Sets.newHashSet(1, 3));
    assertEquals(serverToPartitions.get(server2), Sets.newHashSet(1, 3));
    assertEquals(serverToPartitions.get(server3), Sets.newHashSet(2, 4));
    assertEquals(serverToPartitions.get(server4), Sets.newHashSet(2, 4));
  }

  @Test
  public void testGetConfiguredLocalDirs() throws Exception {
    RssConf conf = new RssConf();
    withEnvironmentVariable(RssUtils.RSS_LOCAL_DIR_KEY, "/path/a").execute(() -> {
      assertEquals(Collections.singletonList("/path/a"), RssUtils.getConfiguredLocalDirs(conf));
    });

    withEnvironmentVariable(RssUtils.RSS_LOCAL_DIR_KEY, "/path/a,/path/b").execute(() -> {
      assertEquals(Arrays.asList("/path/a", "/path/b"), RssUtils.getConfiguredLocalDirs(conf));
    });

    withEnvironmentVariable(RssUtils.RSS_LOCAL_DIR_KEY, null).execute(() -> {
      assertNull(RssUtils.getConfiguredLocalDirs(conf));
      conf.set(RssBaseConf.RSS_STORAGE_BASE_PATH, Arrays.asList("/path/a", "/path/b"));
      assertEquals(Arrays.asList("/path/a", "/path/b"), RssUtils.getConfiguredLocalDirs(conf));
    });
  }

  // Copy from ClientUtils
  private Long getBlockId(long partitionId, long taskAttemptId, long atomicInt) {
    return (atomicInt << (Constants.PARTITION_ID_MAX_LENGTH + Constants.TASK_ATTEMPT_ID_MAX_LENGTH))
        + (partitionId << Constants.TASK_ATTEMPT_ID_MAX_LENGTH) + taskAttemptId;
  }

  interface RssUtilTestDummy {
    String get();
  }

  public static class RssUtilTestDummyFailNotSub {
    public RssUtilTestDummyFailNotSub() {
    }
  }

  public static class RssUtilTestDummyNoConstructor implements RssUtilTestDummy {
    public RssUtilTestDummyNoConstructor(int a) {
    }

    public String get() {
      return null;
    }
  }

  public static class RssUtilTestDummySuccess implements RssUtilTestDummy {
    private final String s;

    public RssUtilTestDummySuccess(String s) {
      this.s = s;
    }

    public String get() {
      return s;
    }
  }


}
