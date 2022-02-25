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

import org.junit.Assert;
import org.junit.Test;

/**
 * ViewEntityEntity tests.
 */
public class ViewEntityEntityTest {
  @Test
  public void testSetGetId() throws Exception {
    ViewEntityEntity viewEntityEntity = new ViewEntityEntity();
    viewEntityEntity.setId(99L);
    Assert.assertEquals(99L, (long) viewEntityEntity.getId());
  }

  @Test
  public void testSetGetViewName() throws Exception {
    ViewEntityEntity viewEntityEntity = new ViewEntityEntity();
    viewEntityEntity.setViewName("foo");
    Assert.assertEquals("foo", viewEntityEntity.getViewName());
  }

  @Test
  public void testSetGetViewInstanceName() throws Exception {
    ViewEntityEntity viewEntityEntity = new ViewEntityEntity();
    viewEntityEntity.setViewInstanceName("foo");
    Assert.assertEquals("foo", viewEntityEntity.getViewInstanceName());
  }

  @Test
  public void testSetGetClassName() throws Exception {
    ViewEntityEntity viewEntityEntity = new ViewEntityEntity();
    viewEntityEntity.setClassName("TestClass");
    Assert.assertEquals("TestClass", viewEntityEntity.getClassName());
  }


  @Test
  public void testSetGetIdProperty() throws Exception {
    ViewEntityEntity viewEntityEntity = new ViewEntityEntity();
    viewEntityEntity.setIdProperty("id");
    Assert.assertEquals("id", viewEntityEntity.getIdProperty());
  }

  @Test
  public void testSetGetViewInstance() throws Exception {
    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();
    ViewEntityEntity viewEntityEntity = new ViewEntityEntity();
    viewEntityEntity.setViewInstance(viewInstanceEntity);
    Assert.assertEquals(viewInstanceEntity, viewEntityEntity.getViewInstance());
  }

  @Test
  public void testEquals() throws Exception {
    ViewEntityEntity viewEntityEntity = new ViewEntityEntity();
    viewEntityEntity.setId(99L);
    viewEntityEntity.setClassName("TestClass");
    viewEntityEntity.setIdProperty("id");
    viewEntityEntity.setViewName("foo");
    viewEntityEntity.setViewInstanceName("bar");

    ViewEntityEntity viewEntityEntity2 = new ViewEntityEntity();
    viewEntityEntity2.setId(99L);
    viewEntityEntity2.setClassName("TestClass");
    viewEntityEntity2.setIdProperty("id");
    viewEntityEntity2.setViewName("foo");
    viewEntityEntity2.setViewInstanceName("bar");

    Assert.assertTrue(viewEntityEntity.equals(viewEntityEntity2));

    viewEntityEntity2.setId(100L);

    Assert.assertFalse(viewEntityEntity.equals(viewEntityEntity2));
  }

  @Test
  public void testHashCode() throws Exception {
    ViewEntityEntity viewEntityEntity = new ViewEntityEntity();
    viewEntityEntity.setId(99L);
    viewEntityEntity.setClassName("TestClass");
    viewEntityEntity.setIdProperty("id");
    viewEntityEntity.setViewName("foo");
    viewEntityEntity.setViewInstanceName("bar");

    ViewEntityEntity viewEntityEntity2 = new ViewEntityEntity();
    viewEntityEntity2.setId(99L);
    viewEntityEntity2.setClassName("TestClass");
    viewEntityEntity2.setIdProperty("id");
    viewEntityEntity2.setViewName("foo");
    viewEntityEntity2.setViewInstanceName("bar");

    Assert.assertEquals(viewEntityEntity.hashCode(), viewEntityEntity2.hashCode());
  }
}
