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
package org.apache.drill.exec.hive;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.drill.PlanTestBase;
import org.apache.drill.exec.store.hive.HiveTestDataGenerator;
import org.apache.drill.test.BaseDirTestWatcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.Description;

/**
 * Base class for Hive test. Takes care of generating and adding Hive test plugin before tests and deleting the
 * plugin after tests.
 */
public class HiveTestBase extends PlanTestBase {

  public static final HiveTestFixture HIVE_TEST_FIXTURE;

  static {
    if (HiveTestUtilities.supportedJavaVersion()) {
      // generate hive data common for all test classes using own dirWatcher
      BaseDirTestWatcher generalDirWatcher = new BaseDirTestWatcher() {
        {
        /*
           Below protected method invoked to create directory DirWatcher.dir with path like:
           ./target/org.apache.drill.exec.hive.HiveTestBase123e4567-e89b-12d3-a456-556642440000.
           Then subdirectory with name 'root' will be used to hold metastore_db and other data shared between
           all derivatives of the class. Note that UUID suffix is necessary to avoid conflicts between forked JVMs.
        */
          starting(Description.createSuiteDescription(HiveTestBase.class.getName().concat(UUID.randomUUID().toString())));
        }
      };
      File baseDir = generalDirWatcher.getRootDir();
      HIVE_TEST_FIXTURE = HiveTestFixture.builder(baseDir).build();
      HiveTestDataGenerator dataGenerator = new HiveTestDataGenerator(generalDirWatcher, baseDir,
          HIVE_TEST_FIXTURE.getWarehouseDir());
      HIVE_TEST_FIXTURE.getDriverManager().runWithinSession(dataGenerator::generateData);

      // set hook for clearing watcher's dir on JVM shutdown
      Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(generalDirWatcher.getDir())));
    } else {
      HIVE_TEST_FIXTURE = null;
    }
  }

  @BeforeClass
  public static void setUp() {
    HiveTestUtilities.assumeJavaVersion();
    HIVE_TEST_FIXTURE.getPluginManager().addHivePluginTo(bits);
  }

  @AfterClass
  public static void tearDown() {
    if (HIVE_TEST_FIXTURE != null) {
      HIVE_TEST_FIXTURE.getPluginManager().removeHivePluginFrom(bits);
    }
  }
}
