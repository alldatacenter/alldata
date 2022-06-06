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
package org.apache.ambari.server.state;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Tests desired config instances.
 */
public class DesiredConfigTest {

  @Test
  public void testDesiredConfig() throws Exception {
    DesiredConfig dc = new DesiredConfig();
    dc.setServiceName("service");
    dc.setTag("global");

    Assert.assertEquals("Expected service 'service'", "service", dc.getServiceName());
    Assert.assertEquals("Expected version 'global'", "global", dc.getTag());
    Assert.assertEquals("Expected no host overrides", 0, dc.getHostOverrides().size());
    

    List<DesiredConfig.HostOverride> hosts = Arrays.asList(
        new DesiredConfig.HostOverride("h1", "v2"),
        new DesiredConfig.HostOverride("h2", "v3"));
    dc.setHostOverrides(hosts);

    Assert.assertNotNull("Expected host overrides to be set", dc.getHostOverrides());
    Assert.assertEquals("Expected host override equality", hosts, dc.getHostOverrides());
  }

  @Test
  public void testHostOverride() throws Exception {
    DesiredConfig.HostOverride override = new DesiredConfig.HostOverride("h1", "v1");

    Assert.assertNotNull(override.getName());
    Assert.assertNotNull(override.getVersionTag());
    Assert.assertEquals("Expected override host 'h1'", "h1", override.getName());
    Assert.assertEquals("Expected override version 'v1'", "v1", override.getVersionTag());
  }

  @Test
  public void testEquals() throws Exception {
    EqualsVerifier.forClass(DesiredConfig.class)
      .usingGetClass()
      .suppress(Warning.NONFINAL_FIELDS)
      .verify();
  }

  @Test
  public void testHostOverride_Equals() throws Exception {
    EqualsVerifier.forClass(DesiredConfig.HostOverride.class)
      .usingGetClass()
      .verify();
  }

}
