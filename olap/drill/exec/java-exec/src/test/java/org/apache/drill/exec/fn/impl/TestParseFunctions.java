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
package org.apache.drill.exec.fn.impl;

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import static org.apache.drill.test.TestBuilder.mapOf;

@Category(SqlFunctionTest.class)
public class TestParseFunctions extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    generateDataSource();
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  private static void generateDataSource() throws Exception {
    // Contents of the generated file:
    /*
      {"url": "ftp://somewhere.com:3190/someFile?a=12&b=someValue"}
      {"url": null}
      {"url": "http://someUrl?p1=v1&p2=v=2&"}
     */
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(
        new File(dirTestWatcher.getRootDir(), "nullable_urls.json")))) {
      String[] urls = {"\"ftp://somewhere.com:3190/someFile?a=12&b=someValue\"", null, "\"http://someUrl?p1=v1&p2=v=2&\""};
      for (String url : urls) {
        String entry = String.format("{ \"url\": %s}\n", url);
        writer.write(entry);
      }
    }
  }

  @Test
  public void testParseQueryFunction() throws Exception {
    testBuilder()
        .sqlQuery("select parse_query(url) parameters from dfs.`nullable_urls.json`")
        .unOrdered()
        .baselineColumns("parameters")
        .baselineValues(mapOf("a", "12", "b", "someValue"))
        .baselineValues(mapOf())
        .baselineValues(mapOf("p1", "v1", "p2", "v=2"))
        .go();
  }

  @Test
  public void testParseUrlFunction() throws Exception {
    testBuilder()
        .sqlQuery("select parse_url(url) data from dfs.`nullable_urls.json`")
        .unOrdered()
        .baselineColumns("data")
        .baselineValues(mapOf("protocol", "ftp", "authority", "somewhere.com:3190", "host", "somewhere.com",
            "path", "/someFile", "query", "a=12&b=someValue", "filename", "/someFile?a=12&b=someValue", "port", 3190))
        .baselineValues(mapOf())
        .baselineValues(mapOf("protocol", "http", "authority", "someUrl", "host", "someUrl", "path", "",
            "query", "p1=v1&p2=v=2&", "filename", "?p1=v1&p2=v=2&"))
        .go();
  }
}
