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
package org.apache.ambari.server.security.authentication.kerberos;

import static org.easymock.EasyMock.expect;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.tproxy.AmbariTProxyConfiguration;
import org.apache.ambari.server.security.authorization.Users;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

public class AmbariProxiedUserDetailsServiceTest extends EasyMockSupport {
  @Test
  public void testValidateHost() throws UnknownHostException {
    AmbariProxiedUserDetailsService service = createMockBuilder(AmbariProxiedUserDetailsService.class)
        .withConstructor(createNiceMock(Configuration.class), createNiceMock(Users.class))
        .addMockedMethod("getIpAddress", String.class)
        .createMock();
    expect(service.getIpAddress("host1.example.com")).andReturn("192.168.74.101").anyTimes();
    expect(service.getIpAddress("host2.example.com")).andReturn("192.168.74.102").anyTimes();

    AmbariTProxyConfiguration tproxyConfigration = createMock(AmbariTProxyConfiguration.class);
    expect(tproxyConfigration.getAllowedHosts("proxyUser")).andReturn("*").once();
    expect(tproxyConfigration.getAllowedHosts("proxyUser")).andReturn("192.168.74.101").once();
    expect(tproxyConfigration.getAllowedHosts("proxyUser")).andReturn("host1.example.com").once();
    expect(tproxyConfigration.getAllowedHosts("proxyUser")).andReturn("192.168.74.0/24").once();
    expect(tproxyConfigration.getAllowedHosts("proxyUser")).andReturn(null).once();
    expect(tproxyConfigration.getAllowedHosts("proxyUser")).andReturn("").once();
    expect(tproxyConfigration.getAllowedHosts("proxyUser")).andReturn("192.168.74.102").once();
    expect(tproxyConfigration.getAllowedHosts("proxyUser")).andReturn("host2.example.com").once();
    expect(tproxyConfigration.getAllowedHosts("proxyUser")).andReturn("192.168.74.1/32").once();

    replayAll();

    // ambari.tproxy.proxyuser.proxyUser.users = "*"
    Assert.assertTrue(service.validateHost(tproxyConfigration, "proxyUser", "192.168.74.101"));

    // ambari.tproxy.proxyuser.proxyUser.users = "192.168.74.101"
    Assert.assertTrue(service.validateHost(tproxyConfigration, "proxyUser", "192.168.74.101"));

    // ambari.tproxy.proxyuser.proxyUser.users = "host1.example.com"
    Assert.assertTrue(service.validateHost(tproxyConfigration, "proxyUser", "192.168.74.101"));

    // ambari.tproxy.proxyuser.proxyUser.users = "192.168.74.0/24"
    Assert.assertTrue(service.validateHost(tproxyConfigration, "proxyUser", "192.168.74.101"));

    // ambari.tproxy.proxyuser.proxyUser.users = null
    Assert.assertFalse(service.validateHost(tproxyConfigration, "proxyUser", "192.168.74.101"));

    // ambari.tproxy.proxyuser.proxyUser.users = ""
    Assert.assertFalse(service.validateHost(tproxyConfigration, "proxyUser", "192.168.74.101"));

    // ambari.tproxy.proxyuser.proxyUser.users = "192.168.74.102"
    Assert.assertFalse(service.validateHost(tproxyConfigration, "proxyUser", "192.168.74.101"));

    // ambari.tproxy.proxyuser.proxyUser.users = "host1.example.com"
    Assert.assertFalse(service.validateHost(tproxyConfigration, "proxyUser", "192.168.74.101"));

    // ambari.tproxy.proxyuser.proxyUser.users = "192.168.74.1/32"
    Assert.assertFalse(service.validateHost(tproxyConfigration, "proxyUser", "192.168.74.101"));

    verifyAll();
  }

  @Test
  public void testValidateUser() {
    AmbariProxiedUserDetailsService service = new AmbariProxiedUserDetailsService(createNiceMock(Configuration.class), createNiceMock(Users.class));

    AmbariTProxyConfiguration tproxyConfigration = createMock(AmbariTProxyConfiguration.class);
    expect(tproxyConfigration.getAllowedUsers("proxyUser")).andReturn("*").once();
    expect(tproxyConfigration.getAllowedUsers("proxyUser")).andReturn("validUser").once();
    expect(tproxyConfigration.getAllowedUsers("proxyUser")).andReturn("validuser").once();
    expect(tproxyConfigration.getAllowedUsers("proxyUser")).andReturn("validUser, tom, *").once();
    expect(tproxyConfigration.getAllowedUsers("proxyUser")).andReturn(null).once();
    expect(tproxyConfigration.getAllowedUsers("proxyUser")).andReturn("").once();
    expect(tproxyConfigration.getAllowedUsers("proxyUser")).andReturn("notValidUser").once();

    replayAll();

    // ambari.tproxy.proxyuser.proxyUser.users = "*"
    Assert.assertTrue(service.validateUser(tproxyConfigration, "proxyUser", "validUser"));

    // ambari.tproxy.proxyuser.proxyUser.users = "validUser"
    Assert.assertTrue(service.validateUser(tproxyConfigration, "proxyUser", "validUser"));

    // ambari.tproxy.proxyuser.proxyUser.users = "validuser"  (different case)
    Assert.assertTrue(service.validateUser(tproxyConfigration, "proxyUser", "validUser"));

    // ambari.tproxy.proxyuser.proxyUser.users = "validUser, tom, *"
    Assert.assertTrue(service.validateUser(tproxyConfigration, "proxyUser", "validUser"));

    // ambari.tproxy.proxyuser.proxyUser.users = null
    Assert.assertFalse(service.validateUser(tproxyConfigration, "proxyUser", "validUser"));

    // ambari.tproxy.proxyuser.proxyUser.users = ""
    Assert.assertFalse(service.validateUser(tproxyConfigration, "proxyUser", "validUser"));

    // ambari.tproxy.proxyuser.proxyUser.users = "notValidUser"
    Assert.assertFalse(service.validateUser(tproxyConfigration, "proxyUser", "validUser"));

    verifyAll();
  }

  @Test
  public void testValidateGroup() {
    AmbariProxiedUserDetailsService service = new AmbariProxiedUserDetailsService(createNiceMock(Configuration.class), createNiceMock(Users.class));

    AmbariTProxyConfiguration tproxyConfigration = createMock(AmbariTProxyConfiguration.class);
    expect(tproxyConfigration.getAllowedGroups("proxyUser")).andReturn("*").once();
    expect(tproxyConfigration.getAllowedGroups("proxyUser")).andReturn("validGroup").once();
    expect(tproxyConfigration.getAllowedGroups("proxyUser")).andReturn("validgroup").once();
    expect(tproxyConfigration.getAllowedGroups("proxyUser")).andReturn("validGroup, *").once();
    expect(tproxyConfigration.getAllowedGroups("proxyUser")).andReturn("").once();
    expect(tproxyConfigration.getAllowedGroups("proxyUser")).andReturn(null).once();
    expect(tproxyConfigration.getAllowedGroups("proxyUser")).andReturn("notValidGroup").once();

    Set<MemberEntity> memberEntities = new HashSet<>();
    memberEntities.add(createMockMemberEntity("validGroup"));
    memberEntities.add(createMockMemberEntity("users"));

    // Null Group name - maybe this is not possible
    memberEntities.add(createMockMemberEntity(null));

    // Null Group - maybe this is not possible
    MemberEntity memberEntity = createMock(MemberEntity.class);
    expect(memberEntity.getGroup()).andReturn(null).anyTimes();
    memberEntities.add(memberEntity);

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getMemberEntities()).andReturn(memberEntities).anyTimes();

    replayAll();

    // ambari.tproxy.proxyuser.proxyUser.groups = "*"
    Assert.assertTrue(service.validateGroup(tproxyConfigration, "proxyUser", userEntity));

    // ambari.tproxy.proxyuser.proxyUser.groups = "validGroup"
    Assert.assertTrue(service.validateGroup(tproxyConfigration, "proxyUser", userEntity));

    // ambari.tproxy.proxyuser.proxyUser.groups = "validgroup"  (different case)
    Assert.assertTrue(service.validateGroup(tproxyConfigration, "proxyUser", userEntity));

    // ambari.tproxy.proxyuser.proxyUser.groups = "validGroup, *"
    Assert.assertTrue(service.validateGroup(tproxyConfigration, "proxyUser", userEntity));

    // ambari.tproxy.proxyuser.proxyUser.groups = null
    Assert.assertFalse(service.validateGroup(tproxyConfigration, "proxyUser", userEntity));

    // ambari.tproxy.proxyuser.proxyUser.groups = ""
    Assert.assertFalse(service.validateGroup(tproxyConfigration, "proxyUser", userEntity));

    // ambari.tproxy.proxyuser.proxyUser.groups = "notValidGroup"
    Assert.assertFalse(service.validateGroup(tproxyConfigration, "proxyUser", userEntity));

    verifyAll();
  }

  @Test
  public void testIsInIpAddressRange() {
    AmbariProxiedUserDetailsService service = new AmbariProxiedUserDetailsService(createNiceMock(Configuration.class), createNiceMock(Users.class));

    Assert.assertTrue(service.isInIpAddressRange("192.168.74.10/32", "192.168.74.10"));
    Assert.assertFalse(service.isInIpAddressRange("192.168.74.10/32", "192.168.74.11"));

    for (int i = 0; i <= 255; i++) {
      Assert.assertTrue(service.isInIpAddressRange("192.168.1.0/24", String.format("192.168.1.%d", i)));
    }
    Assert.assertFalse(service.isInIpAddressRange("192.168.1.0/24", "192.168.2.100"));
  }

  private MemberEntity createMockMemberEntity(String groupName) {
    GroupEntity groupEntity = createMock(GroupEntity.class);
    expect(groupEntity.getGroupName()).andReturn(groupName).anyTimes();

    MemberEntity memberEntity = createMock(MemberEntity.class);
    expect(memberEntity.getGroup()).andReturn(groupEntity).anyTimes();
    return memberEntity;
  }

}