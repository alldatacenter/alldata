/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.phoenix;

import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.categories.SlowTest;
import org.apache.drill.test.BaseTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.LoggerFactory;


@RunWith(Suite.class)
@SuiteClasses ({
   PhoenixDataTypeTest.class,
   PhoenixSQLTest.class,
   PhoenixCommandTest.class
})
@Ignore
@Category({ SlowTest.class })
public class PhoenixTestSuite extends BaseTest {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PhoenixTestSuite.class);

  private static volatile boolean runningSuite = false;
  private static AtomicInteger initCount = new AtomicInteger(0);

  @BeforeClass
  public static void initPhoenixQueryServer() throws Exception {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    synchronized (PhoenixTestSuite.class) {
      if (initCount.get() == 0) {
        logger.info("Boot the test cluster...");
        QueryServerBasicsIT.doSetup();
      }
      initCount.incrementAndGet();
      runningSuite = true;
    }
  }

  @AfterClass
  public static void tearDownCluster() throws Exception {
    synchronized (PhoenixTestSuite.class) {
      if (initCount.decrementAndGet() == 0) {
        logger.info("Shutdown all instances of test cluster.");
        QueryServerBasicsIT.afterClass();
      }
    }
  }

  public static boolean isRunningSuite() {
    return runningSuite;
  }
}
