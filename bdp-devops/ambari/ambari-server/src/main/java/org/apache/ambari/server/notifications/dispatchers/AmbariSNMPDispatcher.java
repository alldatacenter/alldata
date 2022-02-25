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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.state.alert.AlertNotification;
import org.apache.ambari.server.state.alert.TargetType;
import org.apache.ambari.server.state.services.AlertNoticeDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;

import com.google.inject.Singleton;

/**
 * The {@link AmbariSNMPDispatcher} class is used to dispatch {@link AlertNotification} via SNMP using predefined Ambari OIDs.
 *
 * <pre>The base OID for Ambari is 1.3.6.1.4.1.18060.16. Off of this, we define the following:
 * .0 - apacheAmbariTraps
 * .1 - apacheAmbariAlerts
 * From these two roots, we define other bindings:
 * .1.1 - apacheAmbariAlertTable
 * .1.1.1 - apacheAmbariAlertEntry
 * .1.1.1.2 - alertDefinitionName
 * </pre>
 */

@Singleton
public class AmbariSNMPDispatcher extends SNMPDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(AmbariSNMPDispatcher.class);

    public static final String BASE_AMBARI_OID = "1.3.6.1.4.1.18060.16";
    public static final String APACHE_AMBARI_TRAPS_OID = BASE_AMBARI_OID + ".0";
    public static final String AMBARI_ALERT_TRAP_OID = APACHE_AMBARI_TRAPS_OID + ".1";
    public static final String AMBARI_ALERTS_OID = BASE_AMBARI_OID + ".1";
    public static final String AMBARI_ALERT_TABLE_OID = AMBARI_ALERTS_OID + ".1";
    public static final String AMBARI_ALERT_ENTRY_OID = AMBARI_ALERT_TABLE_OID + ".1";
    //ALERT_ENTRY fields
    public static final String AMBARI_ALERT_DEFINITION_ID_OID = AMBARI_ALERT_ENTRY_OID + ".1";
    public static final String AMBARI_ALERT_DEFINITION_NAME_OID = AMBARI_ALERT_ENTRY_OID + ".2";
    public static final String AMBARI_ALERT_DEFINITION_HASH_OID = AMBARI_ALERT_ENTRY_OID + ".3";
    public static final String AMBARI_ALERT_NAME_OID = AMBARI_ALERT_ENTRY_OID + ".4";
    public static final String AMBARI_ALERT_TEXT_OID = AMBARI_ALERT_ENTRY_OID + ".5";
    public static final String AMBARI_ALERT_STATE_OID = AMBARI_ALERT_ENTRY_OID + ".6";
    public static final String AMBARI_ALERT_HOST_NAME_OID = AMBARI_ALERT_ENTRY_OID + ".7";
    public static final String AMBARI_ALERT_SERVICE_NAME_OID = AMBARI_ALERT_ENTRY_OID + ".8";
    public static final String AMBARI_ALERT_COMPONENT_NAME_OID = AMBARI_ALERT_ENTRY_OID + ".9";

    protected AmbariSNMPDispatcher(Snmp snmp) {
        super(snmp);
    }

    public AmbariSNMPDispatcher(Integer port) throws IOException {
        super(port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return TargetType.AMBARI_SNMP.name();
    }

    /**
     * {@inheritDoc}
     * Uses default Ambari OIDs
     */
    @Override
    protected PDU prepareTrap(Notification notification, SnmpVersion snmpVersion) throws InvalidSnmpConfigurationException {
        AlertNotification alertNotification;
        PDU pdu = DefaultPDUFactory.createPDU(snmpVersion.getTargetVersion());

        if (Notification.Type.ALERT.equals(notification.getType())) {
            try {
                alertNotification = (AlertNotification) notification;
            } catch (ClassCastException e) {
                LOG.error("Notification wasn't casted to AlertNotification. Returning empty Protocol data unit", e);
                return pdu;
            }
        } else {
            LOG.error("Notification for AmbariSNMPDispatcher should be of type AlertNotification, but it wasn't. Returning empty Protocol data unit");
            return pdu;
        }

        pdu.setType(snmpVersion.getTrapType());
    
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptimeInMillis = runtimeMXBean.getUptime();
        pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(uptimeInMillis)));

       // Set trap oid for PDU
        pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(AMBARI_ALERT_TRAP_OID)));
        // Set notification body and subject for PDU objects with identifiers specified in dispatch properties.
        AlertNoticeDispatchService.AlertInfo alertInfo = alertNotification.getAlertInfo();
        addIntVariableBindingCheckForNull(pdu, AMBARI_ALERT_DEFINITION_ID_OID, new BigDecimal(alertInfo.getAlertDefinitionId()).intValueExact());
        addStringVariableBindingCheckForNull(pdu, AMBARI_ALERT_DEFINITION_NAME_OID, alertInfo.getAlertDefinition().getDefinitionName());
        addStringVariableBindingCheckForNull(pdu, AMBARI_ALERT_DEFINITION_HASH_OID, alertInfo.getAlertDefinitionHash());
        addStringVariableBindingCheckForNull(pdu, AMBARI_ALERT_NAME_OID, alertInfo.getAlertName());
        addStringVariableBindingCheckForNull(pdu, AMBARI_ALERT_TEXT_OID, alertInfo.getAlertText());
        addIntVariableBindingCheckForNull(pdu, AMBARI_ALERT_STATE_OID, alertInfo.getAlertState().getIntValue());
        addStringVariableBindingCheckForNull(pdu, AMBARI_ALERT_HOST_NAME_OID, alertInfo.getHostName());
        addStringVariableBindingCheckForNull(pdu, AMBARI_ALERT_SERVICE_NAME_OID, alertInfo.getServiceName());
        addStringVariableBindingCheckForNull(pdu, AMBARI_ALERT_COMPONENT_NAME_OID, alertInfo.getComponentName());

        return pdu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getSetOfDefaultNeededPropertyNames() {
        return new HashSet<>(Collections.singletonList(PORT_PROPERTY));
    }

    /**
     * Adds new {@link VariableBinding} using provided {@link OID} and value to {@link PDU}
     * if val is null than adds {@link OctetString} with "null" value;
     * @param pdu
     * @param oid
     * @param val
     */
    private void addStringVariableBindingCheckForNull(PDU pdu, String oid, Object val) {
        if (val == null)  {
            pdu.add(new VariableBinding(new OID(oid), new OctetString("null")));
        } else {
            pdu.add(new VariableBinding(new OID(oid),
                    new OctetString(String.valueOf(val))));
        }
    }
    /**
     * Adds new {@link VariableBinding} using provided {@link OID} and value to {@link PDU}
     * if val is null than adds {@link OctetString} with "null" value;
     * @param pdu
     * @param oid
     * @param val
     */
    private void addIntVariableBindingCheckForNull(PDU pdu, String oid, Integer val) {
        if (val == null)  {
            pdu.add(new VariableBinding(new OID(oid), new OctetString("null")));
        } else {
            pdu.add(new VariableBinding(new OID(oid),
                    new Integer32(val)));
        }
    }
}
