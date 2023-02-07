/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.drill.exec.udfs;

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.Map;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestUserAgentFunctions extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testParseUserAgentString() throws Exception {
    String query = "SELECT t1.ua.DeviceClass AS DeviceClass,\n" +
      "t1.ua.DeviceName AS DeviceName,\n" +
      "t1.ua.DeviceBrand AS DeviceBrand,\n" +
      "t1.ua.DeviceCpuBits AS DeviceCpuBits,\n" +
      "t1.ua.OperatingSystemClass AS OperatingSystemClass,\n" +
      "t1.ua.OperatingSystemName AS OperatingSystemName,\n" +
      "t1.ua.OperatingSystemVersion AS OperatingSystemVersion,\n" +
      "t1.ua.OperatingSystemVersionMajor AS OperatingSystemVersionMajor,\n" +
      "t1.ua.OperatingSystemNameVersion AS OperatingSystemNameVersion,\n" +
      "t1.ua.OperatingSystemNameVersionMajor AS OperatingSystemNameVersionMajor,\n" +
      "t1.ua.LayoutEngineClass AS LayoutEngineClass,\n" +
      "t1.ua.LayoutEngineName AS LayoutEngineName,\n" +
      "t1.ua.LayoutEngineVersion AS LayoutEngineVersion,\n" +
      "t1.ua.LayoutEngineVersionMajor AS LayoutEngineVersionMajor,\n" +
      "t1.ua.LayoutEngineNameVersion AS LayoutEngineNameVersion,\n" +
      "t1.ua.LayoutEngineBuild AS LayoutEngineBuild,\n" +
      "t1.ua.AgentClass AS AgentClass,\n" +
      "t1.ua.AgentName AS AgentName,\n" +
      "t1.ua.AgentVersion AS AgentVersion,\n" +
      "t1.ua.AgentVersionMajor AS AgentVersionMajor,\n" +
      "t1.ua.AgentNameVersionMajor AS AgentNameVersionMajor,\n" +
      "t1.ua.AgentLanguage AS AgentLanguage,\n" +
      "t1.ua.AgentLanguageCode AS AgentLanguageCode,\n" +
      "t1.ua.AgentSecurity AS AgentSecurity\n" +
      "FROM (SELECT parse_user_agent('Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.11) Gecko/20071127 Firefox/2.0.0.11') AS ua FROM (values(1))) AS t1";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("DeviceClass", "DeviceName", "DeviceBrand", "DeviceCpuBits", "OperatingSystemClass", "OperatingSystemName", "OperatingSystemVersion", "OperatingSystemVersionMajor", "OperatingSystemNameVersion", "OperatingSystemNameVersionMajor", "LayoutEngineClass", "LayoutEngineName", "LayoutEngineVersion", "LayoutEngineVersionMajor", "LayoutEngineNameVersion", "LayoutEngineBuild", "AgentClass", "AgentName", "AgentVersion", "AgentVersionMajor", "AgentNameVersionMajor", "AgentLanguage", "AgentLanguageCode", "AgentSecurity")
      .baselineValues("Desktop", "Desktop", "Unknown", "32", "Desktop", "Windows NT", "XP", "XP", "Windows XP", "Windows XP", "Browser", "Gecko", "1.8.1.11", "1", "Gecko 1.8.1.11", "20071127", "Browser", "Firefox", "2.0.0.11", "2", "Firefox 2", "English (United States)", "en-us", "Strong security")
      .go();
  }

  @Test
  public void testGetHostName() throws Exception {
    String query = "SELECT parse_user_agent('Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.11) Gecko/20071127 Firefox/2.0.0.11', 'AgentSecurity') AS agent FROM "
      + "(values(1))";
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("agent")
      .baselineValues("Strong security")
      .go();
  }

  @Test
  public void testEmptyFieldName() throws Exception {
    String query = "SELECT parse_user_agent('Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.11) Gecko/20071127 Firefox/2.0.0.11', '') AS agent FROM " + "(values" +
      "(1))";
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("agent")
      .baselineValues("Unknown")
      .go();
  }

  @Test
  public void testNullUserAgent() throws Exception {
    String query = "SELECT parse_user_agent(CAST(null as VARCHAR)) AS agent FROM (values(1))";
    Map<?, ?> emptyMap = Collections.emptyMap();
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("agent")
      .baselineValues(emptyMap)
      .go();
  }


  @Test
  public void testEmptyUAStringAndFieldName() throws Exception {
    String query = "SELECT parse_user_agent('', '') AS agent FROM (values(1))";
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("agent")
      .baselineValues("Unknown")
      .go();
  }

  @Test
  public void testNullUAStringAndEmptyFieldName() throws Exception {
    String query = "SELECT parse_user_agent(CAST(null as VARCHAR), '') AS agent FROM (values(1))";
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("agent")
      .baselineValues((String) null)
      .go();
  }

  @Test
  public void testNullUAStringAndNullFieldName() throws Exception {
    String query = "SELECT parse_user_agent(CAST(null as VARCHAR), CAST(null as VARCHAR)) AS agent FROM (values(1))";
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("agent")
      .baselineValues((String) null)
      .go();
  }

  @Test
  public void testNullUAStringAndFieldName() throws Exception {
    String query = "SELECT parse_user_agent(CAST(null as VARCHAR), 'AgentSecurity') AS agent FROM (values(1))";
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("agent")
      .baselineValues((String) null)
      .go();
  }

  @Test
  public void testEmptyUAString() throws Exception {
    String query = "SELECT t1.ua.AgentName AS AgentName FROM (SELECT parse_user_agent('') AS ua FROM (values(1))) as t1";

    // If the UA string is empty, all returned fields default to "Hacker"
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("AgentName")
      .baselineValues("Hacker")
      .go();
  }
}
