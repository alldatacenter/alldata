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

package org.apache.ambari.server.state.quicklinksprofile;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfileBuilder.COMPONENTS;
import static org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfileBuilder.FILTERS;
import static org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfileBuilder.NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class QuickLinksProfileBuilderTest {


  @Test
  public void testBuildProfileOnlyGlobalFilters() throws Exception {
    Set<Map<String, String>> filters = newHashSet(
      filter("namenode_ui", null, true),
      filter(null, "sso", true),
      filter(null, null, false)
    );

    String profileJson = new QuickLinksProfileBuilder().buildQuickLinksProfile(filters, null);

    //verify
    QuickLinksProfile profile = new QuickLinksProfileParser().parse(profileJson.getBytes());
    assertFilterExists(profile, null, null, Filter.linkNameFilter("namenode_ui", true));
    assertFilterExists(profile, null, null, Filter.linkAttributeFilter("sso", true));
    assertFilterExists(profile, null, null, Filter.acceptAllFilter(false));
  }

  @Test
  public void testBuildProfileOnlyServiceFilters() throws Exception {
    Map<String, Object> nameNode = component("NAMENODE",
        newHashSet(filter("namenode_ui", null, false)));

    Map<String, Object> hdfs = service("HDFS",
        newHashSet(nameNode),
        newHashSet(filter(null, "sso", true)));

    Set<Map<String, Object>> services = Sets.newHashSet(hdfs);

    String profileJson = new QuickLinksProfileBuilder().buildQuickLinksProfile(null, services);

    //verify
    QuickLinksProfile profile = new QuickLinksProfileParser().parse(profileJson.getBytes());
    assertFilterExists(profile, "HDFS", "NAMENODE", Filter.linkNameFilter("namenode_ui", false));
    assertFilterExists(profile, "HDFS", null, Filter.linkAttributeFilter("sso", true));
  }

  @Test
  public void testBuildProfileBothGlobalAndServiceFilters() throws Exception {
    Set<Map<String, String>> globalFilters = newHashSet( filter(null, null, false) );

    Map<String, Object> nameNode = component(
      "NAMENODE",
      newHashSet(
        filter("namenode_ui", null, false),
        filter("namenode_logs", null, "http://customlink.org/namenode_logs", true)
      )
    );

    Map<String, Object> hdfs = service("HDFS",
        newHashSet(nameNode),
        newHashSet(filter(null, "sso", true)));

    Set<Map<String, Object>> services = Sets.newHashSet(hdfs);

    String profileJson = new QuickLinksProfileBuilder().buildQuickLinksProfile(globalFilters, services);

    // verify
    QuickLinksProfile profile = new QuickLinksProfileParser().parse(profileJson.getBytes());
    assertFilterExists(profile, null, null, Filter.acceptAllFilter(false));
    assertFilterExists(profile, "HDFS", "NAMENODE", Filter.linkNameFilter("namenode_ui", false));
    assertFilterExists(profile, "HDFS", "NAMENODE", Filter.linkNameFilter("namenode_ui", false));
    assertFilterExists(profile, "HDFS", "NAMENODE", Filter.linkNameFilter("namenode_logs",
      "http://customlink.org/namenode_logs", true));
    assertFilterExists(profile, "HDFS", null, Filter.linkAttributeFilter("sso", true));
  }

  @Test(expected = QuickLinksProfileEvaluationException.class)
  public void testBuildProfileBadInputStructure() throws Exception {
    new QuickLinksProfileBuilder().buildQuickLinksProfile("Hello", "World");
  }

  @Test(expected = QuickLinksProfileEvaluationException.class)
  public void testBuildProfileMissingDataServiceName() throws Exception {
    Map<String, Object> nameNode = component("NAMENODE",
        newHashSet(filter("namenode_ui", null, false)));

    Map<String, Object> hdfs = service(null, // intentionally omitting service name
        newHashSet(nameNode),
        newHashSet(filter(null, "sso", true)));

    Set<Map<String, Object>> services = Sets.newHashSet(hdfs);

    new QuickLinksProfileBuilder().buildQuickLinksProfile(null, services);
  }

  @Test(expected = QuickLinksProfileEvaluationException.class)
  public void testBuildProfileMissingDataComponentName() throws Exception {
    Map<String, Object> nameNode = component(null, // intentionally omitting component name
        newHashSet(filter("namenode_ui", null, false)));

    Map<String, Object> hdfs = service("HDFS",
        newHashSet(nameNode),
        newHashSet(filter(null, "sso", true)));

    Set<Map<String, Object>> services = Sets.newHashSet(hdfs);

    new QuickLinksProfileBuilder().buildQuickLinksProfile(null, services);
  }

  @Test(expected = QuickLinksProfileEvaluationException.class)
  public void testBuildProfileInvalidProfileDefiniton_contradictingFilters() throws Exception {
    // Contradicting rules in the profile
    Set<Map<String, String>> filters = newHashSet(
        filter(null, "sso", true),
        filter(null, "sso", false)
    );

    new QuickLinksProfileBuilder().buildQuickLinksProfile(filters, null);
  }

  @Test(expected = QuickLinksProfileEvaluationException.class)
  public void testBuildProfileInvalidProfileDefiniton_invalidAttribute() throws Exception {
    Map<String, String> badFilter = ImmutableMap.of("visible", "true", "linkkk_atirbuteee", "sso");
    Set<Map<String, String>> filters = newHashSet(badFilter);

    new QuickLinksProfileBuilder().buildQuickLinksProfile(filters, null);
  }

  /**
   * Verifies that the filter specified by the arguments exists in the received {@link QuickLinksProfile}
   * @param profile the {@link QuickLinksProfile} to examine
   * @param serviceName the service name where the filter is defined. {@code null} means the searched filter is a global
   *                    filter
   * @param componentName the component name where the filter is defined. Only makes sense when serviceName is defined
   *                    too. {@code null} means the searched filter is a service level filter, not component level one.
   *                    filter
   * @param filter the {@link Filter} to look for.
   */
  private static void assertFilterExists(@Nonnull QuickLinksProfile profile,
                                         @Nullable String serviceName,
                                         @Nullable String componentName,
                                         @Nonnull Filter filter) {
    // looking for a global filter
    if (null == serviceName) {
      if (!profile.getFilters().contains(filter)) {
        throw new AssertionError("Expected global filter not found: " + filter);
      }
    }
    // looking for a filter defined on service or component level
    else {
      Service service = findService(profile.getServices(), serviceName);
      // looking for a filter defined on service level
      if (null == componentName) {
        if (!service.getFilters().contains(filter)) {
          throw new AssertionError(String.format("Expected filter not found. Service: %s, Filter: %s",
              serviceName, filter));
        }
      }
      // looking for a filter defined on component level
      else {
        Component component = findComponent(service.getComponents(), componentName);
        if (!component.getFilters().contains(filter)) {
          throw new AssertionError(String.format("Expected filter not found. Service: %s, Component: %s, Filter: %s",
              serviceName, componentName, filter));
        }
      }
    }
  }

  private static Component findComponent(List<Component> components, String componentName) {
    for (Component component: components) {
      if (component.getName().equals(componentName)) {
        return component;
      }
    }
    throw new AssertionError("Expected component not found: " + componentName);
  }

  private static Service findService(List<Service> services, String serviceName) {
    for (Service service: services) {
      if (service.getName().equals(serviceName)) {
        return service;
      }
    }
    throw new AssertionError("Expected service not found: " + serviceName);
  }

  public static Map<String, String> filter(@Nullable String linkName,
                                           @Nullable String attributeName,
                                           boolean visible) {
    return filter(linkName, attributeName, null, visible);
  }

  public static Map<String, String> filter(@Nullable String linkName,
                                           @Nullable String attributeName,
                                           @Nullable String linkUrl,
                                           boolean visible) {
    Map<String, String> map = new HashMap<>(4);
    if (null != linkName) {
      map.put(LinkNameFilter.LINK_NAME, linkName);
    }
    if (null != linkUrl) {
      map.put(LinkNameFilter.LINK_URL, linkUrl);
    }
    if (null != attributeName) {
      map.put(LinkAttributeFilter.LINK_ATTRIBUTE, attributeName);
    }
    map.put(Filter.VISIBLE, Boolean.toString(visible));
    return map;
  }

  public static Map<String, Object> component(String componentName, Set<Map<String, String>> filters) {
    Map<String, Object> map = new HashMap<>();
    map.put(NAME, componentName);
    map.put(FILTERS, filters);
    return map;
  }

  public static Map<String, Object> service(String serviceName, Set<Map<String, Object>> components,
                                             Set<Map<String, String>> filters) {
    Map<String, Object> map = new HashMap<>();
    map.put(NAME, serviceName);
    if (null != components) {
      map.put(COMPONENTS, components);
    }
    if (null != filters) {
      map.put(FILTERS, filters);
    }
    return map;
  }

}