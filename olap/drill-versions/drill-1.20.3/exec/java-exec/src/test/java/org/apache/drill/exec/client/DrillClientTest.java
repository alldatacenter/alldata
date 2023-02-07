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

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.DrillSystemTestBase;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * The unit test case will read a physical plan in json format. The physical plan contains a "trace" operator,
 * which will produce a dump file.  The dump file will be input into DumpCat to test query mode and batch mode.
 */

public class DrillClientTest extends DrillSystemTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillClientTest.class);

  private final DrillConfig config = DrillConfig.create();

  @Test
  public void testParseAndVerifyEndpointsSingleDrillbitIp() throws Exception {

    // Test with single drillbit ip
    final String drillBitConnection = "10.10.100.161";
    final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
      (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
    final CoordinationProtos.DrillbitEndpoint endpoint = endpointsList.get(0);
    assertEquals(endpointsList.size(), 1);
    assertEquals(endpoint.getAddress(), drillBitConnection);
    assertEquals(endpoint.getUserPort(), config.getInt(ExecConstants.INITIAL_USER_PORT));
  }

  @Test
  public void testParseAndVerifyEndpointsSingleDrillbitIpPort() throws Exception {

    // Test with single drillbit ip:port
    final String drillBitConnection = "10.10.100.161:5000";
    final String[] ipAndPort = drillBitConnection.split(":");
    final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
      (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
    assertEquals(endpointsList.size(), 1);

    final CoordinationProtos.DrillbitEndpoint endpoint = endpointsList.get(0);
    assertEquals(endpoint.getAddress(), ipAndPort[0]);
    assertEquals(endpoint.getUserPort(), Integer.parseInt(ipAndPort[1]));
  }

  @Test
  public void testParseAndVerifyEndpointsMultipleDrillbitIp() throws Exception {

    // Test with multiple drillbit ip
    final String drillBitConnection = "10.10.100.161,10.10.100.162";
    final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
      (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
    assertEquals(endpointsList.size(), 2);

    CoordinationProtos.DrillbitEndpoint endpoint = endpointsList.get(0);
    assertEquals(endpoint.getAddress(), "10.10.100.161");
    assertEquals(endpoint.getUserPort(), config.getInt(ExecConstants.INITIAL_USER_PORT));

    endpoint = endpointsList.get(1);
    assertEquals(endpoint.getAddress(), "10.10.100.162");
    assertEquals(endpoint.getUserPort(), config.getInt(ExecConstants.INITIAL_USER_PORT));
  }

  @Test
  public void testParseAndVerifyEndpointsMultipleDrillbitIpPort() throws Exception {

    // Test with multiple drillbit ip:port
    final String drillBitConnection = "10.10.100.161:5000,10.10.100.162:5000";
    final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
      (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
    assertEquals(endpointsList.size(), 2);

    CoordinationProtos.DrillbitEndpoint endpoint = endpointsList.get(0);
    assertEquals(endpoint.getAddress(), "10.10.100.161");
    assertEquals(endpoint.getUserPort(), 5000);

    endpoint = endpointsList.get(1);
    assertEquals(endpoint.getAddress(), "10.10.100.162");
    assertEquals(endpoint.getUserPort(), 5000);
  }

  @Test
  public void testParseAndVerifyEndpointsMultipleDrillbitIpPortIp() throws Exception {

    // Test with multiple drillbit with mix of ip:port and ip
    final String drillBitConnection = "10.10.100.161:5000,10.10.100.162";
    final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
      (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
    assertEquals(endpointsList.size(), 2);

    CoordinationProtos.DrillbitEndpoint endpoint = endpointsList.get(0);
    assertEquals(endpoint.getAddress(), "10.10.100.161");
    assertEquals(endpoint.getUserPort(), 5000);

    endpoint = endpointsList.get(1);
    assertEquals(endpoint.getAddress(), "10.10.100.162");
    assertEquals(endpoint.getUserPort(), config.getInt(ExecConstants.INITIAL_USER_PORT));
  }

  @Test
  public void testParseAndVerifyEndpointsEmptyString() throws Exception {

    // Test with empty string
    final String drillBitConnection = "";
    try {
      final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
        (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
      fail();
    } catch (InvalidConnectionInfoException e) {
      logger.error("Exception ", e);
    }
  }

  @Test
  public void testParseAndVerifyEndpointsOnlyPortDelim() throws Exception{
    // Test to check when connection string only has delimiter
    final String drillBitConnection = ":";

    try {
      final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
        (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
      fail();
    } catch (InvalidConnectionInfoException e) {
      logger.error("Exception ", e);
    }
  }

  @Test
  public void testParseAndVerifyEndpointsWithOnlyPort() throws Exception{
    // Test to check when connection string has port with no ip
    final String drillBitConnection = ":5000";

    try {
      final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
        (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
      fail();
    } catch (InvalidConnectionInfoException e) {
      logger.error("Exception ", e);
    }
  }

  @Test
  public void testParseAndVerifyEndpointsWithMultiplePort() throws Exception{
    // Test to check when connection string has multiple port with one ip
    final String drillBitConnection = "10.10.100.161:5000:6000";

    try {
      final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
        (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
      fail();
    } catch (InvalidConnectionInfoException e) {
      logger.error("Exception ", e);
    }
  }

  @Test
  public void testParseAndVerifyEndpointsIpWithDelim() throws Exception{
    // Test to check when connection string has ip with delimiter
    final String drillBitConnection = "10.10.100.161:";
    final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
        (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
    final CoordinationProtos.DrillbitEndpoint endpoint = endpointsList.get(0);
    assertEquals(endpointsList.size(), 1);
    assertEquals(endpoint.getAddress(), "10.10.100.161");
    assertEquals(endpoint.getUserPort(), config.getInt(ExecConstants.INITIAL_USER_PORT));
  }

  @Test
  public void testParseAndVerifyEndpointsIpWithEmptyPort() throws Exception{
    // Test to check when connection string has ip with delimiter
    final String drillBitConnection = "10.10.100.161:    ";
    final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
      (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
    final CoordinationProtos.DrillbitEndpoint endpoint = endpointsList.get(0);
    assertEquals(endpointsList.size(), 1);
    assertEquals(endpoint.getAddress(), "10.10.100.161");
    assertEquals(endpoint.getUserPort(), config.getInt(ExecConstants.INITIAL_USER_PORT));
  }

  @Test
  public void testParseAndVerifyEndpointsIpWithSpaces() throws Exception{
    // Test to check when connection string has spaces in between
    final String drillBitConnection = "10.10.100.161 : 5000, 10.10.100.162:6000    ";
    final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
      (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));

    CoordinationProtos.DrillbitEndpoint endpoint = endpointsList.get(0);
    assertEquals(endpointsList.size(), 2);
    assertEquals(endpoint.getAddress(), "10.10.100.161");
    assertEquals(endpoint.getUserPort(), 5000);

    endpoint = endpointsList.get(1);
    assertEquals(endpoint.getAddress(), "10.10.100.162");
    assertEquals(endpoint.getUserPort(), 6000);
  }

  @Test
  public void testParseAndVerifyEndpointsStringWithSpaces() throws Exception{
    // Test to check when connection string has ip with delimiter
    final String drillBitConnection = "10.10.100.161 : 5000";
    final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
      (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
    final CoordinationProtos.DrillbitEndpoint endpoint = endpointsList.get(0);
    assertEquals(endpointsList.size(), 1);
    assertEquals(endpoint.getAddress(), "10.10.100.161");
    assertEquals(endpoint.getUserPort(), 5000);
  }

  @Test
  public void testParseAndVerifyEndpointsNonNumericPort() throws Exception{
    // Test to check when connection string has non-numeric port
    final String drillBitConnection = "10.10.100.161:5ab0";

    try{
      final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
        (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
      fail();
    } catch (InvalidConnectionInfoException e) {
      logger.error("Exception ", e);
    }
  }

  @Test
  public void testParseAndVerifyEndpointsOnlyDelim() throws Exception{
    // Test to check when connection string has only delimiter coma
    final String drillBitConnection = "  ,   ";

    try{
      final List<CoordinationProtos.DrillbitEndpoint> endpointsList = DrillClient.parseAndVerifyEndpoints
        (drillBitConnection, config.getString(ExecConstants.INITIAL_USER_PORT));
      fail();
    } catch (InvalidConnectionInfoException e) {
      logger.error("Exception ", e);
    }
  }
}
