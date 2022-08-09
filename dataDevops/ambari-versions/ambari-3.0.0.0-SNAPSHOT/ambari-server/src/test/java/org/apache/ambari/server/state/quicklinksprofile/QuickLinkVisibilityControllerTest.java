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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.apache.ambari.server.state.quicklinks.Link;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class QuickLinkVisibilityControllerTest {

  static final String AUTHENTICATED = "authenticated";
  static final String SSO = "sso";
  static final String NAMENODE = "NAMENODE";
  static final String HDFS = "HDFS";
  public static final String YARN = "YARN";
  static final String NAMENODE_UI = "namenode_ui";
  static final String NAMENODE_LOGS = "namenode_logs";
  static final String NAMENODE_JMX = "namenode_jmx";
  static final String THREAD_STACKS = "Thread Stacks";
  static final String LINK_URL_1 = "www.overridden.org/1";
  static final String LINK_URL_2 = "www.overridden.org/2";
  static final String LINK_URL_3 = "www.overridden.org/3";


  private Link namenodeUi;
  private Link namenodeLogs;
  private Link namenodeJmx;
  private Link threadStacks;

  public QuickLinkVisibilityControllerTest() {
    namenodeUi = link(NAMENODE_UI, NAMENODE, ImmutableList.of(AUTHENTICATED));
    namenodeLogs = link(NAMENODE_LOGS, NAMENODE, null);
    namenodeJmx = link(NAMENODE_JMX, NAMENODE, null);
    threadStacks = link(THREAD_STACKS, NAMENODE, null);
  }

  /**
   * Test to prove that {@link DefaultQuickLinkVisibilityController} can accept quicklink profiles with null values.
   */
  @Test
  public void testNullsAreAccepted() throws Exception {
    QuickLinksProfile profile = QuickLinksProfile.create(ImmutableList.of(Filter.acceptAllFilter(true)), null);
    DefaultQuickLinkVisibilityController evaluator = new DefaultQuickLinkVisibilityController(profile);
    evaluator.isVisible(HDFS, namenodeUi); //should not throw NPE

    Service service = Service.create(HDFS, ImmutableList.of(Filter.acceptAllFilter(true)), null);
    profile = QuickLinksProfile.create(null, ImmutableList.of(service));
    evaluator = new DefaultQuickLinkVisibilityController(profile);
    evaluator.isVisible(HDFS, namenodeUi); //should not throw NPE

  }

  /**
   * Quicklinks profile must contain at least one filter (can be on any level: global/component/service), otherwise
   * an exception is thrown.
   */
  @Test(expected = QuickLinksProfileEvaluationException.class)
  public void testProfileMustContainAtLeastOneFilter() throws Exception {
    Component component = Component.create("NAMENODE", null);
    Service service = Service.create(HDFS, null, ImmutableList.of(component));
    QuickLinksProfile profile = QuickLinksProfile.create(null, ImmutableList.of(service));
    QuickLinkVisibilityController evaluator = new DefaultQuickLinkVisibilityController(profile);
  }

  /**
   * Test to prove that {@link Link}'s with unset {@code componentName} fields are handled properly.
   */
  @Test
  public void testLinkWithNoComponentField() throws Exception {
    Component component = Component.create(NAMENODE,
        ImmutableList.of(Filter.linkNameFilter(NAMENODE_UI, true)));

    Service service = Service.create(HDFS, ImmutableList.of(), ImmutableList.of(component));

    QuickLinksProfile profile = QuickLinksProfile.create(ImmutableList.of(), ImmutableList.of(service));
    DefaultQuickLinkVisibilityController evaluator = new DefaultQuickLinkVisibilityController(profile);
    namenodeUi.setComponentName(null);
    assertFalse("Link should be hidden as there are no applicable filters", evaluator.isVisible(HDFS, namenodeUi));
  }

  /**
   * Test to prove that component level filters are evaluated first.
   */
  @Test
  public void testComponentLevelFiltersEvaluatedFirst() throws Exception {
    Component component = Component.create(
        NAMENODE,
        ImmutableList.of(Filter.linkAttributeFilter(AUTHENTICATED, true)));

    Service service = Service.create(
        HDFS,
        ImmutableList.of(Filter.linkAttributeFilter(AUTHENTICATED, false)),
        ImmutableList.of(component));

    QuickLinksProfile profile = QuickLinksProfile.create(
        ImmutableList.of(Filter.acceptAllFilter(false)),
        ImmutableList.of(service));

    DefaultQuickLinkVisibilityController evaluator = new DefaultQuickLinkVisibilityController(profile);
    assertTrue("Component level filter should have been applied.", evaluator.isVisible(HDFS, namenodeUi));
  }

  /**
   * Test to prove that service level filters are evaluated secondly.
   */
  @Test
  public void testServiceLevelFiltersEvaluatedSecondly() throws Exception {
    Component component = Component.create(NAMENODE,
        ImmutableList.of(Filter.linkAttributeFilter(SSO, false)));

    Service service = Service.create(HDFS,
        ImmutableList.of(Filter.linkAttributeFilter(AUTHENTICATED, true)),
        ImmutableList.of(component));

    QuickLinksProfile profile = QuickLinksProfile.create(
        ImmutableList.of(Filter.acceptAllFilter(false)),
        ImmutableList.of(service));

    DefaultQuickLinkVisibilityController evaluator = new DefaultQuickLinkVisibilityController(profile);
    assertTrue("Component level filter should have been applied.", evaluator.isVisible(HDFS, namenodeUi));
  }

  /**
   * Test to prove that global filters are evaluated last.
   */
  @Test
  public void testGlobalFiltersEvaluatedLast() throws Exception {
    Component component = Component.create(NAMENODE,
        ImmutableList.of(Filter.linkAttributeFilter(SSO, false)));

    Service service = Service.create(HDFS,
        ImmutableList.of(Filter.linkAttributeFilter(SSO, false)),
        ImmutableList.of(component));

    QuickLinksProfile profile = QuickLinksProfile.create(
        ImmutableList.of(Filter.acceptAllFilter(true)),
        ImmutableList.of(service));

    DefaultQuickLinkVisibilityController evaluator = new DefaultQuickLinkVisibilityController(profile);
    assertTrue("Global filter should have been applied.", evaluator.isVisible(HDFS, namenodeUi));
  }

  /**
   * Test to prove that the link is hidden if no filters apply.
   */
  @Test
  public void testNoMatchingRule() throws Exception {
    Component component1 = Component.create(NAMENODE,
        ImmutableList.of(Filter.linkAttributeFilter(SSO, true)));

    Component component2 = Component.create("DATANODE",
        ImmutableList.of(Filter.acceptAllFilter(true)));

    Service service1 = Service.create(HDFS,
        ImmutableList.of(Filter.linkAttributeFilter(SSO, true)),
        ImmutableList.of(component1, component2));

    Service service2 = Service.create("YARN",
        ImmutableList.of(Filter.acceptAllFilter(true)),
        ImmutableList.of());

    QuickLinksProfile profile = QuickLinksProfile.create(
        ImmutableList.of(Filter.linkAttributeFilter(SSO, true)),
        ImmutableList.of(service1, service2));

    DefaultQuickLinkVisibilityController evaluator = new DefaultQuickLinkVisibilityController(profile);
    assertFalse("No filters should have been applied, so default false should have been returned.",
        evaluator.isVisible(HDFS, namenodeUi));
  }

  @Test
  public void testUrlOverride() throws Exception {
    Component nameNode = Component.create(
      NAMENODE,
      ImmutableList.of(
        Filter.linkNameFilter(NAMENODE_UI, true),
        Filter.linkNameFilter(NAMENODE_LOGS, LINK_URL_1, true)));
    Service hdfs = Service.create(
      HDFS,
      ImmutableList.of(Filter.linkNameFilter(NAMENODE_JMX, LINK_URL_2, true)),
      ImmutableList.of(nameNode));
    QuickLinksProfile profile = QuickLinksProfile.create(
      ImmutableList.of(Filter.linkNameFilter(THREAD_STACKS, LINK_URL_3, true)),
      ImmutableList.of(hdfs));

    DefaultQuickLinkVisibilityController evaluator = new DefaultQuickLinkVisibilityController(profile);
    assertEquals(Optional.empty(), evaluator.getUrlOverride(HDFS, namenodeUi));
    assertEquals(Optional.of(LINK_URL_1), evaluator.getUrlOverride(HDFS, namenodeLogs));
    assertEquals(Optional.of(LINK_URL_2), evaluator.getUrlOverride(HDFS, namenodeJmx));
    // component name doesn't matter
    namenodeLogs.setComponentName(null);
    assertEquals(Optional.of(LINK_URL_1), evaluator.getUrlOverride(HDFS, namenodeLogs));
    // no override for links not in the profile
    assertEquals(Optional.empty(), evaluator.getUrlOverride(YARN, link("resourcemanager_ui", "RESOURCEMANAGER", null)));
    // url overrides in global filters are ignored
    assertEquals(Optional.empty(), evaluator.getUrlOverride(HDFS, threadStacks));
  }

  @Test
  public void testUrlOverride_duplicateDefinitions() throws Exception {
    // same link is defined twice for a service
    Component nameNode = Component.create(
      NAMENODE,
      ImmutableList.of(
        Filter.linkNameFilter(NAMENODE_UI, LINK_URL_1, true))); // this will override service level setting for the same link
    Service hdfs = Service.create(
      HDFS,
      ImmutableList.of(Filter.linkNameFilter(NAMENODE_UI, LINK_URL_2, true)), // same link on service level with different url
      ImmutableList.of(nameNode));
    Service yarn = Service.create(
      YARN,
      ImmutableList.of(Filter.linkNameFilter(NAMENODE_UI, LINK_URL_3, true)), // this belongs to an other service so doesn't affect outcome
      ImmutableList.of(nameNode));

    QuickLinksProfile profile = QuickLinksProfile.create(
      ImmutableList.of(),
      ImmutableList.of(hdfs));

    DefaultQuickLinkVisibilityController evaluator = new DefaultQuickLinkVisibilityController(profile);
    assertEquals(Optional.of(LINK_URL_1), evaluator.getUrlOverride(HDFS, namenodeUi));
  }


  private static final Link link(String name, String componentName, List<String> attributes) {
    Link link = new Link();
    link.setName(name);
    link.setComponentName(componentName);
    link.setAttributes(attributes);
    return link;
  }

}