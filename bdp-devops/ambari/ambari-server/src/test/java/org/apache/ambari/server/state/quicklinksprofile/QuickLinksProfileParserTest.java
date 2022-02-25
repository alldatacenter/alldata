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

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.Resources;

public class QuickLinksProfileParserTest {


  @Test
  public void testParseProfile() throws Exception {
    String profileName = "example_quicklinks_profile.json";
    QuickLinksProfileParser parser = new QuickLinksProfileParser();
    QuickLinksProfile profile = parser.parse(Resources.getResource(profileName));
    assertEquals(1, profile.getFilters().size());
    assertEquals(
        Filter.linkAttributeFilter("sso", true),
        profile.getFilters().get(0));
    assertEquals(3, profile.getServices().size());

    Service hdfs = profile.getServices().get(0);
    assertEquals("HDFS", hdfs.getName());
    assertEquals(1, hdfs.getFilters().size());
    assertEquals(1, hdfs.getComponents().size());
    assertEquals(
        Filter.linkAttributeFilter("authenticated", true),
        hdfs.getFilters().get(0));

    Component nameNode = hdfs.getComponents().get(0);
    assertEquals(2, nameNode.getFilters().size());
    assertEquals(
        Filter.linkNameFilter("namenode_ui", "http://customlink.org/namenode", false),
        nameNode.getFilters().get(0));

    Component historyServer = profile.getServices().get(1).getComponents().get(0);
    assertEquals(1, historyServer.getFilters().size());
    assertEquals(
        Filter.acceptAllFilter(true),
        historyServer.getFilters().get(0));

    Service yarn = profile.getServices().get(2);
    assertEquals(1, yarn.getFilters().size());
    assertEquals(
        Filter.linkNameFilter("resourcemanager_ui", "http://customlink.org/resourcemanager", true),
        yarn.getFilters().get(0));
  }

  @Test(expected = JsonProcessingException.class)
  public void testParseInconsistentProfile_ambigousFilterDefinition() throws Exception {
    String profileName = "inconsistent_quicklinks_profile.json";
    QuickLinksProfileParser parser = new QuickLinksProfileParser();
    parser.parse(Resources.getResource(profileName));
  }

  @Test(expected = JsonProcessingException.class)
  public void testParseInconsistentProfile_invalidLinkUrl() throws Exception {
    String profileName = "inconsistent_quicklinks_profile_4.json";
    QuickLinksProfileParser parser = new QuickLinksProfileParser();
    parser.parse(Resources.getResource(profileName));
  }

  @Test(expected = JsonProcessingException.class)
  public void testParseInconsistentProfile_misspelledFilerDefinition() throws Exception {
    String profileName = "inconsistent_quicklinks_profile_3.json";
    QuickLinksProfileParser parser = new QuickLinksProfileParser();
    parser.parse(Resources.getResource(profileName));
  }

}