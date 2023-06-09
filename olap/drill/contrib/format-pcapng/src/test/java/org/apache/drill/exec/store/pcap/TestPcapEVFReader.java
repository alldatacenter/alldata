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

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.exec.store.pcap.plugin.PcapFormatConfig;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.time.LocalDateTime;
import java.time.Month;


@Category(RowSetTests.class)
public class TestPcapEVFReader extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));
    cluster.defineFormat("cp", "sample", new PcapFormatConfig(null, true, false));
  }

  @Test
  public void testStarQuery() throws Exception {
    String sql = "SELECT * FROM cp.`pcap/synscan.pcap` LIMIT 1";

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("type", "packet_timestamp", "timestamp_micro", "network", "src_mac_address", "dst_mac_address", "dst_ip", "src_ip", "src_port", "dst_port", "packet_length",
        "tcp_session", "tcp_sequence", "tcp_ack", "tcp_flags", "tcp_parsed_flags", "tcp_flags_ns", "tcp_flags_cwr", "tcp_flags_ece", "tcp_flags_ece_ecn_capable", "tcp_flags_ece_congestion_experienced", "tcp_flags_urg", "tcp_flags_ack", "tcp_flags_psh", "tcp_flags_rst", "tcp_flags_syn", "tcp_flags_fin", "data", "is_corrupt")
      .baselineValues("TCP", LocalDateTime.of(2010, Month.JULY, 4, 20, 24, 16, 274000000), 1278275056274870L, 1, "00:25:B3:BF:91:EE", "00:26:0B:31:07:33",
        "64.13.134.52", "172.16.0.8", 36050, 443, 58,
        317740574511239903L, -581795048, false, 2,"SYN", false, false, false, false, false, false, false, false, false, true, false,  "[]", false)
      .go();
  }

  @Test
  public void testExplicitAllQuery() throws Exception {
    String sql = "SELECT type AS packet_type, packet_timestamp, timestamp_micro, network, src_mac_address, dst_mac_address, dst_ip, src_ip, src_port, dst_port, " +
      "packet_length, tcp_session, " +
      "tcp_sequence, tcp_ack, tcp_flags," +
      " tcp_parsed_flags, tcp_flags_ns, tcp_flags_cwr, tcp_flags_ece, tcp_flags_ece_ecn_capable, tcp_flags_ece_congestion_experienced, tcp_flags_urg, tcp_flags_ack, tcp_flags_psh, tcp_flags_rst, tcp_flags_syn," +
      " tcp_flags_fin, data, is_corrupt FROM cp.`pcap/synscan.pcap` LIMIT 1";

    testBuilder()
      .sqlQuery(sql)
      .ordered()
      .baselineColumns("packet_type", "packet_timestamp", "timestamp_micro", "network", "src_mac_address", "dst_mac_address", "dst_ip", "src_ip", "src_port", "dst_port", "packet_length", "tcp_session", "tcp_sequence", "tcp_ack", "tcp_flags", "tcp_parsed_flags", "tcp_flags_ns", "tcp_flags_cwr", "tcp_flags_ece", "tcp_flags_ece_ecn_capable", "tcp_flags_ece_congestion_experienced", "tcp_flags_urg", "tcp_flags_ack", "tcp_flags_psh", "tcp_flags_rst", "tcp_flags_syn", "tcp_flags_fin", "data", "is_corrupt")
      .baselineValues("TCP", LocalDateTime.of(2010, Month.JULY, 4, 20, 24, 16, 274000000), 1278275056274870L, 1, "00:25:B3:BF:91:EE", "00:26:0B:31:07:33",
         "64.13.134.52", "172.16.0.8", 36050, 443, 58,
        317740574511239903L, -581795048, false, 2,"SYN", false, false, false, false, false, false, false, false, false, true, false,  "[]", false)
      .go();
  }

  @Test
  public void testAggregateQuery() throws Exception {
    String sql = "SELECT is_corrupt, COUNT(*) as packet_count FROM cp.`pcap/testv1.pcap` GROUP BY is_corrupt ORDER BY packet_count DESC";

    testBuilder()
      .sqlQuery(sql)
      .ordered()
      .baselineColumns("is_corrupt", "packet_count")
      .baselineValues(false, 6984L)
      .baselineValues(true, 16L)
      .go();
  }

  @Test
  public void testArpPcapFile() throws Exception {
    String sql = "SELECT src_ip, dst_ip FROM cp.`pcap/arpWithNullIP.pcap` WHERE src_port=1";
    testBuilder()
      .sqlQuery(sql)
      .ordered()
      .baselineColumns("src_ip", "dst_ip")
      .baselineValues((String)null, (String)null)
      .baselineValues((String)null, (String)null)
      .baselineValues((String)null, (String)null)
      .baselineValues((String)null, (String)null)
      .baselineValues((String)null, (String)null)
      .baselineValues((String)null, (String)null)
      .go();
  }
}
