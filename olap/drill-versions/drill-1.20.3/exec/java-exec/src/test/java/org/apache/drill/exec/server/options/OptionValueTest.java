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
package org.apache.drill.exec.server.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.test.BaseTest;
import org.junit.Test;

public class OptionValueTest extends BaseTest {

  @Test
  public void createBooleanKindTest() {
    final OptionValue createdValue = OptionValue.create(
      OptionValue.Kind.BOOLEAN, OptionValue.AccessibleScopes.ALL,
      "myOption", "true", OptionValue.OptionScope.SYSTEM);

    final OptionValue expectedValue = OptionValue.create(
      OptionValue.AccessibleScopes.ALL, "myOption", true, OptionValue.OptionScope.SYSTEM);

    assertEquals(expectedValue, createdValue);
  }

  @Test
  public void createDoubleKindTest() {
    final OptionValue createdValue = OptionValue.create(
      OptionValue.Kind.DOUBLE, OptionValue.AccessibleScopes.ALL,
      "myOption", "1.5", OptionValue.OptionScope.SYSTEM);

    final OptionValue expectedValue = OptionValue.create(
      OptionValue.AccessibleScopes.ALL, "myOption", 1.5, OptionValue.OptionScope.SYSTEM);

    assertEquals(expectedValue, createdValue);

    try {
      OptionValue.create(
          OptionValue.Kind.DOUBLE, OptionValue.AccessibleScopes.ALL,
          "myOption", "bogus", OptionValue.OptionScope.SYSTEM);
      fail();
    } catch (UserException e) {
      // Expected
    }
  }

  @Test
  public void createLongKindTest() {
    final OptionValue createdValue = OptionValue.create(
      OptionValue.Kind.LONG, OptionValue.AccessibleScopes.ALL,
      "myOption", "3000", OptionValue.OptionScope.SYSTEM);

    final OptionValue expectedValue = OptionValue.create(
      OptionValue.AccessibleScopes.ALL, "myOption", 3000l, OptionValue.OptionScope.SYSTEM);

    assertEquals(expectedValue, createdValue);

    try {
      OptionValue.create(
          OptionValue.Kind.LONG, OptionValue.AccessibleScopes.ALL,
          "myOption", "bogus", OptionValue.OptionScope.SYSTEM);
      fail();
    } catch (UserException e) {
      // Expected
    }
  }

  @Test
  public void createStringKindTest() {
    final OptionValue createdValue = OptionValue.create(
      OptionValue.Kind.STRING, OptionValue.AccessibleScopes.ALL,
      "myOption", "wabalubawubdub", OptionValue.OptionScope.SYSTEM);

    final OptionValue expectedValue = OptionValue.create(
      OptionValue.AccessibleScopes.ALL, "myOption", "wabalubawubdub", OptionValue.OptionScope.SYSTEM);

    assertEquals(expectedValue, createdValue);
  }
}
