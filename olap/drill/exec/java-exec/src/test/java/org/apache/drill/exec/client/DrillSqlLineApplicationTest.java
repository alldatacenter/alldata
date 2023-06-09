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
package org.apache.drill.exec.client;

import org.apache.drill.common.util.DrillVersionInfo;
import org.apache.drill.test.BaseTest;
import org.junit.BeforeClass;
import org.junit.Test;
import sqlline.Application;
import sqlline.CommandHandler;
import sqlline.OutputFormat;
import sqlline.SqlLine;
import sqlline.SqlLineOpts;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class DrillSqlLineApplicationTest extends BaseTest {

  private static Application application;

  @BeforeClass
  public static void init() {
    application = new DrillSqlLineApplication(
        "drill-sqlline.conf",
        "drill-sqlline-test-override.conf");
  }

  @Test
  public void testInfoMessage() {
    String infoMessage = application.getInfoMessage();
    assertThat(infoMessage, containsString("\"All code is guilty until proven innocent.\""));
  }

  @Test
  public void testVersion() {
    assertEquals(DrillVersionInfo.getVersion(), application.getVersion());
  }

  @Test
  public void testDrivers() {
    Collection<String> drivers = application.allowedDrivers();
    assertEquals(1L, drivers.size());
    assertEquals("org.apache.drill.jdbc.Driver", drivers.iterator().next());
  }

  @Test
  public void testOutputFormats() {
    SqlLine sqlLine = new SqlLine();
    Map<String, OutputFormat> outputFormats = application.getOutputFormats(sqlLine);
    assertEquals(sqlLine.getOutputFormats(), outputFormats);
  }

  @Test
  public void testConnectionUrlExamples() {
    Collection<String> examples = application.getConnectionUrlExamples();
    assertEquals(1L, examples.size());
    assertEquals("jdbc:drill:zk=local", examples.iterator().next());
  }

  @Test
  public void testCommandHandlers() {
    SqlLine sqlLine = new SqlLine();
    Collection<CommandHandler> commandHandlers = application.getCommandHandlers(sqlLine);
    List<String> excludedCommands = Arrays.asList("describe", "indexes");
    List<CommandHandler> matchingCommands = commandHandlers.stream()
        .filter(c -> c.getNames().stream()
            .anyMatch(excludedCommands::contains))
        .collect(Collectors.toList());
    assertTrue(matchingCommands.isEmpty());
  }

  @Test
  public void testOpts() {
    SqlLine sqlLine = new SqlLine();
    SqlLineOpts opts = application.getOpts(sqlLine);
    assertFalse(opts.getIncremental());
    assertEquals("TRANSACTION_NONE", opts.getIsolation());
    assertEquals(80, opts.getMaxColumnWidth());
    assertEquals(200, opts.getTimeout());
    assertEquals("obsidian", opts.getColorScheme());
    assertEquals("null", opts.getNullValue());
  }

  @Test
  public void testEmptyConfig() {
    DrillSqlLineApplication application = new DrillSqlLineApplication(
        "missing.conf", "missing_override.conf");
    assertTrue(application.getConfig().isEmpty());
  }

  @Test
  public void testConfigWithoutOverride() {
    DrillSqlLineApplication application = new DrillSqlLineApplication(
        "drill-sqlline.conf", "missing_override.conf");
    assertFalse(application.getConfig().isEmpty());
  }

}
