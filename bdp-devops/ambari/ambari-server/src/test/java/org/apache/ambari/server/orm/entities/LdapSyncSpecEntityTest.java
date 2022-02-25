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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * LdapSyncSpecEntity tests.
 */
public class LdapSyncSpecEntityTest {
  @Test
  public void testGetPrincipalType() throws Exception {
    LdapSyncSpecEntity entity = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.USERS,
        LdapSyncSpecEntity.SyncType.ALL, Collections.emptyList(), false);
    Assert.assertEquals(LdapSyncSpecEntity.PrincipalType.USERS, entity.getPrincipalType());

    entity = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.GROUPS,
        LdapSyncSpecEntity.SyncType.ALL, Collections.emptyList(), false);
    Assert.assertEquals(LdapSyncSpecEntity.PrincipalType.GROUPS, entity.getPrincipalType());
  }

  @Test
  public void testGetSyncType() throws Exception {
    LdapSyncSpecEntity entity = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.USERS,
        LdapSyncSpecEntity.SyncType.ALL, Collections.emptyList(), false);
    Assert.assertEquals(LdapSyncSpecEntity.SyncType.ALL, entity.getSyncType());

    entity = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.USERS,
        LdapSyncSpecEntity.SyncType.EXISTING, Collections.emptyList(), false);
    Assert.assertEquals(LdapSyncSpecEntity.SyncType.EXISTING, entity.getSyncType());
  }

  @Test
  public void testGetPrincipalNames() throws Exception {
    List<String> names = new LinkedList<>();
    names.add("joe");
    names.add("fred");

    LdapSyncSpecEntity entity = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.USERS,
        LdapSyncSpecEntity.SyncType.SPECIFIC, names, false);
    Assert.assertEquals(names, entity.getPrincipalNames());
  }

  @Test
  public void testIllegalConstruction() throws Exception {
    try {
      new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.USERS,
          LdapSyncSpecEntity.SyncType.SPECIFIC, Collections.emptyList(), false);
      Assert.fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }

    List<String> names = new LinkedList<>();
    names.add("joe");
    names.add("fred");

    try {
      new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.USERS,
          LdapSyncSpecEntity.SyncType.ALL, names, false);
      Assert.fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.USERS,
          LdapSyncSpecEntity.SyncType.EXISTING, names, false);
      Assert.fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
}
