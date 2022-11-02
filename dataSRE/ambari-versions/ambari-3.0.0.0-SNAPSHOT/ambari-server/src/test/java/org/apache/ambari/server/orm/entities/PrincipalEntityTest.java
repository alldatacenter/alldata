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

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * PrincipalEntity tests.
 */
public class PrincipalEntityTest {
  @Test
  public void testSetGetId() throws Exception {
    PrincipalEntity entity = new PrincipalEntity();

    entity.setId(1L);
    Assert.assertEquals(1L, (long) entity.getId());

    entity.setId(99L);
    Assert.assertEquals(99L, (long) entity.getId());
  }

  @Test
  public void testSetGetPrincipalType() throws Exception {
    PrincipalEntity entity = new PrincipalEntity();
    PrincipalTypeEntity typeEntity = new PrincipalTypeEntity();

    entity.setPrincipalType(typeEntity);
    Assert.assertEquals(typeEntity, entity.getPrincipalType());
  }

  @Test
  public void testSetGetPrivileges() throws Exception {

    Set<PrivilegeEntity> privileges = new HashSet<>();
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setId(1);
    privileges.add(privilegeEntity);
    privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setId(2);
    privileges.add(privilegeEntity);

    PrincipalEntity entity = new PrincipalEntity();

    entity.setPrivileges(privileges);
    Assert.assertEquals(privileges, entity.getPrivileges());
  }

  @Test
  public void testRemovePrivilege() throws Exception {

    Set<PrivilegeEntity> privileges = new HashSet<>();
    PrivilegeEntity privilegeEntity1 = new PrivilegeEntity();
    privilegeEntity1.setId(1);
    privileges.add(privilegeEntity1);
    PrivilegeEntity privilegeEntity2 = new PrivilegeEntity();
    privilegeEntity2.setId(2);
    privileges.add(privilegeEntity2);

    PrincipalEntity entity = new PrincipalEntity();

    entity.setPrivileges(privileges);

    entity.removePrivilege(privilegeEntity2);

    privileges = entity.getPrivileges();
    Assert.assertEquals(1, privileges.size());

    privileges.contains(privilegeEntity1);
  }
}
