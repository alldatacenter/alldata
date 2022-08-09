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

package org.apache.ambari.server.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.quicklinks.Check;
import org.apache.ambari.server.state.quicklinks.Host;
import org.apache.ambari.server.state.quicklinks.Link;
import org.apache.ambari.server.state.quicklinks.Port;
import org.apache.ambari.server.state.quicklinks.Protocol;
import org.apache.ambari.server.state.quicklinks.QuickLinks;
import org.apache.ambari.server.state.quicklinks.QuickLinksConfiguration;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class QuickLinksConfigurationModuleTest {

  @Test
  public void testAddErrors() {
    Set<String> errors = ImmutableSet.of("one error", "two errors");
    QuickLinksConfigurationModule module = new QuickLinksConfigurationModule((File) null);
    module.addErrors(errors);
    assertEquals(errors, ImmutableSet.copyOf(module.getErrors()));
  }

  @Test
  public void testResolveInherit() throws Exception{
    QuickLinks[] results = resolveQuickLinks("parent_quicklinks.json", "child_quicklinks_to_inherit.json");
    QuickLinks parentQuickLinks = results[0];
    QuickLinks childQuickLinks = results[1];

    //resolved quicklinks configuration
    QuickLinksConfiguration childQuickLinksConfig = childQuickLinks.getQuickLinksConfiguration();
    assertNotNull(childQuickLinksConfig);

    //inherit links
    List<Link> links = childQuickLinksConfig.getLinks();
    assertNotNull(links);
    assertEquals(4, links.size());
    assertEquals(4, parentQuickLinks.getQuickLinksConfiguration().getLinks().size());

    //inherit protocol
    Protocol protocol = childQuickLinksConfig.getProtocol();
    assertNotNull(protocol);
    assertEquals("https", protocol.getType());
    assertEquals(1, protocol.getChecks().size());
  }

  @Test
  public void testResolveMerge() throws Exception {
    QuickLinks[] results = resolveQuickLinks("parent_quicklinks.json", "child_quicklinks_to_merge.json");
    QuickLinks parentQuickLinks = results[0];
    QuickLinks childQuickLinks = results[1];

    //resolved quicklinks configuration
    QuickLinksConfiguration childQuickLinksConfig = childQuickLinks.getQuickLinksConfiguration();
    assertNotNull(childQuickLinksConfig);

    //merged links
    List<Link> links = childQuickLinksConfig.getLinks();
    assertNotNull(links);
    assertEquals(7, links.size());
    assertEquals(4, parentQuickLinks.getQuickLinksConfiguration().getLinks().size());
    Link threadStacks = getLink(links, "thread_stacks");
    assertNotNull("https_regex property should have been inherited",
        threadStacks.getPort().getHttpsRegex());
  }

  private Link getLink(Collection<Link> links, String name) {
    for (Link link: links) {
      if (name.equals(link.getName())) {
        return link;
      }
    }
    throw new NoSuchElementException("name");
  }

  @Test
  public void testResolveOverride() throws Exception{
    QuickLinks[] results = resolveQuickLinks("parent_quicklinks.json", "child_quicklinks_to_override.json");
    QuickLinks parentQuickLinks = results[0];
    QuickLinks childQuickLinks = results[1];

    //resolved quicklinks configuration
    QuickLinksConfiguration childQuickLinksConfig = childQuickLinks.getQuickLinksConfiguration();
    assertNotNull(childQuickLinksConfig);

    //links
    List<Link> links = childQuickLinksConfig.getLinks();
    assertNotNull(links);
    assertEquals(7, links.size());
    assertEquals(4, parentQuickLinks.getQuickLinksConfiguration().getLinks().size());
    boolean hasLink = false;
    for(Link link: links){
      String name = link.getName();
      if("thread_stacks".equals(name)){
        hasLink = true;
        Port port = link.getPort();
        assertEquals("mapred-site", port.getSite());
        Host host = link.getHost();
        assertEquals("core-site", host.getSite());
      }
    }
    assertTrue(hasLink);

    //protocol
    Protocol protocol = childQuickLinksConfig.getProtocol();
    assertNotNull(protocol);
    assertEquals("http", protocol.getType());
    assertEquals(3, protocol.getChecks().size());
    List<Check> checks = protocol.getChecks();
    for(Check check: checks){
      assertEquals("mapred-site", check.getSite());
    }
  }

  @Test
  public void testResolveOverrideProperties() throws Exception{
    QuickLinks[] results = resolveQuickLinks("parent_quicklinks_with_attributes.json",
        "child_quicklinks_with_attributes.json");
    QuickLinks parentQuickLinks = results[0];
    QuickLinks childQuickLinks = results[1];

    //resolved quicklinks configuration
    QuickLinksConfiguration childQuickLinksConfig = childQuickLinks.getQuickLinksConfiguration();
    assertNotNull(childQuickLinksConfig);

    //links
    List<Link> links = childQuickLinksConfig.getLinks();
    assertNotNull(links);
    assertEquals(3, links.size());
    Map<String, Link> linksByName = new HashMap<>();
    for (Link link: links) {
      linksByName.put(link.getName(), link);
    }
    assertEquals("Links are not properly overridden for foo_ui",
        Lists.newArrayList("authenticated", "sso"),
        linksByName.get("foo_ui").getAttributes());
    assertEquals("Parent links for foo_jmx are not inherited.",
        Lists.newArrayList("authenticated"),
        linksByName.get("foo_jmx").getAttributes());
    assertEquals("Links are not properly overridden for foo_logs",
        new ArrayList<>(),
        linksByName.get("foo_logs").getAttributes());
  }

  private QuickLinks[] resolveQuickLinks(String parentJson, String childJson) throws AmbariException{
    File parentQuiclinksFile = new File(this.getClass().getClassLoader().getResource(parentJson).getFile());
    File childQuickLinksFile = new File(this.getClass().getClassLoader().getResource(childJson).getFile());

    QuickLinksConfigurationModule parentModule = new QuickLinksConfigurationModule(parentQuiclinksFile);
    QuickLinksConfigurationModule childModule = new QuickLinksConfigurationModule(childQuickLinksFile);

    childModule.resolve(parentModule, null, null, null);

    QuickLinks parentQuickLinks = parentModule.getModuleInfo().getQuickLinksConfigurationMap().get(QuickLinksConfigurationModule.QUICKLINKS_CONFIGURATION_KEY);
    QuickLinks childQuickLinks = childModule.getModuleInfo().getQuickLinksConfigurationMap().get(QuickLinksConfigurationModule.QUICKLINKS_CONFIGURATION_KEY);

    return new QuickLinks[]{parentQuickLinks, childQuickLinks};
  }
}
