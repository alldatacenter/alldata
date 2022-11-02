/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.orm.entities;

import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests methods on {@link AlertCurrentEntity}.
 */
public class AlertCurrentEntityTest {

  /**
   * Tests {@link AlertCurrentEntity#hashCode()} and {@link AlertCurrentEntity#equals(Object)}
   */
  @Test
  public void testHashCodeAndEquals(){
    AlertHistoryEntity history1 = new AlertHistoryEntity();
    AlertHistoryEntity history2 = new AlertHistoryEntity();
    history1.setAlertId(1L);
    history2.setAlertId(2L);

    AlertCurrentEntity current1 = new AlertCurrentEntity();
    AlertCurrentEntity current2 = new AlertCurrentEntity();

    Assert.assertEquals(current1.hashCode(), current2.hashCode());
    Assert.assertTrue(Objects.equals(current1, current2));

    current1.setAlertHistory(history1);
    current2.setAlertHistory(history2);
    Assert.assertNotSame(current1.hashCode(), current2.hashCode());
    Assert.assertFalse(Objects.equals(current1, current2));

    current2.setAlertHistory(history1);
    Assert.assertEquals(current1.hashCode(), current2.hashCode());
    Assert.assertTrue(Objects.equals(current1, current2));

    current1.setAlertId(1L);
    current2.setAlertId(2L);
    Assert.assertNotSame(current1.hashCode(), current2.hashCode());
    Assert.assertFalse(Objects.equals(current1, current2));

    current2.setAlertId(1L);
    Assert.assertEquals(current1.hashCode(), current2.hashCode());
    Assert.assertTrue(Objects.equals(current1, current2));
  }
}
