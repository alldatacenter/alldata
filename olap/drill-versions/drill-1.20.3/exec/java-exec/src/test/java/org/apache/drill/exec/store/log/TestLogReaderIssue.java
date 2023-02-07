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
package org.apache.drill.exec.store.log;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.easy.text.compliant.BaseCsvTest;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.QueryBuilder.QuerySummary;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(EvfTest.class)
public class TestLogReaderIssue extends BaseCsvTest {

  private static String[] mock_issue7853 = {
      "h2 2021-01-23T23:55:00.664544Z app/alb/abc123",
      "h2 2021-01-23T23:55:00.666170Z app/alb/abc123"
  };

  private static String regex_issue7853 = "(\\w{2,})" // type field
                                            + " " // white space
                                              + "(\\d{4}-\\d{2}-\\w{5}:\\d{2}:\\d{2}.\\d{6}\\w)" // time field
                                                + ".*?";

  @BeforeClass
  public static void setup() throws Exception {
    BaseCsvTest.setup(false, false);

    File rootDir = new File(testDir, PART_DIR);
    rootDir.mkdir();
    buildFile(new File(rootDir, "issue7853.log"), mock_issue7853);
    buildFile(new File(rootDir, "issue7853.log2"), mock_issue7853);

    Map<String, FormatPluginConfig> formats = new HashMap<>();
    formats.put("log", issue7853Config());
    formats.put("log2", issue7853UseValidDatetimeFormatConfig());
    cluster.defineFormats("dfs", formats);
  }

  // DRILL-7853
  private static LogFormatConfig issue7853Config() {
    List<LogFormatField> schema = Lists.newArrayList(
        new LogFormatField("type", "VARCHAR"),
        new LogFormatField("time", "TIMESTAMP", "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")); // valid
    return new LogFormatConfig(regex_issue7853, "log", null, schema);
  }

  // DRILL-7853
  private static LogFormatConfig issue7853UseValidDatetimeFormatConfig() {
    List<LogFormatField> schema = Lists.newArrayList(
        new LogFormatField("type", "VARCHAR"),
        new LogFormatField("time", "TIMESTAMP", "yyyy-MM-dd''T''HH:mm:ss.SSSSSSZ")); // invalid
    return new LogFormatConfig(regex_issue7853, "log2", null, schema);
  }

  @Test
  public void testIssue7853UseValidDatetimeFormat() throws Exception {
    String sql = "SELECT type, `time` FROM `dfs.data`.`root/issue7853.log`";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("type", MinorType.VARCHAR)
        .addNullable("time", MinorType.TIMESTAMP)
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("h2", 1611446100664L)
        .addRow("h2", 1611446100666L)
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testIssue7853() throws Exception {
    thrownException.expect(UserRemoteException.class);
    thrownException.expectMessage("is not valid for type TIMESTAMP");
    String sql = "SELECT type, `time` FROM `dfs.data`.`root/issue7853.log2`";
    QuerySummary result = client.queryBuilder().sql(sql).run();
    assertEquals(2, result.recordCount());
  }
}
