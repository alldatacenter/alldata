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

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestNetworkFunctions extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testInetAton() throws Exception {
    String query = "select inet_aton('192.168.0.1') as inet from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("inet").baselineValues(3232235521L).go();

    query = "select inet_aton('192.168.0') as inet from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("inet").baselineValues((Long) null).go();

    query = "select inet_aton('') as inet from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("inet").baselineValues((Long) null).go();

    query = "select inet_aton(cast(null as varchar)) as inet from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("inet").baselineValues((Long) null).go();
  }

  @Test
  public void testInetNtoa() throws Exception {
    String query = "select inet_ntoa(3232235521) as inet from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("inet").baselineValues("192.168.0.1").go();

    query = "select inet_ntoa(cast(null as int)) as inet from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("inet").baselineValues((String) null).go();
  }

  @Test
  public void testInNetwork() throws Exception {
    final String query = "select in_network('192.168.0.1', '192.168.0.0/28') as in_net FROM (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("in_net").baselineValues(true).go();
  }

  @Test
  public void testNotInNetwork() throws Exception {
    String query = "select in_network('10.10.10.10', '192.168.0.0/28') as in_net from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("in_net").baselineValues(false).go();

    query = "select in_network('10.10.10.10', '') as in_net from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("in_net").baselineValues(false).go();

    query = "select in_network('', '192.168.0.0/28') as in_net from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("in_net").baselineValues(false).go();

    query = "select in_network(cast(null as varchar), '192.168.0.0/28') as in_net from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("in_net").baselineValues((Boolean) null).go();

    query = "select in_network('10.10.10.10', cast(null as varchar)) as in_net from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("in_net").baselineValues((Boolean) null).go();
  }

  @Test
  public void testBroadcastAddress() throws Exception {
    String query = "select broadcast_address('192.168.0.0/28') as broadcast_address from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("broadcast_address").baselineValues("192.168.0.15").go();

    query = "select broadcast_address('192.168.') as broadcast_address from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("broadcast_address").baselineValues((String) null).go();

    query = "select broadcast_address('') as broadcast_address from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("broadcast_address").baselineValues((String) null).go();
  }

  @Test
  public void testNetmask() throws Exception {
    String query = "select netmask('192.168.0.0/28') as netmask from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("netmask").baselineValues("255.255.255.240").go();

    query = "select netmask('192222') as netmask from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("netmask").baselineValues((String) null).go();

    query = "select netmask('') as netmask from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("netmask").baselineValues((String) null).go();
  }

  @Test
  public void testLowAddress() throws Exception {
    String query = "select low_address('192.168.0.0/28') as low from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("low").baselineValues("192.168.0.1").go();

    query = "select low_address('192.168.0.0/') as low from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("low").baselineValues((String) null).go();

    query = "select low_address('192.168.0.0/') as low from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("low").baselineValues((String) null).go();
  }

  @Test
  public void testHighAddress() throws Exception {
    String query = "select high_address('192.168.0.0/28') as high from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("high").baselineValues("192.168.0.14").go();

    query = "select high_address('192.168.0.') as high from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("high").baselineValues((String) null).go();

    query = "select high_address('') as high from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("high").baselineValues((String) null).go();
  }

  @Test
  public void testEncodeUrl() throws Exception {
    final String query = "SELECT url_encode('http://www.test.com/login.php?username=Charles&password=12345') AS encoded_url FROM (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("encoded_url").baselineValues("http%3A%2F%2Fwww.test.com%2Flogin.php%3Fusername%3DCharles%26password%3D12345").go();
  }

  @Test
  public void testDecodeUrl() throws Exception {
    final String query = "SELECT url_decode('http%3A%2F%2Fwww.test.com%2Flogin.php%3Fusername%3DCharles%26password%3D12345') AS decoded_url FROM (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("decoded_url").baselineValues("http://www.test.com/login.php?username=Charles&password=12345").go();
  }

  @Test
  public void testNotPrivateIP() throws Exception {
    String query = "select is_private_ip('8.8.8.8') as is_private_ip from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_private_ip").baselineValues(false).go();

    query = "select is_private_ip('8.A.8') as is_private_ip from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_private_ip").baselineValues(false).go();

    query = "select is_private_ip('192.168') as is_private_ip from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_private_ip").baselineValues(false).go();

    query = "select is_private_ip('') as is_private_ip from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_private_ip").baselineValues(false).go();

    query = "select is_private_ip(cast(null as varchar)) as is_private_ip from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_private_ip").baselineValues((Boolean) null).go();
  }

  @Test
  public void testPrivateIP() throws Exception {
    final String query = "SELECT is_private_ip('192.168.0.1') AS is_private_ip FROM (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_private_ip").baselineValues(true).go();
  }

  @Test
  public void testNotValidIP() throws Exception {
    String query = "select is_valid_IP('258.257.234.23') as is_valid_IP from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP").baselineValues(false).go();

    query = "select is_valid_IP('258.257.2') as is_valid_IP from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP").baselineValues(false).go();

    query = "select is_valid_IP('') as is_valid_IP from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP").baselineValues(false).go();

    query = "select is_valid_IP(cast(null as varchar)) as is_valid_IP from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP").baselineValues((Boolean) null).go();
  }

  @Test
  public void testIsValidIP() throws Exception {
    final String query = "SELECT is_valid_IP('10.10.10.10') AS is_valid_IP FROM (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP").baselineValues(true).go();
  }

  @Test
  public void testNotValidIPv4() throws Exception {
    String query = "select is_valid_IPv4('192.168.0.257') as is_valid_IP4 from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP4").baselineValues(false).go();

    query = "select is_valid_IPv4('192123') as is_valid_IP4 from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP4").baselineValues(false).go();

    query = "select is_valid_IPv4('') as is_valid_IP4 from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP4").baselineValues(false).go();

    query = "select is_valid_IPv4(cast(null as varchar)) as is_valid_IP4 from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP4").baselineValues((Boolean) null).go();
  }

  @Test
  public void testIsValidIPv4() throws Exception {
    final String query = "SELECT is_valid_IPv4( '192.168.0.1') AS is_valid_IP4 FROM (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP4").baselineValues(true).go();
  }

  @Test
  public void testIsValidIPv6() throws Exception {
    final String query = "SELECT is_valid_IPv6('1050:0:0:0:5:600:300c:326b') AS is_valid_IP6 FROM (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP6").baselineValues(true).go();
  }

  @Test
  public void testNotValidIPv6() throws Exception {
    String query = "select is_valid_IPv6('1050:0:0:0:5:600:300c:326g') as is_valid_IP6 from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP6").baselineValues(false).go();

    query = "select is_valid_IPv6('1050:0:0:0:5:600_AAA') as is_valid_IP6 from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP6").baselineValues(false).go();

    query = "select is_valid_IPv6('') as is_valid_IP6 from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP6").baselineValues(false).go();

    query = "select is_valid_IPv6(cast(null as varchar)) as is_valid_IP6 from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("is_valid_IP6").baselineValues((Boolean) null).go();
  }

  @Test
  public void testAddressCount() throws Exception {
    String query = "select address_count('192.168.0.1/30') as address_count from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("address_count").baselineValues(2L).go();

    query = "select address_count('192.168') as address_count from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("address_count").baselineValues((Long) null).go();

    query = "select address_count('192.168.0.1/100') as address_count from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("address_count").baselineValues((Long) null).go();

    query = "select address_count('') as address_count from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("address_count").baselineValues((Long) null).go();
  }

}