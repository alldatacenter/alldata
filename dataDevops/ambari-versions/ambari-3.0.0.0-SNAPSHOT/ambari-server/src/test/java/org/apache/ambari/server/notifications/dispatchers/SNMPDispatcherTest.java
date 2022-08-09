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
package org.apache.ambari.server.notifications.dispatchers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.notifications.DispatchCallback;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.Recipient;
import org.apache.ambari.server.notifications.TargetConfigurationResult;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.VariableBinding;

public class SNMPDispatcherTest {

  private static final int DEFAULT_SNMP_PORT = 31444;

  @Test
  public void testDispatch_nullProperties() throws Exception {
    SNMPDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    Notification notification = mock(Notification.class);
    notification.Callback = mock(DispatchCallback.class);
    notification.CallbackIds = new ArrayList<>();
    dispatcher.dispatch(notification);
    verify(notification.Callback).onFailure(notification.CallbackIds);
    verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
  }

  @Test
  public void testDispatchUdpTransportMappingCrash() throws Exception {
    SNMPDispatcher dispatcher = spy(new SNMPDispatcher(DEFAULT_SNMP_PORT));
    SNMPDispatcher.SnmpVersion snmpVersion = SNMPDispatcher.SnmpVersion.SNMPv1;
    Notification notification = mock(Notification.class);
    notification.Callback = mock(DispatchCallback.class);
    notification.CallbackIds = new ArrayList<>();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "3");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "4");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    notification.DispatchProperties = properties;
    notification.Body = "body";
    notification.Subject = "subject";
    notification.Recipients = Arrays.asList(new Recipient());
    doThrow(new IOException()).when(dispatcher).sendTraps(notification, snmpVersion);
    dispatcher.dispatch(notification);
    verify(notification.Callback).onFailure(notification.CallbackIds);
    verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
    assertNull(dispatcher.getTransportMapping());
  }

  @Test
  public void testDispatch_notDefinedProperties() throws Exception {
    SNMPDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    Notification notification = mock(Notification.class);
    notification.Callback = mock(DispatchCallback.class);
    notification.CallbackIds = new ArrayList<>();
    notification.DispatchProperties = new HashMap<>();
    dispatcher.dispatch(notification);
    verify(notification.Callback).onFailure(notification.CallbackIds);
    verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
  }

  @Test
  public void testDispatch_nullRecipients() throws Exception {
    SNMPDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    Notification notification = mock(Notification.class);
    notification.Callback = mock(DispatchCallback.class);
    notification.CallbackIds = new ArrayList<>();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "3");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "4");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    notification.DispatchProperties = properties;
    notification.Body = "body";
    notification.Subject = "subject";
    dispatcher.dispatch(notification);
    verify(notification.Callback).onFailure(notification.CallbackIds);
    verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
  }

  @Test
  public void testDispatch_noRecipients() throws Exception {
    SNMPDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    Notification notification = mock(Notification.class);
    notification.Callback = mock(DispatchCallback.class);
    notification.CallbackIds = new ArrayList<>();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "3");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "4");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    notification.DispatchProperties = properties;
    notification.Body = "body";
    notification.Subject = "subject";
    notification.Recipients = new ArrayList<>();
    dispatcher.dispatch(notification);
    verify(notification.Callback).onFailure(notification.CallbackIds);
    verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
  }

  @Test
  public void testDispatch_sendTrapError() throws Exception {
    SNMPDispatcher dispatcher = spy(new SNMPDispatcher(DEFAULT_SNMP_PORT));
    Notification notification = mock(Notification.class);
    notification.Callback = mock(DispatchCallback.class);
    notification.CallbackIds = new ArrayList<>();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "3");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "4");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    notification.DispatchProperties = properties;
    notification.Body = "body";
    notification.Subject = "subject";
    notification.Recipients = Arrays.asList(new Recipient());
    doThrow(new RuntimeException()).when(dispatcher).sendTraps(eq(notification), any(SNMPDispatcher.SnmpVersion.class));
    dispatcher.dispatch(notification);
    verify(notification.Callback).onFailure(notification.CallbackIds);
    verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
  }

  @Test
  public void testDispatch_incorrectSnmpVersion() throws Exception {
    SNMPDispatcher dispatcher = spy(new SNMPDispatcher(DEFAULT_SNMP_PORT));
    Notification notification = mock(Notification.class);
    notification.Callback = mock(DispatchCallback.class);
    notification.CallbackIds = new ArrayList<>();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "3");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "4");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv11");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    notification.DispatchProperties = properties;
    notification.Body = "body";
    notification.Subject = "subject";
    notification.Recipients = Arrays.asList(new Recipient());
    dispatcher.dispatch(notification);
    verify(notification.Callback).onFailure(notification.CallbackIds);
    verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
  }

  @Test
  public void testDispatch_successful_v1() throws Exception {
    SNMPDispatcher dispatcher = spy(new SNMPDispatcher(DEFAULT_SNMP_PORT));
    SNMPDispatcher.SnmpVersion snmpVersion = SNMPDispatcher.SnmpVersion.SNMPv1;
    Notification notification = mock(Notification.class);
    notification.Callback = mock(DispatchCallback.class);
    notification.CallbackIds = new ArrayList<>();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "3");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "4");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    notification.DispatchProperties = properties;
    notification.Body = "body";
    notification.Subject = "subject";
    notification.Recipients = Arrays.asList(new Recipient());
    doNothing().when(dispatcher).sendTraps(notification, snmpVersion);
    dispatcher.dispatch(notification);
    verify(notification.Callback, never()).onFailure(notification.CallbackIds);
    verify(notification.Callback).onSuccess(notification.CallbackIds);
  }

  @Test
  public void testDispatch_successful_v2() throws Exception {
    SNMPDispatcher dispatcher = spy(new SNMPDispatcher(DEFAULT_SNMP_PORT));
    SNMPDispatcher.SnmpVersion snmpVersion = SNMPDispatcher.SnmpVersion.SNMPv2c;
    Notification notification = mock(Notification.class);
    notification.Callback = mock(DispatchCallback.class);
    notification.CallbackIds = new ArrayList<>();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "3");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "4");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv2c");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    notification.DispatchProperties = properties;
    notification.Body = "body";
    notification.Subject = "subject";
    notification.Recipients = Arrays.asList(new Recipient());
    doNothing().when(dispatcher).sendTraps(notification, snmpVersion);
    dispatcher.dispatch(notification);
    verify(notification.Callback, never()).onFailure(notification.CallbackIds);
    verify(notification.Callback).onSuccess(notification.CallbackIds);
  }

  @Test
  public void testDispatch_successful_v3() throws Exception {
    SNMPDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    Notification notification = new Notification();
    notification.Callback = mock(DispatchCallback.class);
    notification.CallbackIds = new ArrayList<>();
    notification.Body = "body";
    notification.Subject = "subject";
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "public");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
    properties.put(SNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
    properties.put(SNMPDispatcher.SECURITY_PRIV_PASSPHRASE_PROPERTY, "PASSPHRASE2");
    properties.put(SNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_NOPRIV");
    notification.DispatchProperties = properties;
    Recipient recipient = new Recipient();
    recipient.Identifier = "192.168.0.2";
    notification.Recipients = Arrays.asList(recipient);
    dispatcher.dispatch(notification);
    verify(notification.Callback, never()).onFailure(notification.CallbackIds);
    verify(notification.Callback).onSuccess(notification.CallbackIds);
  }

  @Test
  public void testPrepareTrap_v1() throws Exception {
    SNMPDispatcher.SnmpVersion snmpVersion = SNMPDispatcher.SnmpVersion.SNMPv1;
    SNMPDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    Notification notification = new Notification();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "3");
    notification.DispatchProperties = properties;
    notification.Body = "body";
    notification.Subject = "subject";
    PDU pdu = dispatcher.prepareTrap(notification, snmpVersion);
    assertEquals(PDU.V1TRAP, pdu.getType());
    Map<String, VariableBinding> variableBindings = new HashMap<>();
    for (VariableBinding variableBinding : pdu.toArray()) {
      variableBindings.put(variableBinding.getOid().toString(), variableBinding);
    }
    assertEquals(3, variableBindings.size());
    assertEquals("subject", variableBindings.get("1").toValueString());
    assertEquals("body", variableBindings.get("2").toValueString());
    assertEquals("3", variableBindings.get(SnmpConstants.snmpTrapOID.toString()).toValueString());
  }

  @Test
  public void testPrepareTrap_v2c() throws Exception {
    SNMPDispatcher.SnmpVersion snmpVersion = SNMPDispatcher.SnmpVersion.SNMPv2c;
    SNMPDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    Notification notification = new Notification();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "4");
    notification.DispatchProperties = properties;
    notification.Body = "body";
    notification.Subject = "subject";
    PDU pdu = dispatcher.prepareTrap(notification, snmpVersion);
    assertEquals(PDU.TRAP, pdu.getType());
    Map<String, VariableBinding> variableBindings = new HashMap<>();
    for (VariableBinding variableBinding : pdu.toArray()) {
      variableBindings.put(variableBinding.getOid().toString(), variableBinding);
    }
    assertEquals(3, variableBindings.size());
    assertEquals("subject", variableBindings.get("1").toValueString());
    assertEquals("body", variableBindings.get("2").toValueString());
    assertEquals("4", variableBindings.get(SnmpConstants.snmpTrapOID.toString()).toValueString());
  }

  @Test
  public void testSendTraps_v1() throws Exception {
    SNMPDispatcher.SnmpVersion snmpVersion = SNMPDispatcher.SnmpVersion.SNMPv1;
    Snmp snmp = mock(Snmp.class);
    SNMPDispatcher dispatcher = spy(new SNMPDispatcher(snmp));
    PDU trap = mock(PDU.class);
    Notification notification = new Notification();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "public");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    notification.DispatchProperties = properties;
    Recipient rec1 = new Recipient();
    rec1.Identifier = "192.168.0.2";
    notification.Recipients = Arrays.asList(rec1);
    doReturn(trap).when(dispatcher).prepareTrap(notification, snmpVersion);
    dispatcher.sendTraps(notification, snmpVersion);
    ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
    verify(snmp, times(1)).send(eq(trap), argument.capture());
    assertEquals("192.168.0.2/162", argument.getValue().getAddress().toString());
    assertEquals(SnmpConstants.version1, argument.getValue().getVersion());
  }

  @Test
  public void testSendTraps_v2() throws Exception {
    SNMPDispatcher.SnmpVersion snmpVersion = SNMPDispatcher.SnmpVersion.SNMPv2c;
    Snmp snmp = mock(Snmp.class);
    SNMPDispatcher dispatcher = spy(new SNMPDispatcher(snmp));
    PDU trap = mock(PDU.class);
    Notification notification = new Notification();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "public");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    notification.DispatchProperties = properties;
    Recipient rec1 = new Recipient();
    rec1.Identifier = "192.168.0.2";
    notification.Recipients = Arrays.asList(rec1);
    doReturn(trap).when(dispatcher).prepareTrap(notification, snmpVersion);
    dispatcher.sendTraps(notification, snmpVersion);
    ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
    verify(snmp, times(1)).send(eq(trap), argument.capture());
    assertEquals("192.168.0.2/162", argument.getValue().getAddress().toString());
    assertEquals(SnmpConstants.version2c, argument.getValue().getVersion());
  }

  @Test
  public void testSendTraps_v3() throws Exception {
    SNMPDispatcher.SnmpVersion snmpVersion = SNMPDispatcher.SnmpVersion.SNMPv3;
    Snmp snmp = mock(Snmp.class);
    SNMPDispatcher dispatcher = spy(new SNMPDispatcher(snmp));
    PDU trap = mock(PDU.class);
    Notification notification = new Notification();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
    properties.put(SNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
    properties.put(SNMPDispatcher.SECURITY_PRIV_PASSPHRASE_PROPERTY, "PASSPHRASE2");
    properties.put(SNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_NOPRIV");
    notification.DispatchProperties = properties;
    Recipient rec1 = new Recipient();
    rec1.Identifier = "192.168.0.2";
    notification.Recipients = Arrays.asList(rec1);
    doReturn(trap).when(dispatcher).prepareTrap(notification, snmpVersion);
    dispatcher.sendTraps(notification, snmpVersion);
    ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
    verify(snmp, times(1)).send(eq(trap), argument.capture());
    assertEquals("192.168.0.2/162", argument.getValue().getAddress().toString());
    assertEquals(SnmpConstants.version3, argument.getValue().getVersion());
  }

  @Test(expected = SNMPDispatcher.InvalidSnmpConfigurationException.class)
  public void testSendTraps_v3_incorrectSecurityLevelVersion() throws Exception {
    SNMPDispatcher.SnmpVersion snmpVersion = SNMPDispatcher.SnmpVersion.SNMPv3;
    Snmp snmp = mock(Snmp.class);
    SNMPDispatcher dispatcher = spy(new SNMPDispatcher(snmp));
    PDU trap = mock(PDU.class);
    Notification notification = new Notification();
    Map<String, String> properties = new HashMap<>();
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
    properties.put(SNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
    properties.put(SNMPDispatcher.SECURITY_PRIV_PASSPHRASE_PROPERTY, "PASSPHRASE2");
    properties.put(SNMPDispatcher.SECURITY_LEVEL_PROPERTY, "INCORRECT");
    notification.DispatchProperties = properties;
    Recipient rec1 = new Recipient();
    rec1.Identifier = "192.168.0.2";
    notification.Recipients = Arrays.asList(rec1);
    doReturn(trap).when(dispatcher).prepareTrap(notification, snmpVersion);
    dispatcher.sendTraps(notification, snmpVersion);
  }

  @Test
  public void testValidateAlertValidation_SNMPv1() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "public");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_incorrectSNMPversion() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv4");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "public");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_SNMPv1_invalid() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "public");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_SNMPv2c() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv2c");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.COMMUNITY_PROPERTY, "public");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_SNMPv2c_invalid() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv2c");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_SNMPv3_incorrectSecurityLevel() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
    properties.put(SNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
    properties.put(SNMPDispatcher.SECURITY_PRIV_PASSPHRASE_PROPERTY, "PASSPHRASE2");
    properties.put(SNMPDispatcher.SECURITY_LEVEL_PROPERTY, "INCORRECT");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_SNMPv3_noAuthNoPriv() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
    properties.put(SNMPDispatcher.SECURITY_LEVEL_PROPERTY, "NOAUTH_NOPRIV");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_SNMPv3_AuthNoPriv_valid() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
    properties.put(SNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
    properties.put(SNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_NOPRIV");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_SNMPv3_AuthNoPriv_invalid() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
    properties.put(SNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_NOPRIV");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_SNMPv3_AuthPriv_valid() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
    properties.put(SNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
    properties.put(SNMPDispatcher.SECURITY_PRIV_PASSPHRASE_PROPERTY, "PASSPHRASE2");
    properties.put(SNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_PRIV");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_SNMPv3_AuthPriv_noPassphrases() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
    properties.put(SNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_PRIV");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateAlertValidation_SNMPv3_AuthPriv_onlyAuthPassphrase() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SNMPDispatcher.SUBJECT_OID_PROPERTY, "1");
    properties.put(SNMPDispatcher.BODY_OID_PROPERTY, "2");
    properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
    properties.put(SNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
    properties.put(SNMPDispatcher.TRAP_OID_PROPERTY, "1.3.6.1.6.3.1.1.5.4");
    properties.put(SNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
    properties.put(SNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
    properties.put(SNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_PRIV");
    NotificationDispatcher dispatcher = new SNMPDispatcher(DEFAULT_SNMP_PORT);
    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
  }
}
