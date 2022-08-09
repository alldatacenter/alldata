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

package org.apache.ambari.server.topology;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.junit.Test;

/**
 * HostGroupInfo unit tests
 */
public class HostGroupInfoTest {

  @Test
  public void testGetHostGroupName() {
    HostGroupInfo group = new HostGroupInfo("test-name");
    assertEquals("test-name", group.getHostGroupName());
  }

  @Test
  public void testHostName_isConvertedToLowercase() {
    HostGroupInfo group = new HostGroupInfo("test-name");
    // single host add
    group.addHost("HOST1");
    assertEquals(1, group.getHostNames().size());
    assertTrue(group.getHostNames().contains("host1"));
  }

  @Test
  public void testSetGetHostNames() {
    HostGroupInfo group = new HostGroupInfo("test-name");
    // single host add
    group.addHost("host1");
    assertEquals(1, group.getHostNames().size());
    assertTrue(group.getHostNames().contains("host1"));

    // add collection of hosts and duplicate host1
    group.addHosts(Arrays.asList("host2", "host3", "host1"));
    Collection<String> hostNames = group.getHostNames();
    assertEquals(3, hostNames.size());
    assertTrue(hostNames.contains("host1"));
    assertTrue(hostNames.contains("host2"));
    assertTrue(hostNames.contains("host3"));

    // ensure that a copy was returned
    hostNames.clear();
    hostNames = group.getHostNames();
    assertEquals(3, hostNames.size());
    assertTrue(hostNames.contains("host1"));
    assertTrue(hostNames.contains("host2"));
    assertTrue(hostNames.contains("host3"));

  }



  @Test
  public void testSetGetRequestedHostCount_explicit() {
    HostGroupInfo group = new HostGroupInfo("test-name");
    assertEquals(0, group.getRequestedHostCount());
    group.setRequestedCount(5);
    assertEquals(5, group.getRequestedHostCount());
  }

  @Test
  public void testSetGetRequestedHostCount_hostNamesSpecified() {
    HostGroupInfo group = new HostGroupInfo("test-name");
    assertEquals(0, group.getRequestedHostCount());
    group.addHosts(Arrays.asList("host2", "host3", "host1"));
    assertEquals(3, group.getRequestedHostCount());
  }

  @Test
  public void testSetGetGetConfiguration() {
    Configuration configuration = createNiceMock(Configuration.class);
    HostGroupInfo group = new HostGroupInfo("test-name");
    assertNull(group.getConfiguration());
    group.setConfiguration(configuration);
    assertSame(configuration, group.getConfiguration());
  }

  @Test
  public void testSetGetPredicate() throws Exception {
    HostGroupInfo group = new HostGroupInfo("test-name");
    assertNull(group.getPredicateString());
    assertNull(group.getPredicate());

    group.setPredicate("Hosts/host_name=awesome.host.com");
    assertEquals("Hosts/host_name=awesome.host.com", group.getPredicateString());
    assertEquals(new EqualsPredicate<>("Hosts/host_name", "awesome.host.com"), group.getPredicate());
  }

  @Test(expected=InvalidQueryException.class)
  public void testSetPredicate_invalid() throws Exception {
    HostGroupInfo group = new HostGroupInfo("test-name");
    group.setPredicate("=thisIsNotAPredicate");
  }
}
