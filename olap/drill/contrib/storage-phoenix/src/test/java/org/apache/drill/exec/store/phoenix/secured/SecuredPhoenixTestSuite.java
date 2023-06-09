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
package org.apache.drill.exec.store.phoenix.secured;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.store.phoenix.QueryServerBasicsIT;
import org.apache.drill.test.BaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.slf4j.LoggerFactory;

import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;


@Suite
@SelectClasses({
  SecuredPhoenixDataTypeTest.class,
  SecuredPhoenixSQLTest.class,
  SecuredPhoenixCommandTest.class
})
@Disabled
@Tag(SlowTest.TAG)
@Tag(RowSetTests.TAG)
public class SecuredPhoenixTestSuite extends BaseTest {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SecuredPhoenixTestSuite.class);

  private static volatile boolean runningSuite = false;
  private static final AtomicInteger initCount = new AtomicInteger(0);

  @BeforeAll
  public static void initPhoenixQueryServer() throws Exception {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    synchronized (SecuredPhoenixTestSuite.class) {
      if (initCount.get() == 0) {
        logger.info("Boot the test cluster...");
        HttpParamImpersonationQueryServerIT.startQueryServerEnvironment();
      }
      initCount.incrementAndGet();
      runningSuite = true;
    }
  }

  @AfterAll
  public static void tearDownCluster() throws Exception {
    synchronized (SecuredPhoenixTestSuite.class) {
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
