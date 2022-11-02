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
 * PrincipalTypeEntity tests.
 */
public class PrincipalTypeEntityTest {
  @Test
  public void testSetGetId() throws Exception {
    PrincipalTypeEntity principalTypeEntity = new PrincipalTypeEntity();

    principalTypeEntity.setId(1);
    Assert.assertEquals(1L, (long) principalTypeEntity.getId());

    principalTypeEntity.setId(99);
    Assert.assertEquals(99L, (long) principalTypeEntity.getId());
  }

  @Test
  public void testSetGetName() throws Exception {
    PrincipalTypeEntity principalTypeEntity = new PrincipalTypeEntity();

    principalTypeEntity.setName("foo");
    Assert.assertEquals("foo", principalTypeEntity.getName());

    principalTypeEntity.setName("bar");
    Assert.assertEquals("bar", principalTypeEntity.getName());
  }
}
