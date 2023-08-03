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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HealthCheckTest {

  @BeforeAll
  public static void setup() {
    ShuffleServerMetrics.register();
  }

  @AfterAll
  public static void clear() {
    ShuffleServerMetrics.clear();
  }

  @Test
  public void buildInCheckerTest() {
    ShuffleServerConf conf = new ShuffleServerConf();
    assertConf(conf);
    conf.setString(ShuffleServerConf.HEALTH_CHECKER_CLASS_NAMES.key(), "");
    assertConf(conf);
    conf.setString(ShuffleServerConf.HEALTH_CHECKER_CLASS_NAMES.key(), LocalStorageChecker.class.getCanonicalName());
    conf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList("s1"));
    conf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.HDFS.name());
    assertConf(conf);
    conf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.LOCALFILE.name());
    conf.set(ShuffleServerConf.HEALTH_MIN_STORAGE_PERCENTAGE, -1.0);
    assertConf(conf);
    conf.set(ShuffleServerConf.HEALTH_MIN_STORAGE_PERCENTAGE, 102.0);
    assertConf(conf);
    conf.set(ShuffleServerConf.HEALTH_MIN_STORAGE_PERCENTAGE, 1.0);
    conf.set(ShuffleServerConf.HEALTH_STORAGE_MAX_USAGE_PERCENTAGE, -1.0);
    assertConf(conf);
    conf.set(ShuffleServerConf.HEALTH_STORAGE_MAX_USAGE_PERCENTAGE, 101.0);
    assertConf(conf);
    conf.set(ShuffleServerConf.HEALTH_STORAGE_MAX_USAGE_PERCENTAGE, 1.0);
    conf.set(ShuffleServerConf.HEALTH_STORAGE_RECOVERY_USAGE_PERCENTAGE, -1.0);
    assertConf(conf);
    conf.set(ShuffleServerConf.HEALTH_STORAGE_RECOVERY_USAGE_PERCENTAGE, 101.0);
    assertConf(conf);
    conf.set(ShuffleServerConf.HEALTH_STORAGE_RECOVERY_USAGE_PERCENTAGE, 1.0);
    new LocalStorageChecker(conf, Lists.newArrayList());
  }

  @Test
  public void checkTest() {
    AtomicBoolean healthy = new AtomicBoolean(false);
    ShuffleServerConf conf = new ShuffleServerConf();
    conf.setString(ShuffleServerConf.HEALTH_CHECKER_CLASS_NAMES.key(), HealthyMockChecker.class.getCanonicalName());
    HealthCheck checker = new HealthCheck(healthy, conf, Lists.newArrayList());
    checker.check();
    assertTrue(healthy.get());
    assertEquals(0, ShuffleServerMetrics.gaugeIsHealthy.get());

    conf.setString(ShuffleServerConf.HEALTH_CHECKER_CLASS_NAMES.key(), UnHealthyMockChecker.class.getCanonicalName());
    checker = new HealthCheck(healthy, conf, Lists.newArrayList());
    checker.check();
    assertFalse(healthy.get());
    assertEquals(1, ShuffleServerMetrics.gaugeIsHealthy.get());

    conf.setString(ShuffleServerConf.HEALTH_CHECKER_CLASS_NAMES.key(),
        UnHealthyMockChecker.class.getCanonicalName() + "," + HealthyMockChecker.class.getCanonicalName());
    checker = new HealthCheck(healthy, conf, Lists.newArrayList());
    checker.check();
    assertFalse(healthy.get());
    assertEquals(1, ShuffleServerMetrics.gaugeIsHealthy.get());
  }

  private void assertConf(ShuffleServerConf conf) {
    boolean isThrown;
    isThrown = false;
    try {
      new LocalStorageChecker(conf, Lists.newArrayList());
    } catch (IllegalArgumentException e) {
      isThrown = true;
    }
    assertTrue(isThrown);
  }
}
