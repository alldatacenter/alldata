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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * ServiceConfigEntity unit tests.
 */
public class ServiceConfigEntityTest {
  @Test
  public void testSettersGetters() {
    ServiceConfigEntity entity = new ServiceConfigEntity();
    entity.setServiceName("foo");
    entity.setUser("bar");
    entity.setNote("note");
    entity.setVersion(1L);
    entity.setServiceConfigId(1L);
    entity.setClusterId(1L);
    entity.setCreateTimestamp(1111L);
    assertEquals("foo", entity.getServiceName());
    assertEquals("bar", entity.getUser());
    assertEquals("note", entity.getNote());
    assertEquals(Long.valueOf(1), entity.getVersion());
    assertEquals(Long.valueOf(1), entity.getServiceConfigId());
    assertEquals(Long.valueOf(1), entity.getClusterId());
    assertEquals(Long.valueOf(1111), entity.getCreateTimestamp());
  }

}
