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

package org.apache.drill.exec.store.pcap;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.pcap.plugin.PcapFormatConfig;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.joda.time.Period;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestSessionizePCAP extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));

    PcapFormatConfig sampleConfig = new PcapFormatConfig(null, true, true);
    cluster.defineFormat("cp", "pcap", sampleConfig);
    dirTestWatcher.copyResourceToRoot(Paths.get("pcap/"));
  }

  @Test
  public void testSessionizedStarQuery() throws Exception {
    String sql = "SELECT * FROM cp.`/pcap/http.pcap`";
    String dataFromRemote = readAFileIntoString(dirTestWatcher.getRootDir().getAbsolutePath() + "/pcap/dataFromRemote.txt");

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("src_ip", TypeProtos.MinorType.VARCHAR)
      .addNullable("dst_ip", TypeProtos.MinorType.VARCHAR)
      .addNullable("src_port", TypeProtos.MinorType.INT)
      .addNullable("dst_port", TypeProtos.MinorType.INT)
      .addNullable("src_mac_address", TypeProtos.MinorType.VARCHAR)
      .addNullable("dst_mac_address", TypeProtos.MinorType.VARCHAR)
      .addNullable("session_start_time", TypeProtos.MinorType.TIMESTAMP)
      .addNullable("session_end_time", TypeProtos.MinorType.TIMESTAMP)
      .addNullable("session_duration", TypeProtos.MinorType.INTERVAL)
      .addNullable("total_packet_count", TypeProtos.MinorType.INT)
      .addNullable("data_volume_from_origin", TypeProtos.MinorType.INT)
      .addNullable("data_volume_from_remote", TypeProtos.MinorType.INT)
      .addNullable("packet_count_from_origin", TypeProtos.MinorType.INT)
      .addNullable("packet_count_from_remote", TypeProtos.MinorType.INT)
      .addNullable("connection_time", TypeProtos.MinorType.INTERVAL)
      .addNullable("tcp_session", TypeProtos.MinorType.BIGINT)
      .addNullable("is_corrupt", TypeProtos.MinorType.BIT)
      .addNullable("data_from_originator", TypeProtos.MinorType.VARCHAR)
      .addNullable("data_from_remote", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(
        "145.254.160.237",
        "65.208.228.223",
        3372, 80,
        "00:00:01:00:00:00",
        "FE:FF:20:00:01:00",
        1084443427311L,
        1084443445216L,
        Period.parse("PT17.905S"), 31,
        437,18000,14, 17,
        Period.parse("PT0.911S"),
        -789689725566200012L, false,
        "r-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.6) Gecko/20040113..Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1..Accept-Language: en-us,en;q=0.5..Accept-Encoding: gzip,deflate..Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7..Keep-Alive: 300..Connection: keep-alive..Referer: http://www.ethereal.com/development.html....$K.@....6...6",
        dataFromRemote
        )
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testSessionizedSpecificQuery() throws Exception {
    String sql = "SELECT src_ip, dst_ip, src_port, dst_port, src_mac_address, dst_mac_address," +
      "session_start_time, session_end_time, session_duration, total_packet_count, data_volume_from_origin, data_volume_from_remote," +
      "packet_count_from_origin, packet_count_from_remote, connection_time, tcp_session, is_corrupt, data_from_originator, data_from_remote " +
      "FROM cp.`/pcap/http.pcap`";

    String dataFromRemote = readAFileIntoString(dirTestWatcher.getRootDir().getAbsolutePath() + "/pcap/dataFromRemote.txt");

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("src_ip", TypeProtos.MinorType.VARCHAR)
      .addNullable("dst_ip", TypeProtos.MinorType.VARCHAR)
      .addNullable("src_port", TypeProtos.MinorType.INT)
      .addNullable("dst_port", TypeProtos.MinorType.INT)
      .addNullable("src_mac_address", TypeProtos.MinorType.VARCHAR)
      .addNullable("dst_mac_address", TypeProtos.MinorType.VARCHAR)
      .addNullable("session_start_time", TypeProtos.MinorType.TIMESTAMP)
      .addNullable("session_end_time", TypeProtos.MinorType.TIMESTAMP)
      .addNullable("session_duration", TypeProtos.MinorType.INTERVAL)
      .addNullable("total_packet_count", TypeProtos.MinorType.INT)
      .addNullable("data_volume_from_origin", TypeProtos.MinorType.INT)
      .addNullable("data_volume_from_remote", TypeProtos.MinorType.INT)
      .addNullable("packet_count_from_origin", TypeProtos.MinorType.INT)
      .addNullable("packet_count_from_remote", TypeProtos.MinorType.INT)
      .addNullable("connection_time", TypeProtos.MinorType.INTERVAL)
      .addNullable("tcp_session", TypeProtos.MinorType.BIGINT)
      .addNullable("is_corrupt", TypeProtos.MinorType.BIT)
      .addNullable("data_from_originator", TypeProtos.MinorType.VARCHAR)
      .addNullable("data_from_remote", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(
        "145.254.160.237",
        "65.208.228.223",
        3372, 80,
        "00:00:01:00:00:00",
        "FE:FF:20:00:01:00",
        1084443427311L,
        1084443445216L,
        Period.parse("PT17.905S"), 31,
        437,18000,14, 17,
        Period.parse("PT0.911S"),
        -789689725566200012L, false,
        "r-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.6) Gecko/20040113..Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1..Accept-Language: en-us,en;q=0.5..Accept-Encoding: gzip,deflate..Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7..Keep-Alive: 300..Connection: keep-alive..Referer: http://www.ethereal.com/development.html....$K.@....6...6",
        dataFromRemote
      )
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "SELECT COUNT(*) FROM cp.`/pcap/http.pcap`";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();
    assertEquals("Counts should match", 1L, cnt);
  }

  /**
   * Helper function to read a file into a String.
   * @param filePath Input file which is to be read into a String
   * @return String The text content of the file.
   * @throws IOException If the file is unreachable or unreadable, throw IOException.
   */
  private static String readAFileIntoString(String filePath) throws IOException {
    return new String(Files.readAllBytes(Paths.get(filePath)));
  }
}
