/**
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

package org.apache.atlas.web.service;

import com.google.common.base.Charsets;
import org.apache.curator.framework.AuthInfo;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class AtlasZookeeperSecurityPropertiesTest {

    @Test
    public void shouldGetAcl() {
        ACL acl = AtlasZookeeperSecurityProperties.parseAcl("sasl:myclient@EXAMPLE.COM");
        assertEquals(acl.getId().getScheme(), "sasl");
        assertEquals(acl.getId().getId(), "myclient@EXAMPLE.COM");
        assertEquals(acl.getPerms(), ZooDefs.Perms.ALL);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionForNullAcl() {
        ACL acl = AtlasZookeeperSecurityProperties.parseAcl(null);
        fail("Should have thrown exception for null ACL string");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionForInvalidAclString() {
        ACL acl = AtlasZookeeperSecurityProperties.parseAcl("randomAcl");
        fail("Should have thrown exception for null ACL string");
    }

    @Test
    public void idsWithColonsAreValid() {
        ACL acl = AtlasZookeeperSecurityProperties.parseAcl("auth:user:password");
        assertEquals(acl.getId().getScheme(), "auth");
        assertEquals(acl.getId().getId(), "user:password");
    }

    @Test
    public void shouldGetAuth() {
        AuthInfo authInfo = AtlasZookeeperSecurityProperties.parseAuth("digest:user:password");
        assertEquals(authInfo.getScheme(), "digest");
        assertEquals(authInfo.getAuth(), "user:password".getBytes(Charsets.UTF_8));
    }

    @Test
    public void shouldReturnDefaultAclIfNullOrEmpty() {
        ACL acl = AtlasZookeeperSecurityProperties.parseAcl(null, ZooDefs.Ids.OPEN_ACL_UNSAFE.get(0));
        assertEquals(acl, ZooDefs.Ids.OPEN_ACL_UNSAFE.get(0));
    }
}
