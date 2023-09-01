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
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.uniffle.storage.common.LocalStorage;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StorageCheckerTest {

  private int callTimes = 0;

  @BeforeAll
  public static void setup() {
    ShuffleServerMetrics.register();
  }

  @AfterAll
  public static void clear() {
    ShuffleServerMetrics.clear();
  }

  @Test
  public void checkTest(@TempDir File baseDir) throws Exception {
    ShuffleServerConf conf = new ShuffleServerConf();
    conf.setBoolean(ShuffleServerConf.HEALTH_CHECK_ENABLE, true);
    conf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.LOCALFILE.name());
    String st1 = new File(baseDir, "st1").getPath();
    String st2 = new File(baseDir, "st2").getPath();
    String st3 = new File(baseDir, "st3").getPath();
    conf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList(st1, st2, st3));
    conf.set(ShuffleServerConf.HEALTH_MIN_STORAGE_PERCENTAGE, 55.0);
    List<LocalStorage> storages = Lists.newArrayList();
    storages.add(LocalStorage.newBuilder().basePath(st1).build());
    storages.add(LocalStorage.newBuilder().basePath(st2).build());
    storages.add(LocalStorage.newBuilder().basePath(st3).build());
    LocalStorageChecker checker = new MockStorageChecker(conf, storages);

    assertTrue(checker.checkIsHealthy());
    assertEquals(3000, ShuffleServerMetrics.gaugeLocalStorageTotalSpace.get());
    assertEquals(600, ShuffleServerMetrics.gaugeLocalStorageUsedSpace.get());
    assertEquals(0.2, ShuffleServerMetrics.gaugeLocalStorageUsedSpaceRatio.get());
    assertEquals(3, ShuffleServerMetrics.gaugeLocalStorageTotalDirsNum.get());
    assertEquals(0, ShuffleServerMetrics.gaugeLocalStorageCorruptedDirsNum.get());

    callTimes++;
    assertTrue(checker.checkIsHealthy());
    assertEquals(3000, ShuffleServerMetrics.gaugeLocalStorageTotalSpace.get());
    assertEquals(1400, ShuffleServerMetrics.gaugeLocalStorageUsedSpace.get());
    assertEquals(3, ShuffleServerMetrics.gaugeLocalStorageTotalDirsNum.get());
    assertEquals(0, ShuffleServerMetrics.gaugeLocalStorageCorruptedDirsNum.get());

    callTimes++;
    assertFalse(checker.checkIsHealthy());
    assertEquals(3000, ShuffleServerMetrics.gaugeLocalStorageTotalSpace.get());
    assertEquals(2100, ShuffleServerMetrics.gaugeLocalStorageUsedSpace.get());
    assertEquals(3, ShuffleServerMetrics.gaugeLocalStorageTotalDirsNum.get());
    assertEquals(0, ShuffleServerMetrics.gaugeLocalStorageCorruptedDirsNum.get());

    callTimes++;
    assertTrue(checker.checkIsHealthy());
    conf.set(ShuffleServerConf.HEALTH_MIN_STORAGE_PERCENTAGE, 80.0);
    checker = new MockStorageChecker(conf, storages);
    assertFalse(checker.checkIsHealthy());
    assertEquals(3000, ShuffleServerMetrics.gaugeLocalStorageTotalSpace.get());
    assertEquals(1600, ShuffleServerMetrics.gaugeLocalStorageUsedSpace.get());
    assertEquals(3, ShuffleServerMetrics.gaugeLocalStorageTotalDirsNum.get());
    assertEquals(0, ShuffleServerMetrics.gaugeLocalStorageCorruptedDirsNum.get());

    callTimes++;
    checker.checkIsHealthy();
    assertTrue(checker.checkIsHealthy());
    assertEquals(3000, ShuffleServerMetrics.gaugeLocalStorageTotalSpace.get());
    assertEquals(250, ShuffleServerMetrics.gaugeLocalStorageUsedSpace.get());
    assertEquals(3, ShuffleServerMetrics.gaugeLocalStorageTotalDirsNum.get());
    assertEquals(0, ShuffleServerMetrics.gaugeLocalStorageCorruptedDirsNum.get());
  }

  private class MockStorageChecker extends LocalStorageChecker {
    MockStorageChecker(ShuffleServerConf conf, List<LocalStorage> storages) {
      super(conf, storages);
    }

    @Override
    long getTotalSpace(File file) {
      return 1000;
    }

    // we mock this method, and will return different values according
    // to call times.
    @Override
    long getUsedSpace(File file) {
      long result = 0;
      switch (file.getName()) {
        case "st1":
          switch (callTimes) {
            case 0:
              result = 100;
              break;
            case 1:
            case 2:
            case 3:
              result = 900;
              break;
            case 4:
              result = 150;
              break;
            default:
              break;
          }
          break;
        case "st2":
          switch (callTimes) {
            case 0:
            case 1:
              result = 200;
              break;
            case 2:
              result = 900;
              break;
            case 3:
              result = 400;
              break;
            case 4:
              result = 100;
              break;
            default:
              break;
          }
          break;
        case "st3":
          switch (callTimes) {
            case 0:
            case 1:
            case 2:
            case 3:
              result = 300;
              break;
            default:
              break;
          }
          break;
        default:
          // ignore
      }
      return result;
    }
  }
}
