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
package org.apache.drill.exec.store.pcapng;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.pcap.plugin.PcapFormatConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestPcapngStatRecordReader extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));
    cluster.defineFormat("dfs", "pcapng", new PcapFormatConfig(ImmutableList.of("pcapng"), true, false));
    dirTestWatcher.copyResourceToRoot(Paths.get("pcapng/"));
  }

  @Test
  public void testStarQuery() throws Exception {
    String sql = "select * from dfs.`pcapng/example.pcapng`";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    assertEquals(3, sets.rowCount());
    sets.clear();
  }

  @Test
  public void testExplicitQuery() throws Exception {
    String sql = "select path, shb_hardware, shb_os, if_name, isb_ifrecv from dfs.`pcapng/sniff.pcapng`";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("path", MinorType.VARCHAR)
        .addNullable("shb_hardware", MinorType.VARCHAR)
        .addNullable("shb_os", MinorType.VARCHAR)
        .addNullable("if_name", MinorType.VARCHAR)
        .addNullable("isb_ifrecv", MinorType.BIGINT)
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("sniff.pcapng", "Intel(R) Core(TM) i7-6700HQ CPU @ 2.60GHz (with SSE4.2)",
            "Mac OS X 10.13.3, build 17D47 (Darwin 17.4.0)", null, null)
        .addRow("sniff.pcapng", null, null, "en0", null)
        .addRow("sniff.pcapng", null, null, null, 123)
        .build();

    assertEquals(3, sets.rowCount());
    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testLimitPushdown() throws Exception {
    String sql = "select * from dfs.`pcapng/example.pcapng` limit 2";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    assertEquals(2, sets.rowCount());
    sets.clear();
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "select count(*) from dfs.`pcapng/*.pcapng`";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();

    assertEquals("Counts should match", 6, cnt);
  }

  @Test
  public void testValidHeaders() throws Exception {
    String sql = "select * from dfs.`pcapng/sniff.pcapng`";
    RowSet sets = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("path", MinorType.VARCHAR)
        .addNullable("shb_hardware", MinorType.VARCHAR)
        .addNullable("shb_os", MinorType.VARCHAR)
        .addNullable("shb_userappl", MinorType.VARCHAR)
        .addNullable("if_name", MinorType.VARCHAR)
        .addNullable("if_description", MinorType.VARCHAR)
        .addNullable("if_ipv4addr", MinorType.VARCHAR)
        .addNullable("if_ipv6addr", MinorType.VARCHAR)
        .addNullable("if_macaddr", MinorType.VARCHAR)
        .addNullable("if_euiaddr", MinorType.VARCHAR)
        .addNullable("if_speed", MinorType.INT)
        .addNullable("if_tsresol", MinorType.INT)
        .addNullable("if_tzone", MinorType.INT)
        .addNullable("if_os", MinorType.VARCHAR)
        .addNullable("if_fcslen", MinorType.INT)
        .addNullable("if_tsoffset", MinorType.INT)
        .addNullable("ns_dnsname", MinorType.VARCHAR)
        .addNullable("ns_dnsip4addr", MinorType.VARCHAR)
        .addNullable("ns_dnsip6addr", MinorType.VARCHAR)
        .addNullable("isb_starttime", MinorType.TIMESTAMP)
        .addNullable("isb_endtime", MinorType.TIMESTAMP)
        .addNullable("isb_ifrecv", MinorType.BIGINT)
        .addNullable("isb_ifdrop", MinorType.BIGINT)
        .addNullable("isb_filteraccept", MinorType.BIGINT)
        .addNullable("isb_osdrop", MinorType.BIGINT)
        .addNullable("isb_usrdeliv", MinorType.BIGINT)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema).build();
    new RowSetComparison(expected).verifyAndClearAll(sets);
  }
}
