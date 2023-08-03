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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.config.RssBaseConf;
import org.apache.uniffle.common.config.RssConf;
import org.apache.uniffle.common.exception.RssException;

public class RssUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(RssUtils.class);
  public static final String RSS_LOCAL_DIR_KEY = "RSS_LOCAL_DIRS";

  private RssUtils() {
  }

  /**
   * Load properties present in the given file.
   */
  public static Map<String, String> getPropertiesFromFile(String filename) {
    if (filename == null) {
      String rssHome = System.getenv("RSS_HOME");
      if (rssHome == null) {
        LOGGER.error("Both conf file and RSS_HOME env is null");
        return null;
      }

      LOGGER.info("Conf file is null use {}'s server.conf", rssHome);
      filename = rssHome + "/server.conf";
    }

    File file = new File(filename);

    if (!file.exists()) {
      LOGGER.error("Properties file " + filename + " does not exist");
      return null;
    }

    if (!file.isFile()) {
      LOGGER.error("Properties file " + filename + " is not a normal file");
      return null;
    }

    LOGGER.info("Load config from {}", filename);
    final Map<String, String> result = new HashMap<>();

    try (InputStreamReader inReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      Properties properties = new Properties();
      properties.load(inReader);
      properties.stringPropertyNames().forEach(k -> result.put(k, properties.getProperty(k).trim()));
    } catch (IOException ignored) {
      LOGGER.error("Failed when loading rss properties from " + filename);
    }

    return result;
  }

  // `InetAddress.getLocalHost().getHostAddress()` could return
  // 127.0.0.1 (127.0.1.1 on Debian). To avoid this situation, we can get current
  // ip through network interface (filtered ipv6, loop back, etc.).
  // If the network interface in the machine is more than one, we will choose the first IP.
  public static String getHostIp() throws Exception {
    // For K8S, there are too many IPs, it's hard to decide which we should use.
    // So we use the environment variable to tell RSS to use which one.
    String ip = System.getenv("RSS_IP");
    if (ip != null) {
      if (!InetAddresses.isInetAddress(ip)) {
        throw new RuntimeException("Environment RSS_IP: " + ip + " is wrong format");
      }
      return ip;
    }
    Enumeration<NetworkInterface> nif = NetworkInterface.getNetworkInterfaces();
    String siteLocalAddress = null;
    while (nif.hasMoreElements()) {
      NetworkInterface ni = nif.nextElement();
      if (!ni.isUp() || ni.isLoopback() || ni.isPointToPoint() || ni.isVirtual()) {
        continue;
      }
      for (InterfaceAddress ifa : ni.getInterfaceAddresses()) {
        InetAddress ia = ifa.getAddress();
        InetAddress brd = ifa.getBroadcast();
        if (brd == null || brd.isAnyLocalAddress()) {
          LOGGER.info("ip {} was filtered, because it don't have effective broadcast address", ia.getHostAddress());
          continue;
        }
        if (!ia.isLinkLocalAddress() && !ia.isAnyLocalAddress() && !ia.isLoopbackAddress()
            && ia instanceof Inet4Address && ia.isReachable(5000)) {
          if (!ia.isSiteLocalAddress()) {
            return ia.getHostAddress();
          } else if (siteLocalAddress == null) {
            LOGGER.info("ip {} was candidate, if there is no better choice, we will choose it", ia.getHostAddress());
            siteLocalAddress = ia.getHostAddress();
          } else {
            LOGGER.info("ip {} was filtered, because it's not first effect site local address", ia.getHostAddress());
          }
        } else if (!(ia instanceof Inet4Address)) {
          LOGGER.info("ip {} was filtered, because it's just a ipv6 address", ia.getHostAddress());
        } else if (ia.isLinkLocalAddress()) {
          LOGGER.info("ip {} was filtered, because it's just a link local address", ia.getHostAddress());
        } else if (ia.isAnyLocalAddress()) {
          LOGGER.info("ip {} was filtered, because it's just a any local address", ia.getHostAddress());
        } else if (ia.isLoopbackAddress()) {
          LOGGER.info("ip {} was filtered, because it's just a loop back address", ia.getHostAddress());
        } else {
          LOGGER.info("ip {} was filtered, because it's just not reachable address", ia.getHostAddress());
        }
      }
    }
    return siteLocalAddress;
  }

  public static byte[] serializeBitMap(Roaring64NavigableMap bitmap) throws IOException {
    long size = bitmap.serializedSizeInBytes();
    if (size > Integer.MAX_VALUE) {
      throw new RuntimeException("Unsupported serialized size of bitmap: " + size);
    }
    ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream((int) size);
    DataOutputStream dataOutputStream = new DataOutputStream(arrayOutputStream);
    bitmap.serialize(dataOutputStream);
    return arrayOutputStream.toByteArray();
  }

  public static Roaring64NavigableMap deserializeBitMap(byte[] bytes) throws IOException {
    Roaring64NavigableMap bitmap = Roaring64NavigableMap.bitmapOf();
    if (bytes.length == 0) {
      return bitmap;
    }
    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
    bitmap.deserialize(dataInputStream);
    return bitmap;
  }

  public static Roaring64NavigableMap cloneBitMap(Roaring64NavigableMap bitmap) {
    Roaring64NavigableMap clone = Roaring64NavigableMap.bitmapOf();
    clone.or(bitmap);
    return clone;
  }

  public static String generateShuffleKey(String appId, int shuffleId) {
    return String.join(Constants.KEY_SPLIT_CHAR, appId, String.valueOf(shuffleId));
  }

  public static String generatePartitionKey(String appId, Integer shuffleId, Integer partition) {
    return String.join(Constants.KEY_SPLIT_CHAR, appId, String.valueOf(shuffleId), String.valueOf(partition));
  }

  public static <T> List<T> loadExtensions(Class<T> extClass, List<String> classes, Object obj) {
    if (classes == null || classes.isEmpty()) {
      throw new RuntimeException("Empty classes");
    }

    List<T> extensions = Lists.newArrayList();
    for (String name : classes) {
      name = name.trim();
      try {
        Class<?> klass = Class.forName(name);
        if (!extClass.isAssignableFrom(klass)) {
          throw new RuntimeException(name + " is not subclass of " + extClass.getName());
        }

        Constructor<?> constructor;
        T instance;
        try {
          constructor = klass.getConstructor(obj.getClass());
          instance = (T) constructor.newInstance(obj);
        } catch (Exception e) {
          LOGGER.error("Fail to new instance.", e);
          instance = (T) klass.getConstructor().newInstance();
        }
        extensions.add(instance);
      } catch (Exception e) {
        LOGGER.error("Fail to new instance using default constructor.", e);
        throw new RuntimeException(e);
      }
    }
    return extensions;
  }

  public static void checkQuorumSetting(int replica, int replicaWrite, int replicaRead) {
    if (replica < 1 || replicaWrite > replica || replicaRead > replica) {
      throw new RuntimeException("Replica config is invalid, recommend replica.write + replica.read > replica");
    }
    if (replicaWrite + replicaRead <= replica) {
      throw new RuntimeException("Replica config is unsafe, recommend replica.write + replica.read > replica");
    }
  }

  // the method is used to transfer hostname to metric name.
  // With standard of hostname, `-` and `.` will be included in hostname,
  // but they are not valid for metric name, replace them with '_'
  public static String getMetricNameForHostName(String hostName) {
    if (hostName == null) {
      return "";
    }
    return hostName.replaceAll("[\\.-]", "_");
  }

  public static Map<Integer, Roaring64NavigableMap> generatePartitionToBitmap(
      Roaring64NavigableMap shuffleBitmap, int startPartition, int endPartition) {
    Map<Integer, Roaring64NavigableMap> result = Maps.newHashMap();
    for (int partitionId = startPartition; partitionId < endPartition; partitionId++) {
      result.putIfAbsent(partitionId, Roaring64NavigableMap.bitmapOf());
    }
    Iterator<Long> it = shuffleBitmap.iterator();
    long mask = (1L << Constants.PARTITION_ID_MAX_LENGTH) - 1;
    while (it.hasNext()) {
      Long blockId = it.next();
      int partitionId = Math.toIntExact((blockId >> Constants.TASK_ATTEMPT_ID_MAX_LENGTH) & mask);
      if (partitionId >= startPartition && partitionId < endPartition) {
        result.get(partitionId).add(blockId);
      }
    }
    return result;
  }

  public static Map<ShuffleServerInfo, Set<Integer>> generateServerToPartitions(
      Map<Integer, List<ShuffleServerInfo>> partitionToServers) {
    Map<ShuffleServerInfo, Set<Integer>> serverToPartitions = Maps.newHashMap();
    for (Map.Entry<Integer, List<ShuffleServerInfo>> entry : partitionToServers.entrySet()) {
      int partitionId = entry.getKey();
      for (ShuffleServerInfo serverInfo : entry.getValue()) {
        if (!serverToPartitions.containsKey(serverInfo)) {
          serverToPartitions.put(serverInfo, Sets.newHashSet(partitionId));
        } else {
          serverToPartitions.get(serverInfo).add(partitionId);
        }
      }
    }
    return serverToPartitions;
  }

  public static void checkProcessedBlockIds(Roaring64NavigableMap blockIdBitmap,
                                            Roaring64NavigableMap processedBlockIds) {
    Roaring64NavigableMap cloneBitmap;
    cloneBitmap = RssUtils.cloneBitMap(blockIdBitmap);
    cloneBitmap.and(processedBlockIds);
    if (!blockIdBitmap.equals(cloneBitmap)) {
      throw new RssException("Blocks read inconsistent: expected " + blockIdBitmap.getLongCardinality()
          + " blocks, actual " + cloneBitmap.getLongCardinality() + " blocks");
    }
  }
  
  public static Roaring64NavigableMap generateTaskIdBitMap(Roaring64NavigableMap blockIdBitmap, IdHelper idHelper) {
    Iterator<Long> iterator = blockIdBitmap.iterator();
    Roaring64NavigableMap taskIdBitmap = Roaring64NavigableMap.bitmapOf();
    while (iterator.hasNext()) {
      taskIdBitmap.addLong(idHelper.getTaskAttemptId(iterator.next()));
    }
    return taskIdBitmap;
  }

  public static List<String> getConfiguredLocalDirs(RssConf conf) {
    if (conf.getEnv(RSS_LOCAL_DIR_KEY) != null) {
      return Arrays.asList(conf.getEnv(RSS_LOCAL_DIR_KEY).split(","));
    } else {
      return conf.get(RssBaseConf.RSS_STORAGE_BASE_PATH);
    }
  }
}
