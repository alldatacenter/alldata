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
package com.mapr.drill.maprdb.tests.binary;

import org.apache.drill.hbase.TestHBaseProjectPushDown;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

import com.mapr.drill.maprdb.tests.MaprDBTestsSuite;
import com.mapr.tests.annotations.ClusterTest;

/**
 * This class does not define any test method but includes all test methods
 * defined in the parent class, all of which are tested against MapRDB instead
 * of HBase.
 */
@Category(ClusterTest.class)
public class TestMapRDBProjectPushDown extends TestHBaseProjectPushDown {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    MaprDBTestsSuite.setupTests();
    conf = MaprDBTestsSuite.createPluginAndGetConf(getDrillbitContext());
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    MaprDBTestsSuite.cleanupTests();
  }
}
