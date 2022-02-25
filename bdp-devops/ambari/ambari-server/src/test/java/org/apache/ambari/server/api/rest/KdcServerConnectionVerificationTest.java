/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api.rest;

import static org.apache.ambari.server.KdcServerConnectionVerification.ConnectionProtocol.TCP;
import static org.apache.ambari.server.KdcServerConnectionVerification.ConnectionProtocol.UDP;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.apache.ambari.server.KdcServerConnectionVerification;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.directory.kerberos.client.KdcConfig;
import org.apache.directory.kerberos.client.KdcConnection;
import org.apache.directory.kerberos.client.TgTicket;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Test for {@link KdcServerConnectionVerification}
 */
public class KdcServerConnectionVerificationTest  {

  private Configuration configuration;

  private static final int KDC_TEST_PORT = 8090;

  @Before
  public void before() throws Exception {
    Properties configProps = new Properties();
    configProps.setProperty(Configuration.KDC_PORT.getKey(), Integer.toString(KDC_TEST_PORT));
    configuration = new Configuration(configProps);
  }

  @Test
  public void testValidate__Fail_InvalidPort() throws Exception {
    assertFalse(new KdcServerConnectionVerification(configuration).isKdcReachable("test-host:abcd"));
  }

  @Test
  public void testValidate__Success() throws Exception {
    KdcConnection connection = createStrictMock(KdcConnection.class);

    expect(connection.getTgt("noUser@noRealm", "noPassword")).andReturn(null).once();
    replay(connection);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host:11111");
    assertTrue(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection);
  }

  @Test
  public void testValidateTCP__Successful() throws Exception {
    KdcConnection connection = createStrictMock(KdcConnection.class);

    expect(connection.getTgt("noUser@noRealm", "noPassword")).andReturn(null).once();
    replay(connection);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, TCP);
    assertTrue(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertFalse(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection);
  }

  @Test
  public void testValidateTCP__Successful2() throws Exception {
    KrbError error = createNiceMock(KrbError.class);
    expect(error.getErrorCode()).andReturn(ErrorType.KDC_ERR_C_PRINCIPAL_UNKNOWN).once();
    expect(error.getMessageType()).andReturn(KerberosMessageType.KRB_ERROR).once();

    KerberosException exception = createNiceMock(KerberosException.class);
    expect(exception.getError()).andReturn(error).once();

    KdcConnection connection = createStrictMock(KdcConnection.class);
    expect(connection.getTgt("noUser@noRealm", "noPassword")).andThrow(exception);

    replay(connection, exception, error);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, TCP);
    assertTrue(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertFalse(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection, exception);
  }

  @Test
  public void testValidateTCP__Fail_UnknownException() throws Exception {
    KdcConnection connection = createStrictMock(KdcConnection.class);

    expect(connection.getTgt("noUser@noRealm", "noPassword")).andThrow(
        new RuntimeException("This is a really bad exception"));
    replay(connection);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, TCP);
    assertFalse(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertFalse(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection);
  }

  @Test
  public void testValidateTCP__Fail_Timeout() throws Exception {
    int timeout = 1;
    KdcConnection connection = new BlockingKdcConnection(null);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    kdcConnVerifier.setConnectionTimeout(timeout);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, TCP);
    assertFalse(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertFalse(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(timeout * 1000, kdcConfig.getTimeout());
  }

  @Test
  public void testValidateTCP__Fail_TimeoutErrorCode() throws Exception {
    KdcConnection connection = createStrictMock(KdcConnection.class);

    expect(connection.getTgt("noUser@noRealm", "noPassword")).andThrow(
        new KerberosException(ErrorType.KRB_ERR_GENERIC, "TimeOut occurred"));
    replay(connection);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, TCP);
    assertFalse(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertFalse(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection);
  }

  @Test
  public void testValidateTCP__Fail_GeneralErrorCode_NotTimeout() throws Exception {
    KrbError error = createNiceMock(KrbError.class);
    expect(error.getErrorCode()).andReturn(ErrorType.KRB_ERR_GENERIC).once();
    expect(error.getMessageType()).andReturn(KerberosMessageType.KRB_ERROR).once();

    KerberosException exception = createNiceMock(KerberosException.class);
    expect(exception.getError()).andReturn(error).once();

    KdcConnection connection = createStrictMock(KdcConnection.class);
    expect(connection.getTgt("noUser@noRealm", "noPassword")).andThrow(exception);

    replay(connection, exception, error);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, TCP);
    assertTrue(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertFalse(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection, exception);
  }

  @Test
  public void testValidateUDP__Successful() throws Exception {
    KdcConnection connection = createStrictMock(KdcConnection.class);

    expect(connection.getTgt("noUser@noRealm", "noPassword")).andReturn(null).once();
    replay(connection);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, UDP);
    assertTrue(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertTrue(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection);
  }

  @Test
  public void testValidateUDP__Successful2() throws Exception {
    KrbError error = createNiceMock(KrbError.class);
    expect(error.getErrorCode()).andReturn(ErrorType.KDC_ERR_C_PRINCIPAL_UNKNOWN).once();
    expect(error.getMessageType()).andReturn(KerberosMessageType.KRB_ERROR).once();

    KerberosException exception = createNiceMock(KerberosException.class);
    expect(exception.getError()).andReturn(error).once();

    KdcConnection connection = createStrictMock(KdcConnection.class);
    expect(connection.getTgt("noUser@noRealm", "noPassword")).andThrow(exception);

    replay(connection, exception, error);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, UDP);
    assertTrue(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertTrue(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection, exception);
  }

  @Test
  public void testValidateUDP__Fail_UnknownException() throws Exception {
    KdcConnection connection = createStrictMock(KdcConnection.class);

    expect(connection.getTgt("noUser@noRealm", "noPassword")).andThrow(
        new RuntimeException("This is a really bad exception"));
    replay(connection);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, UDP);
    assertFalse(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertTrue(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection);
  }

  @Test
  public void testValidateUDP__Fail_Timeout() throws Exception {
    int timeout = 1;
    KdcConnection connection = new BlockingKdcConnection(null);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    kdcConnVerifier.setConnectionTimeout(timeout);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, UDP);
    assertFalse(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertTrue(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(timeout * 1000, kdcConfig.getTimeout());
  }

  @Test
  public void testValidateUDP__Fail_TimeoutErrorCode() throws Exception {
    KdcConnection connection = createStrictMock(KdcConnection.class);

    expect(connection.getTgt("noUser@noRealm", "noPassword")).andThrow(
        new KerberosException(ErrorType.KRB_ERR_GENERIC, "TimeOut occurred"));
    replay(connection);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, UDP);
    assertFalse(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertTrue(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection);
  }

  @Test
  public void testValidateUDP__Fail_GeneralErrorCode_NotTimeout() throws Exception {
    KrbError error = createNiceMock(KrbError.class);
    expect(error.getErrorCode()).andReturn(ErrorType.KRB_ERR_GENERIC).once();
    expect(error.getMessageType()).andReturn(KerberosMessageType.KRB_ERROR).once();

    KerberosException exception = createNiceMock(KerberosException.class);
    expect(exception.getError()).andReturn(error).once();

    KdcConnection connection = createStrictMock(KdcConnection.class);
    expect(connection.getTgt("noUser@noRealm", "noPassword")).andThrow(exception);

    replay(connection, exception, error);

    TestKdcServerConnectionVerification kdcConnVerifier =
        new TestKdcServerConnectionVerification(configuration, connection);

    boolean result = kdcConnVerifier.isKdcReachable("test-host", 11111, UDP);
    assertTrue(result);

    KdcConfig kdcConfig = kdcConnVerifier.getConfigUsedInConnectionCreation();
    assertTrue(kdcConfig.isUseUdp());
    assertEquals("test-host", kdcConfig.getHostName());
    assertEquals(11111, kdcConfig.getKdcPort());
    assertEquals(10 * 1000, kdcConfig.getTimeout());

    verify(connection, exception);
  }

  @Test
  @Ignore
  public void testValidate__Live() throws Exception {
    KdcServerConnectionVerification kdcConnVerifier = new KdcServerConnectionVerification(configuration);
    boolean result = kdcConnVerifier.isKdcReachable("c6501:88");
    assertTrue(result);
  }


  // Test implementation which allows a mock KDC connection to be used.
  private static class TestKdcServerConnectionVerification extends KdcServerConnectionVerification {
    private KdcConnection connection;
    private KdcConfig kdcConfig = null;

    public TestKdcServerConnectionVerification(Configuration config, KdcConnection connectionMock) {
      super(config);
      connection = connectionMock;
    }

    @Override
    protected KdcConnection getKdcConnection(KdcConfig config) {
      kdcConfig = config;
      return connection;
    }

    public KdcConfig getConfigUsedInConnectionCreation() {
      return kdcConfig;
    }
  }

  /**
   * Test implementation which blocks on getTgt() for 60 seconds to facilitate timeout testing.
   */
  private static class BlockingKdcConnection extends KdcConnection {

    public BlockingKdcConnection(KdcConfig config) {
      super(config);
    }

    @Override
    public TgTicket getTgt(String principal, String password) throws Exception {
      // although it is generally a bad idea to use sleep in a unit test for a
      // timing mechanism, this is being used to simulate a timeout and should be
      // generally safe as we are not relying on this for timing other than expecting
      // that this will block longer than the timeout set on the connection validator
      // which should be set to 1 second when using this implementation.
      // We will only block the full 60 seconds in the case of a specific test failure
      // where the callable doesn't properly set the timeout on the get.
      Thread.sleep(60000);
      return null;
    }
  }
}
