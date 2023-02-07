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
package org.apache.drill.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

public class SubOperatorTest extends DrillTest {

  protected static OperatorFixture fixture;

  @ClassRule
  public static final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @BeforeClass
  public static void classSetup() throws Exception {
    fixture = OperatorFixture.standardFixture(dirTestWatcher);
    fixture.dirtyMemory(10);
  }

  @AfterClass
  public static void classTeardown() throws Exception {
    fixture.close();
  }
}
