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

package org.apache.ambari.server.controller.spi;

import org.junit.Assert;
import org.junit.Test;

/**
 * Resource tests.
 */
public class ResourceTest {
  @Test
  public void testResource() {

    for (Resource.InternalType internalType : Resource.InternalType.values()) {
      Resource.Type type= null;
      try {
        type = Resource.Type.valueOf(internalType.name());
      } catch (IllegalArgumentException e) {
        Assert.fail("Resource.Type should be defined for internal type " + internalType.name() + ".");
      }
      Assert.assertEquals(type.name(), internalType.name());
      Assert.assertEquals(type.ordinal(), internalType.ordinal());
    }

    Resource.Type newType = new Resource.Type("newType");
    Resource.Type newType2 = new Resource.Type("newType2");

    Assert.assertFalse(newType.equals(newType2));
    Assert.assertFalse(newType2.equals(newType));

    Assert.assertEquals("newType", newType.name());

    Assert.assertFalse(newType.isInternalType());

    try {
      newType.getInternalType();
      Assert.fail("Can't get internal type for a extended resource.");
    } catch (UnsupportedOperationException e) {
      //Expected
    }

    Resource.Type t1 = Resource.Type.valueOf("newType");
    Resource.Type t2 = Resource.Type.valueOf("newType2");
    Assert.assertTrue(newType.equals(t1));
    Assert.assertTrue(t1.equals(newType));
    Assert.assertTrue(newType2.equals(t2));
    Assert.assertTrue(t2.equals(newType2));
    Assert.assertFalse(t1.equals(newType2));
    Assert.assertFalse(t2.equals(newType));

    try {
      Resource.Type.valueOf("badType");
      Assert.fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      //Expected
    }
  }
}
