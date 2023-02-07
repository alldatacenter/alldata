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

package org.apache.drill.exec.store.splunk;

import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class})
public class SplunkIndexesTest extends SplunkBaseTest {

  @Test
  public void testGetSplunkIndexes() throws Exception {
    String sql = "SHOW TABLES IN `splunk`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("TABLE_SCHEMA", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("TABLE_NAME", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("splunk", "summary")
      .addRow("splunk", "splunklogger")
      .addRow("splunk", "_thefishbucket")
      .addRow("splunk", "_audit")
      .addRow("splunk", "_internal")
      .addRow("splunk", "_introspection")
      .addRow("splunk", "main")
      .addRow("splunk", "history")
      .addRow("splunk", "spl")
      .addRow("splunk", "_telemetry")
      .build();

    RowSetUtilities.verify(expected, results);
  }
}
