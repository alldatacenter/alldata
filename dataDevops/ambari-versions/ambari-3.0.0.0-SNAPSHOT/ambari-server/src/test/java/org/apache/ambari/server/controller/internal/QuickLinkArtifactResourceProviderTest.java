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
package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.stack.QuickLinksConfigurationModule;
import org.apache.ambari.server.state.QuickLinksConfigurationInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.quicklinks.Link;
import org.apache.ambari.server.state.quicklinks.QuickLinks;
import org.apache.ambari.server.state.quicklinksprofile.QuickLinkVisibilityControllerFactory;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class QuickLinkArtifactResourceProviderTest {

  private Injector injector;
  String quicklinkProfile;

  @Before
  public void before() {
    injector = Guice.createInjector(new MockModule());
  }

  private QuickLinkArtifactResourceProvider createProvider() {
    return new QuickLinkArtifactResourceProvider(injector.getInstance(AmbariManagementController.class));
  }

  /**
   * Test to prove the returned links' visibility reflect the actual profile
   */
  @Test
  public void getResourcesRespectsVisibility() throws Exception {
    quicklinkProfile = Resources.toString(Resources.getResource("example_quicklinks_profile.json"), Charsets.UTF_8);

    QuickLinkArtifactResourceProvider provider = createProvider();
    Predicate predicate = new PredicateBuilder().property(
        QuickLinkArtifactResourceProvider.STACK_NAME_PROPERTY_ID).equals("HDP").
        and().
        property(QuickLinkArtifactResourceProvider.STACK_VERSION_PROPERTY_ID).equals("2.0.6").
        and().
        property(QuickLinkArtifactResourceProvider.STACK_SERVICE_NAME_PROPERTY_ID).equals("YARN").
        toPredicate();
    Set<Resource> resources =
        provider.getResources(PropertyHelper.getReadRequest(Sets.newHashSet()), predicate);
    Map<String, Link> linkMap = getLinks(resources);

    for (Map.Entry<String, Link> entry: linkMap.entrySet()) {
      assertTrue("Only resourcemanager_ui should be visible.",
          entry.getValue().isVisible() == entry.getKey().equals("resourcemanager_ui"));
    }
  }

  /**
   * Test the application of link url override in the quick links profile
   */
  @Test
  public void getResourcesWithUrlOverride() throws Exception {
    quicklinkProfile = Resources.toString(Resources.getResource("example_quicklinks_profile.json"), Charsets.UTF_8);

    QuickLinkArtifactResourceProvider provider = createProvider();
    Predicate predicate = new PredicateBuilder().property(
      QuickLinkArtifactResourceProvider.STACK_NAME_PROPERTY_ID).equals("HDP").
      and().
      property(QuickLinkArtifactResourceProvider.STACK_VERSION_PROPERTY_ID).equals("2.0.6").
      and().
      property(QuickLinkArtifactResourceProvider.STACK_SERVICE_NAME_PROPERTY_ID).equals("YARN").
      toPredicate();
    Set<Resource> resources =
      provider.getResources(PropertyHelper.getReadRequest(Sets.newHashSet()), predicate);
    Map<String, Link> linkMap = getLinks(resources);

    assertEquals("http://customlink.org/resourcemanager", linkMap.get("resourcemanager_ui").getUrl());
  }

  /**
   * Test to prove the all links are visible if no profile is set
   */
  @Test
  public void whenNoProfileIsSetAllLinksAreVisible() throws Exception {
    quicklinkProfile = null;

    QuickLinkArtifactResourceProvider provider = createProvider();
    Predicate predicate = new PredicateBuilder().property(
        QuickLinkArtifactResourceProvider.STACK_NAME_PROPERTY_ID).equals("HDP").
        and().
        property(QuickLinkArtifactResourceProvider.STACK_VERSION_PROPERTY_ID).equals("2.0.6").
        and().
        property(QuickLinkArtifactResourceProvider.STACK_SERVICE_NAME_PROPERTY_ID).equals("YARN").
        toPredicate();
    Set<Resource> resources =
        provider.getResources(PropertyHelper.getReadRequest(Sets.newHashSet()), predicate);
    Map<String, Link> linkMap = getLinks(resources);

    for (Link link: linkMap.values()) {
      assertTrue("All links should be visible.", link.isVisible());
    }
  }

  /**
   * Test to prove the all links are visible if invalid profile is set
   */
  @Test
  public void whenInvalidProfileIsSetAllLinksAreVisible() throws Exception {
    quicklinkProfile = "{}";

    QuickLinkArtifactResourceProvider provider = createProvider();
    Predicate predicate = new PredicateBuilder().property(
        QuickLinkArtifactResourceProvider.STACK_NAME_PROPERTY_ID).equals("HDP").
        and().
        property(QuickLinkArtifactResourceProvider.STACK_VERSION_PROPERTY_ID).equals("2.0.6").
        and().
        property(QuickLinkArtifactResourceProvider.STACK_SERVICE_NAME_PROPERTY_ID).equals("YARN").
        toPredicate();
    Set<Resource> resources =
        provider.getResources(PropertyHelper.getReadRequest(Sets.newHashSet()), predicate);
    Map<String, Link> linkMap = getLinks(resources);

    for (Link link: linkMap.values()) {
      assertTrue("All links should be visible.", link.isVisible());
    }
  }

  private Map<String, Link> getLinks(Set<Resource> resources) {
    QuickLinks quickLinks = (QuickLinks)
        resources.iterator().next().getPropertiesMap().get("QuickLinkInfo/quicklink_data").values().iterator().next();
    Map<String, Link> linksMap = new HashMap<>();
    for (Link link: quickLinks.getQuickLinksConfiguration().getLinks()) {
      linksMap.put(link.getName(), link);
    }
    return linksMap;
  }

  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
      StackInfo stack = createMock(StackInfo.class);
      ServiceInfo service = createMock(ServiceInfo.class);

      QuickLinksConfigurationInfo qlConfigInfo = new QuickLinksConfigurationInfo();
      qlConfigInfo.setDeleted(false);
      qlConfigInfo.setFileName("parent_quicklinks.json");
      qlConfigInfo.setIsDefault(true);
      File qlFile = new File(Resources.getResource("parent_quicklinks.json").getFile()) ;
      QuickLinksConfigurationModule module = new QuickLinksConfigurationModule(qlFile, qlConfigInfo);
      module.getModuleInfo();

      AmbariManagementController amc = createMock(AmbariManagementController.class);
      expect(amc.getAmbariMetaInfo()).andReturn(metaInfo).anyTimes();
      expect(amc.getQuicklinkVisibilityController())
        .andAnswer(() -> QuickLinkVisibilityControllerFactory.get(quicklinkProfile)).anyTimes();

      try {
        expect(metaInfo.getStack(anyString(), anyString())).andReturn(stack).anyTimes();
      }
      catch (AmbariException ex) {
        throw new RuntimeException(ex);
      }
      expect(stack.getServices()).andReturn(ImmutableList.of(service)).anyTimes();
      expect(stack.getService("YARN")).andReturn(service).anyTimes();
      expect(service.getQuickLinksConfigurationsMap()).andReturn(ImmutableMap.of("YARN", qlConfigInfo));
      expect(service.getName()).andReturn("YARN").anyTimes();
      binder.bind(AmbariManagementController.class).toInstance(amc);
      replay(amc, metaInfo, stack, service);
    }
  }
}
