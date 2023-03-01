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

package org.apache.spark.shuffle;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.internal.SQLConf;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.config.RssClientConf;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RssShuffleManagerTest {
  private static final String SPARK_ADAPTIVE_EXECUTION_ENABLED_KEY = "spark.sql.adaptive.enabled";

  @Test
  public void testGetDataDistributionType() {
    // case1
    SparkConf sparkConf = new SparkConf();
    sparkConf.set(SPARK_ADAPTIVE_EXECUTION_ENABLED_KEY, "true");
    assertEquals(
        ShuffleDataDistributionType.LOCAL_ORDER,
        RssShuffleManager.getDataDistributionType(sparkConf)
    );

    // case2
    sparkConf = new SparkConf();
    sparkConf.set(SPARK_ADAPTIVE_EXECUTION_ENABLED_KEY, "false");
    assertEquals(
        RssClientConf.DATA_DISTRIBUTION_TYPE.defaultValue(),
        RssShuffleManager.getDataDistributionType(sparkConf)
    );

    // case3
    sparkConf = new SparkConf();
    sparkConf.set(SPARK_ADAPTIVE_EXECUTION_ENABLED_KEY, "true");
    sparkConf.set("spark." + RssClientConf.DATA_DISTRIBUTION_TYPE.key(), ShuffleDataDistributionType.NORMAL.name());
    assertEquals(
        ShuffleDataDistributionType.NORMAL,
        RssShuffleManager.getDataDistributionType(sparkConf)
    );

    // case4
    sparkConf = new SparkConf();
    sparkConf.set(SPARK_ADAPTIVE_EXECUTION_ENABLED_KEY, "true");
    sparkConf.set(
        "spark." + RssClientConf.DATA_DISTRIBUTION_TYPE.key(),
        ShuffleDataDistributionType.LOCAL_ORDER.name()
    );
    assertEquals(
        ShuffleDataDistributionType.LOCAL_ORDER,
        RssShuffleManager.getDataDistributionType(sparkConf)
    );

    // case5
    sparkConf = new SparkConf();
    boolean aqeEnable = (boolean) sparkConf.get(SQLConf.ADAPTIVE_EXECUTION_ENABLED());
    if (aqeEnable) {
      assertEquals(
          ShuffleDataDistributionType.LOCAL_ORDER,
          RssShuffleManager.getDataDistributionType(sparkConf)
      );
    } else {
      assertEquals(
          RssClientConf.DATA_DISTRIBUTION_TYPE.defaultValue(),
          RssShuffleManager.getDataDistributionType(sparkConf)
      );
    }
  }
}
