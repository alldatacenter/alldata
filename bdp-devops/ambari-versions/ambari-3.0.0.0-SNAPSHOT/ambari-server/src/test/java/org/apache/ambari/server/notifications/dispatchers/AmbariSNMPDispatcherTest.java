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
import static org.junit.Assert.assertTrue;
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
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.notifications.DispatchCallback;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.Recipient;
import org.apache.ambari.server.notifications.TargetConfigurationResult;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.alert.AlertNotification;
import org.apache.ambari.server.state.services.AlertNoticeDispatchService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

public class AmbariSNMPDispatcherTest {

    private static final int DEFAULT_SNMP_PORT = 31444;


    public static final String DEFINITION_NAME = "definition name";
    public static final String ALERT_LABEL = "alert name";
    public static final String ALERT_TEXT = "alert text";
    public static final String ALERT_HOSTNAME = "hostname";
    public static final String ALERT_SERVICE_NAME = "service name";
    public static final String ALERT_COMPONENT_NAME = "component name";
    public static final Long DEFINITION_ID = 1L;
    public static final AlertState ALERT_STATE = AlertState.OK;

    @Test
    public void testDispatch_nullProperties() throws Exception {
        AmbariSNMPDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        Notification notification = mock(AlertNotification.class);
        notification.Callback = mock(DispatchCallback.class);
        notification.CallbackIds = new ArrayList<>();
        dispatcher.dispatch(notification);
        verify(notification.Callback).onFailure(notification.CallbackIds);
        verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
    }

    @Test
    public void testDispatchUdpTransportMappingCrash() throws Exception {
        AmbariSNMPDispatcher dispatcher = spy(new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT));
        AmbariSNMPDispatcher.SnmpVersion snmpVersion = AmbariSNMPDispatcher.SnmpVersion.SNMPv1;
        Notification notification = mock(AlertNotification.class);
        notification.Callback = mock(DispatchCallback.class);
        notification.CallbackIds = mock(List.class);
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "3");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "4");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
        notification.DispatchProperties = properties;
        notification.Recipients = Arrays.asList(new Recipient());
        doThrow(new IOException()).when(dispatcher).sendTraps(notification, snmpVersion);
        dispatcher.dispatch(notification);
        verify(notification.Callback).onFailure(notification.CallbackIds);
        verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
        assertNull(dispatcher.getTransportMapping());
    }

    @Test
    public void testDispatch_notDefinedProperties() throws Exception {
        AmbariSNMPDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        Notification notification = mock(AlertNotification.class);
        notification.Callback = mock(DispatchCallback.class);
        notification.CallbackIds = mock(List.class);
        notification.DispatchProperties = new HashMap<>();
        dispatcher.dispatch(notification);
        verify(notification.Callback).onFailure(notification.CallbackIds);
        verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
    }

    @Test
    public void testDispatch_nullRecipients() throws Exception {
        AmbariSNMPDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        Notification notification = getAlertNotification(true);
        notification.Callback = mock(DispatchCallback.class);
        notification.CallbackIds = mock(List.class);
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "3");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "4");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
        notification.DispatchProperties = properties;
        dispatcher.dispatch(notification);
        verify(notification.Callback).onFailure(notification.CallbackIds);
        verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
    }

    @Test
    public void testDispatch_noRecipients() throws Exception {
        AmbariSNMPDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        Notification notification = getAlertNotification(true);
        notification.Callback = mock(DispatchCallback.class);
        notification.CallbackIds = mock(List.class);
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "3");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "4");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
        notification.DispatchProperties = properties;
        notification.Recipients = new ArrayList<>();
        dispatcher.dispatch(notification);
        verify(notification.Callback).onFailure(notification.CallbackIds);
        verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
    }

    @Test
    public void testDispatch_sendTrapError() throws Exception {
        AmbariSNMPDispatcher dispatcher = spy(new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT));
        Notification notification = mock(AlertNotification.class);
        notification.Callback = mock(DispatchCallback.class);
        notification.CallbackIds = new ArrayList<>();
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "3");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "4");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
        notification.DispatchProperties = properties;
        notification.Recipients = Arrays.asList(new Recipient());
        doThrow(new RuntimeException()).when(dispatcher).sendTraps(eq(notification), any(AmbariSNMPDispatcher.SnmpVersion.class));
        dispatcher.dispatch(notification);
        verify(notification.Callback).onFailure(notification.CallbackIds);
        verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
    }

    @Test
    public void testDispatch_incorrectSnmpVersion() throws Exception {
        AmbariSNMPDispatcher dispatcher = spy(new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT));
        Notification notification = mock(AlertNotification.class);
        notification.Callback = mock(DispatchCallback.class);
        notification.CallbackIds = new ArrayList<>();
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "3");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "4");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv11");
        notification.DispatchProperties = properties;
        notification.Recipients = Arrays.asList(new Recipient());
        dispatcher.dispatch(notification);
        verify(notification.Callback).onFailure(notification.CallbackIds);
        verify(notification.Callback, never()).onSuccess(notification.CallbackIds);
    }

    @Test
    public void testDispatch_successful_v1() throws Exception {
        AmbariSNMPDispatcher dispatcher = spy(new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT));
        AmbariSNMPDispatcher.SnmpVersion snmpVersion = AmbariSNMPDispatcher.SnmpVersion.SNMPv1;
        Notification notification = mock(AlertNotification.class);
        notification.Callback = mock(DispatchCallback.class);
        notification.CallbackIds = new ArrayList<>();
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "3");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "4");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
        notification.DispatchProperties = properties;
        notification.Recipients = Arrays.asList(new Recipient());
        doNothing().when(dispatcher).sendTraps(notification, snmpVersion);
        dispatcher.dispatch(notification);
        verify(notification.Callback, never()).onFailure(notification.CallbackIds);
        verify(notification.Callback).onSuccess(notification.CallbackIds);
    }

    @Test
    public void testDispatch_successful_v2() throws Exception {
        AmbariSNMPDispatcher dispatcher = spy(new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT));
        AmbariSNMPDispatcher.SnmpVersion snmpVersion = AmbariSNMPDispatcher.SnmpVersion.SNMPv2c;
        Notification notification = mock(AlertNotification.class);
        notification.Callback = mock(DispatchCallback.class);
        notification.CallbackIds = mock(List.class);
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "3");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "4");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv2c");
        notification.DispatchProperties = properties;
        notification.Recipients = Arrays.asList(new Recipient());
        doNothing().when(dispatcher).sendTraps(notification, snmpVersion);
        dispatcher.dispatch(notification);
        verify(notification.Callback, never()).onFailure(notification.CallbackIds);
        verify(notification.Callback).onSuccess(notification.CallbackIds);
    }

    @Test
    public void testDispatch_successful_v3() throws Exception {
        AmbariSNMPDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        Notification notification = getAlertNotification(true);
        notification.Callback = mock(DispatchCallback.class);
        notification.CallbackIds = mock(List.class);
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "public");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
        properties.put(AmbariSNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
        properties.put(AmbariSNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
        properties.put(AmbariSNMPDispatcher.SECURITY_PRIV_PASSPHRASE_PROPERTY, "PASSPHRASE2");
        properties.put(AmbariSNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_NOPRIV");
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
        AmbariSNMPDispatcher.SnmpVersion snmpVersion = AmbariSNMPDispatcher.SnmpVersion.SNMPv1;
        AmbariSNMPDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        Notification notification = getAlertNotification(true);
        PDU pdu = dispatcher.prepareTrap(notification, snmpVersion);
        assertEquals(PDU.V1TRAP, pdu.getType());
        Map<String, VariableBinding> variableBindings = new HashMap<>();
        for (VariableBinding variableBinding : pdu.toArray()) {
            variableBindings.put(variableBinding.getOid().toString(), variableBinding);
        }
    assertEquals(11, variableBindings.size());
        assertEquals(AmbariSNMPDispatcher.AMBARI_ALERT_TRAP_OID, variableBindings.get(SnmpConstants.snmpTrapOID.toString()).toValueString());
        assertTrue(variableBindings.get(SnmpConstants.snmpTrapOID.toString()).getVariable() instanceof OID);
        assertEquals(String.valueOf(DEFINITION_ID), variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_DEFINITION_ID_OID).toValueString());
        assertTrue(variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_DEFINITION_ID_OID).getVariable() instanceof Integer32);
        assertEquals(DEFINITION_NAME, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_DEFINITION_NAME_OID).toValueString());
        assertTrue(variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_DEFINITION_NAME_OID).getVariable() instanceof OctetString);
        assertEquals(ALERT_LABEL, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_NAME_OID).toValueString());
        assertTrue(variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_NAME_OID).getVariable() instanceof OctetString);
        assertEquals(ALERT_TEXT, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_TEXT_OID).toValueString());
        assertTrue(variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_TEXT_OID).getVariable() instanceof OctetString);
        assertEquals(String.valueOf(ALERT_STATE.getIntValue()), variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_STATE_OID).toValueString());
        assertTrue(variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_STATE_OID).getVariable() instanceof Integer32);
        assertEquals(ALERT_HOSTNAME, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_HOST_NAME_OID).toValueString());
        assertTrue(variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_HOST_NAME_OID).getVariable() instanceof OctetString);
        assertEquals(ALERT_SERVICE_NAME, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_SERVICE_NAME_OID).toValueString());
        assertTrue(variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_SERVICE_NAME_OID).getVariable() instanceof OctetString);
        assertEquals(ALERT_COMPONENT_NAME, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_COMPONENT_NAME_OID).toValueString());
        assertTrue(variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_COMPONENT_NAME_OID).getVariable() instanceof OctetString);
    }

    @Test
    public void testPrepareTrapNull() throws Exception {
        AmbariSNMPDispatcher.SnmpVersion snmpVersion = AmbariSNMPDispatcher.SnmpVersion.SNMPv1;
        AmbariSNMPDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        AlertNotification notification = (AlertNotification) getAlertNotification(false);
        PDU pdu = dispatcher.prepareTrap(notification, snmpVersion);
        assertEquals(PDU.V1TRAP, pdu.getType());
        Map<String, VariableBinding> variableBindings = new HashMap<>();
        for (VariableBinding variableBinding : pdu.toArray()) {
            variableBindings.put(variableBinding.getOid().toString(), variableBinding);
        }
    assertEquals(11, variableBindings.size());
        assertEquals("null", variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_COMPONENT_NAME_OID).toValueString());
    }


    @Test
    public void testPrepareTrap_v2c() throws Exception {
        AmbariSNMPDispatcher.SnmpVersion snmpVersion = AmbariSNMPDispatcher.SnmpVersion.SNMPv2c;
        AmbariSNMPDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        Notification notification = getAlertNotification(true);
        PDU pdu = dispatcher.prepareTrap(notification, snmpVersion);
        assertEquals(PDU.TRAP, pdu.getType());
        Map<String, VariableBinding> variableBindings = new HashMap<>();
        for (VariableBinding variableBinding : pdu.toArray()) {
            variableBindings.put(variableBinding.getOid().toString(), variableBinding);
        }

    assertEquals(11, variableBindings.size());
        assertEquals(AmbariSNMPDispatcher.AMBARI_ALERT_TRAP_OID, variableBindings.get(SnmpConstants.snmpTrapOID.toString()).toValueString());
        assertEquals(String.valueOf(DEFINITION_ID), variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_DEFINITION_ID_OID).toValueString());
        assertEquals(DEFINITION_NAME, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_DEFINITION_NAME_OID).toValueString());
        assertEquals(ALERT_LABEL, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_NAME_OID).toValueString());
        assertEquals(ALERT_TEXT, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_TEXT_OID).toValueString());
        assertEquals(String.valueOf(ALERT_STATE.getIntValue()), variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_STATE_OID).toValueString());
        assertEquals(ALERT_HOSTNAME, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_HOST_NAME_OID).toValueString());
        assertEquals(ALERT_SERVICE_NAME, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_SERVICE_NAME_OID).toValueString());
        assertEquals(ALERT_COMPONENT_NAME, variableBindings.get(AmbariSNMPDispatcher.AMBARI_ALERT_COMPONENT_NAME_OID).toValueString());
    }

    @Test
    public void testSendTraps_v1() throws Exception {
        AmbariSNMPDispatcher.SnmpVersion snmpVersion = AmbariSNMPDispatcher.SnmpVersion.SNMPv1;
        Snmp snmp = mock(Snmp.class);
        AmbariSNMPDispatcher dispatcher = spy(new AmbariSNMPDispatcher(snmp));
        PDU trap = mock(PDU.class);
        Notification notification = new AlertNotification();
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "public");
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
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
        AmbariSNMPDispatcher.SnmpVersion snmpVersion = AmbariSNMPDispatcher.SnmpVersion.SNMPv2c;
        Snmp snmp = mock(Snmp.class);
        AmbariSNMPDispatcher dispatcher = spy(new AmbariSNMPDispatcher(snmp));
        PDU trap = mock(PDU.class);
        Notification notification = new AlertNotification();
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "public");
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
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
        AmbariSNMPDispatcher.SnmpVersion snmpVersion = AmbariSNMPDispatcher.SnmpVersion.SNMPv3;
        Snmp snmp = mock(Snmp.class);
        AmbariSNMPDispatcher dispatcher = spy(new AmbariSNMPDispatcher(snmp));
        PDU trap = mock(PDU.class);
        Notification notification = new AlertNotification();
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
        properties.put(AmbariSNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
        properties.put(AmbariSNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
        properties.put(AmbariSNMPDispatcher.SECURITY_PRIV_PASSPHRASE_PROPERTY, "PASSPHRASE2");
        properties.put(AmbariSNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_NOPRIV");
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

    @Test(expected = AmbariSNMPDispatcher.InvalidSnmpConfigurationException.class)
    public void testSendTraps_v3_incorrectSecurityLevelVersion() throws Exception {
        AmbariSNMPDispatcher.SnmpVersion snmpVersion = AmbariSNMPDispatcher.SnmpVersion.SNMPv3;
        Snmp snmp = mock(Snmp.class);
        AmbariSNMPDispatcher dispatcher = spy(new AmbariSNMPDispatcher(snmp));
        PDU trap = mock(PDU.class);
        Notification notification = new AlertNotification();
        Map<String, String> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
        properties.put(AmbariSNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
        properties.put(AmbariSNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
        properties.put(AmbariSNMPDispatcher.SECURITY_PRIV_PASSPHRASE_PROPERTY, "PASSPHRASE2");
        properties.put(AmbariSNMPDispatcher.SECURITY_LEVEL_PROPERTY, "INCORRECT");
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
        properties.put(SNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "public");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_incorrectSNMPversion() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv4");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "public");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_SNMPv1_invalid_noPort() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv1");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "public");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_SNMPv2c() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv2c");
        properties.put(AmbariSNMPDispatcher.COMMUNITY_PROPERTY, "public");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_SNMPv2c_invalid() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv2c");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_SNMPv3_incorrectSecurityLevel() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
        properties.put(AmbariSNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
        properties.put(AmbariSNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
        properties.put(AmbariSNMPDispatcher.SECURITY_PRIV_PASSPHRASE_PROPERTY, "PASSPHRASE2");
        properties.put(AmbariSNMPDispatcher.SECURITY_LEVEL_PROPERTY, "INCORRECT");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_SNMPv3_noAuthNoPriv() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
        properties.put(AmbariSNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
        properties.put(AmbariSNMPDispatcher.SECURITY_LEVEL_PROPERTY, "NOAUTH_NOPRIV");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_SNMPv3_AuthNoPriv_valid() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
        properties.put(AmbariSNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
        properties.put(AmbariSNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
        properties.put(AmbariSNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_NOPRIV");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_SNMPv3_AuthNoPriv_invalid() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
        properties.put(AmbariSNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
        properties.put(AmbariSNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_NOPRIV");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_SNMPv3_AuthPriv_valid() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
        properties.put(AmbariSNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
        properties.put(AmbariSNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
        properties.put(AmbariSNMPDispatcher.SECURITY_PRIV_PASSPHRASE_PROPERTY, "PASSPHRASE2");
        properties.put(AmbariSNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_PRIV");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_SNMPv3_AuthPriv_noPassphrases() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
        properties.put(AmbariSNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
        properties.put(AmbariSNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_PRIV");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
    }

    @Test
    public void testValidateAlertValidation_SNMPv3_AuthPriv_onlyAuthPassphrase() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(AmbariSNMPDispatcher.PORT_PROPERTY, "162");
        properties.put(AmbariSNMPDispatcher.SNMP_VERSION_PROPERTY, "SNMPv3");
        properties.put(AmbariSNMPDispatcher.SECURITY_USERNAME_PROPERTY, "USER");
        properties.put(AmbariSNMPDispatcher.SECURITY_AUTH_PASSPHRASE_PROPERTY, "PASSPHRASE1");
        properties.put(AmbariSNMPDispatcher.SECURITY_LEVEL_PROPERTY, "AUTH_PRIV");
        NotificationDispatcher dispatcher = new AmbariSNMPDispatcher(DEFAULT_SNMP_PORT);
        TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
        assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
    }

    private Notification getAlertNotification(boolean hasComponent) {
        AlertNotification notification = new AlertNotification();
        AlertDefinitionEntity alertDefinitionEntity = new AlertDefinitionEntity();
        alertDefinitionEntity.setDefinitionName(DEFINITION_NAME);
        alertDefinitionEntity.setLabel(ALERT_LABEL);
        alertDefinitionEntity.setDefinitionId(DEFINITION_ID);
        AlertHistoryEntity alertHistoryEntity = new AlertHistoryEntity();
        alertHistoryEntity.setAlertDefinition(alertDefinitionEntity);
        alertHistoryEntity.setAlertLabel(ALERT_LABEL);
        alertHistoryEntity.setAlertState(ALERT_STATE);
        alertHistoryEntity.setAlertText(ALERT_TEXT);
        alertHistoryEntity.setHostName(ALERT_HOSTNAME);
        alertHistoryEntity.setServiceName(ALERT_SERVICE_NAME);
        if (hasComponent) {
            alertHistoryEntity.setComponentName(ALERT_COMPONENT_NAME);
        }
        AlertNoticeDispatchService.AlertInfo alertInfo = new AlertNoticeDispatchService.AlertInfo(alertHistoryEntity);
        notification.setAlertInfo(alertInfo);
        return notification;
    }
}
