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
package org.apache.drill.exec.vector.accessor;

import org.apache.drill.exec.vector.accessor.sql.TimePrintMillis;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

public class TestTimePrintMillis extends BaseTest {

  @Test
  public void testPrintingMillis() {
    // testing the regular case where the precision of the millisecond is 3
    TimePrintMillis time = new TimePrintMillis(999);
    Assert.assertTrue(time.toString().endsWith("999"));

    // test case where one leading zero needs to be added
    time = new TimePrintMillis(99);
    Assert.assertTrue(time.toString().endsWith("099"));

    // test case where two leading zeroes needs to be added
    time = new TimePrintMillis(1);
    Assert.assertTrue(time.toString().endsWith("001"));
  }
}
