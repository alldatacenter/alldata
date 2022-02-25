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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

/**
 * HostEntity tests.
 */
public class HostEntityTest {

  @Test
  public void testGetHostComponentDesiredStateEntities() throws Exception {
    HostEntity hostEntity = new HostEntity();
    HostComponentDesiredStateEntity stateEntity = new HostComponentDesiredStateEntity();

    hostEntity.setHostComponentDesiredStateEntities(new HashSet<>());

    Collection<HostComponentDesiredStateEntity> stateEntities = hostEntity.getHostComponentDesiredStateEntities();
    assertTrue(stateEntities.isEmpty());
    try {
      stateEntities.add(stateEntity);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testAddHostComponentDesiredStateEntity() throws Exception {
    HostEntity hostEntity = new HostEntity();
    HostComponentDesiredStateEntity stateEntity = new HostComponentDesiredStateEntity();

    hostEntity.setHostComponentDesiredStateEntities(new HashSet<>());

    Collection<HostComponentDesiredStateEntity> stateEntities = hostEntity.getHostComponentDesiredStateEntities();
    assertTrue(stateEntities.isEmpty());

    hostEntity.addHostComponentDesiredStateEntity(stateEntity);

    stateEntities = hostEntity.getHostComponentDesiredStateEntities();
    assertTrue(stateEntities.contains(stateEntity));
  }

  @Test
  public void testRemoveHostComponentDesiredStateEntity() throws Exception {
    HostEntity hostEntity = new HostEntity();
    HostComponentDesiredStateEntity stateEntity = new HostComponentDesiredStateEntity();

    hostEntity.setHostComponentDesiredStateEntities(new HashSet<>());

    Collection<HostComponentDesiredStateEntity> stateEntities = hostEntity.getHostComponentDesiredStateEntities();
    assertTrue(stateEntities.isEmpty());

    hostEntity.addHostComponentDesiredStateEntity(stateEntity);

    stateEntities = hostEntity.getHostComponentDesiredStateEntities();
    assertTrue(stateEntities.contains(stateEntity));

    hostEntity.removeHostComponentDesiredStateEntity(stateEntity);

    stateEntities = hostEntity.getHostComponentDesiredStateEntities();
    assertFalse(stateEntities.contains(stateEntity));
  }

  @Test
  public void testGetHostComponentStateEntities() throws Exception {
    HostEntity hostEntity = new HostEntity();
    HostComponentStateEntity stateEntity = new HostComponentStateEntity();

    hostEntity.setHostComponentStateEntities(new HashSet<>());

    Collection<HostComponentStateEntity> stateEntities = hostEntity.getHostComponentStateEntities();
    assertTrue(stateEntities.isEmpty());
    try {
      stateEntities.add(stateEntity);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }

  }

  @Test
  public void testAddHostComponentStateEntity() throws Exception {
    HostEntity hostEntity = new HostEntity();
    HostComponentStateEntity stateEntity = new HostComponentStateEntity();

    hostEntity.setHostComponentStateEntities(new HashSet<>());

    Collection<HostComponentStateEntity> stateEntities = hostEntity.getHostComponentStateEntities();
    assertTrue(stateEntities.isEmpty());

    hostEntity.addHostComponentStateEntity(stateEntity);

    stateEntities = hostEntity.getHostComponentStateEntities();
    assertTrue(stateEntities.contains(stateEntity));

  }

  @Test
  public void testRemoveHostComponentStateEntity() throws Exception {
    HostEntity hostEntity = new HostEntity();
    HostComponentStateEntity stateEntity = new HostComponentStateEntity();

    hostEntity.setHostComponentStateEntities(new HashSet<>());

    Collection<HostComponentStateEntity> stateEntities = hostEntity.getHostComponentStateEntities();
    assertTrue(stateEntities.isEmpty());

    hostEntity.addHostComponentStateEntity(stateEntity);

    stateEntities = hostEntity.getHostComponentStateEntities();
    assertTrue(stateEntities.contains(stateEntity));

    hostEntity.removeHostComponentStateEntity(stateEntity);

    stateEntities = hostEntity.getHostComponentStateEntities();
    assertFalse(stateEntities.contains(stateEntity));

  }
}
