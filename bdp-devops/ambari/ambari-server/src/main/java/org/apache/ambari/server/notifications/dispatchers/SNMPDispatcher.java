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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.Recipient;
import org.apache.ambari.server.notifications.TargetConfigurationResult;
import org.apache.ambari.server.state.alert.TargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.transport.UdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;

import com.google.inject.Singleton;

/**
 * The {@link SNMPDispatcher} class is used to dispatch {@link Notification} via SNMP.
 */
@Singleton
public class SNMPDispatcher implements NotificationDispatcher {

  private static final Logger LOG = LoggerFactory.getLogger(SNMPDispatcher.class);

  // Trap's object identifiers
  public static final String BODY_OID_PROPERTY = "ambari.dispatch.snmp.oids.body";
  public static final String SUBJECT_OID_PROPERTY = "ambari.dispatch.snmp.oids.subject";
  public static final String TRAP_OID_PROPERTY = "ambari.dispatch.snmp.oids.trap";

  // SNMP Server port
  public static final String PORT_PROPERTY = "ambari.dispatch.snmp.port";

  // SNMP version
  public static final String SNMP_VERSION_PROPERTY = "ambari.dispatch.snmp.version";

  // Properties for community-based security model configuration
  public static final String COMMUNITY_PROPERTY = "ambari.dispatch.snmp.community";

  // Properties for user-based security model configuration
  public static final String SECURITY_USERNAME_PROPERTY = "ambari.dispatch.snmp.security.username";
  public static final String SECURITY_AUTH_PASSPHRASE_PROPERTY = "ambari.dispatch.snmp.security.auth.passphrase";
  public static final String SECURITY_PRIV_PASSPHRASE_PROPERTY = "ambari.dispatch.snmp.security.priv.passphrase";
  public static final String SECURITY_LEVEL_PROPERTY = "ambari.dispatch.snmp.security.level";

  private Snmp snmp;

  private final Integer port;
  private volatile UdpTransportMapping transportMapping;

  public SNMPDispatcher(Snmp snmp) {
    this.port = null;
    this.snmp = snmp;
  }

  public SNMPDispatcher() throws IOException {
    this((Integer) null);
  }

  /**
   * Creates SNMP server with specified port. In case port is null will be used random value as default
   * @param port port
   * @throws IOException
   */
  public SNMPDispatcher(Integer port) throws IOException {
    if(port != null && port >= 0 && port <= '\uffff') {
      //restrict invalid ports to avoid exception on socket create
      this.port = port;
    } else {
      this.port = null;
    }
  }

  private void createTransportMapping() throws IOException {
    if (transportMapping == null) {
      synchronized (this) {
        if (transportMapping == null) {
          if (port != null) {
            LOG.info("Setting SNMP dispatch port: " + port);
            transportMapping = new DefaultUdpTransportMapping(new UdpAddress(port), true);
          } else {
            transportMapping = new DefaultUdpTransportMapping();
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return TargetType.SNMP.name();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isNotificationContentGenerationRequired() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispatch(Notification notification) {
    LOG.info("Sending SNMP trap: {}", notification.Subject);
    try {
      createTransportMapping();
      snmp = new Snmp(transportMapping);
      SnmpVersion snmpVersion = getSnmpVersion(notification.DispatchProperties);
      sendTraps(notification, snmpVersion);
      successCallback(notification);
    } catch (InvalidSnmpConfigurationException ex) {
      LOG.error("Unable to dispatch SNMP trap with invalid configuration. " + ex.getMessage());
      failureCallback(notification);
    } catch (Exception ex) {
      LOG.error("Error occurred during SNMP trap dispatching.", ex);
      failureCallback(notification);
      transportMapping = null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TargetConfigurationResult validateTargetConfig(Map<String, Object> properties) {
    Map<String, String> stringValuesConfig = new HashMap<>(properties.size());
    for (Map.Entry<String, Object> propertyEntry : properties.entrySet()) {
      stringValuesConfig.put(propertyEntry.getKey(), propertyEntry.getValue().toString());
    }
    try {

      for (String property : getSetOfDefaultNeededPropertyNames()) {
        getDispatchProperty(stringValuesConfig, property);
      }

      SnmpVersion snmpVersion = getSnmpVersion(stringValuesConfig);
      switch (snmpVersion) {
        case SNMPv3:
          getDispatchProperty(stringValuesConfig, SECURITY_USERNAME_PROPERTY);
          TrapSecurity securityLevel = getSecurityLevel(stringValuesConfig);
          switch (securityLevel) {
            case AUTH_PRIV:
              getDispatchProperty(stringValuesConfig, SECURITY_PRIV_PASSPHRASE_PROPERTY);
              getDispatchProperty(stringValuesConfig, SECURITY_AUTH_PASSPHRASE_PROPERTY);
              break;
            case AUTH_NOPRIV:
              getDispatchProperty(stringValuesConfig, SECURITY_AUTH_PASSPHRASE_PROPERTY);
              break;
          }
          break;
        case SNMPv2c:
        case SNMPv1:
          getDispatchProperty(stringValuesConfig, COMMUNITY_PROPERTY);
          break;
      }
    } catch (InvalidSnmpConfigurationException ex) {
      return TargetConfigurationResult.invalid(ex.getMessage());
    }
    return TargetConfigurationResult.valid();
  }

  /**
   * @return Set that contains names of properties that are needed for all SNMP configurations.
   */
  protected Set<String> getSetOfDefaultNeededPropertyNames() {
    return new HashSet<>(Arrays.asList(BODY_OID_PROPERTY, SUBJECT_OID_PROPERTY,
            TRAP_OID_PROPERTY, PORT_PROPERTY));
  }

  /**
   * Creates protocol data unit (PDU) with corresponding SNMP version for alert notification.
   * @param notification alert notification to dispatch
   * @param snmpVersion SNMP version
   * @return PDU containing notification info
   * @throws InvalidSnmpConfigurationException if notification's dispatch properties don't contain any of required properties.
   */
  protected PDU prepareTrap(Notification notification, SnmpVersion snmpVersion) throws InvalidSnmpConfigurationException {
    PDU pdu = DefaultPDUFactory.createPDU(snmpVersion.getTargetVersion());
    pdu.setType(snmpVersion.getTrapType());
    // Set trap oid for PDU
    pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(getDispatchProperty(notification.DispatchProperties, TRAP_OID_PROPERTY))));
    // Set notification body and subject for PDU objects with identifiers specified in dispatch properties.
    pdu.add(new VariableBinding(new OID(getDispatchProperty(notification.DispatchProperties, BODY_OID_PROPERTY)), new OctetString(notification.Body)));
    pdu.add(new VariableBinding(new OID(getDispatchProperty(notification.DispatchProperties, SUBJECT_OID_PROPERTY)), new OctetString(notification.Subject)));
    return pdu;
  }

  /**
   * Creates trap based on alerts notification and sends it to hosts specified in recipients list.
   * @param notification alert notification to dispatch
   * @param snmpVersion SNMP version
   * @throws InvalidSnmpConfigurationException if notification's dispatch properties don't contain any of required properties or recipient list is empty.
   * @throws IOException if the SNMP trap could not be sent
   */
  protected void sendTraps(Notification notification, SnmpVersion snmpVersion) throws InvalidSnmpConfigurationException, IOException {
    PDU trap = prepareTrap(notification, snmpVersion);
    String udpPort = getDispatchProperty(notification.DispatchProperties, PORT_PROPERTY);
    for (Recipient recipient : getNotificationRecipients(notification)) {
      String address = recipient.Identifier;
      Target target = createTrapTarget(notification, snmpVersion);
      target.setAddress(new UdpAddress(address + "/" + udpPort));
      snmp.send(trap, target);
    }
  }

  /**
   * Creates snmp target with security model corresponding to snmp version.
   * @param notification alerts notification
   * @param snmpVersion SNMP version
   * @return target with corresponding security model
   * @throws InvalidSnmpConfigurationException if notification's dispatch properties don't contain any of required properties
   */
  protected Target createTrapTarget(Notification notification, SnmpVersion snmpVersion) throws InvalidSnmpConfigurationException {
    if (snmpVersion.isCommunityTargetRequired()) {
      OctetString community = new OctetString(getDispatchProperty(notification.DispatchProperties, COMMUNITY_PROPERTY));
      CommunityTarget communityTarget = new CommunityTarget();
      communityTarget.setCommunity(community);
      communityTarget.setVersion(snmpVersion.getTargetVersion());
      return communityTarget;
    } else {
      OctetString userName = new OctetString(getDispatchProperty(notification.DispatchProperties, SECURITY_USERNAME_PROPERTY));
      if (snmp.getUSM() == null) {
        // provide User-based Security Model (USM) with user specified
        USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        // authPassphraseProperty and privPassphraseProperty can be null for NoAuthNoPriv security level
        String authPassphraseProperty = notification.DispatchProperties.get(SECURITY_AUTH_PASSPHRASE_PROPERTY);
        String privPassphraseProperty = notification.DispatchProperties.get(SECURITY_PRIV_PASSPHRASE_PROPERTY);
        OctetString authPassphrase = authPassphraseProperty != null ? new OctetString(authPassphraseProperty) : null;
        OctetString privPassphrase = privPassphraseProperty != null ? new OctetString(privPassphraseProperty) : null;
        UsmUser usmUser = new UsmUser(userName, AuthMD5.ID, authPassphrase, PrivDES.ID, privPassphrase);
        usm.addUser(userName, usmUser);
        SecurityModels.getInstance().addSecurityModel(usm);
      }
      UserTarget userTarget = new UserTarget();
      userTarget.setSecurityName(userName);
      userTarget.setSecurityLevel(getSecurityLevel(notification.DispatchProperties).getSecurityLevel());
      userTarget.setSecurityModel(SecurityModel.SECURITY_MODEL_USM);
      userTarget.setVersion(snmpVersion.getTargetVersion());
      return userTarget;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDigestSupported() {
    return false;
  }

  /**
   * Possible SNMP security levels
   */
  protected enum TrapSecurity {

    /**
     * No password authentication and the communications between the agent and the server are not encrypted.
     */
    NOAUTH_NOPRIV(SecurityLevel.NOAUTH_NOPRIV),
    /**
     * Password authentication is hash based and no encryption is used for communications between the hosts.
     */
    AUTH_NOPRIV(SecurityLevel.AUTH_NOPRIV),
    /**
     * Password authentication is hash based and the communications between the agent and the server are also encrypted.
     */
    AUTH_PRIV(SecurityLevel.AUTH_PRIV);

    int securityLevel;

    TrapSecurity(int securityLevel) {
      this.securityLevel = securityLevel;
    }

    public int getSecurityLevel() {
      return securityLevel;
    }
  }

  /**
   * Supported versions of SNMP
   */
  protected enum SnmpVersion {

    SNMPv1(PDU.V1TRAP, SnmpConstants.version1, true),
    SNMPv2c(PDU.TRAP, SnmpConstants.version2c, true),
    SNMPv3(PDU.TRAP, SnmpConstants.version3, false);

    private int trapType;
    private int targetVersion;
    private boolean communityTargetRequired;

    SnmpVersion(int trapType, int targetVersion, boolean communityTargetRequired) {
      this.trapType = trapType;
      this.targetVersion = targetVersion;
      this.communityTargetRequired = communityTargetRequired;
    }

    public int getTrapType() {
      return trapType;
    }

    public int getTargetVersion() {
      return targetVersion;
    }

    public boolean isCommunityTargetRequired() {
      return communityTargetRequired;
    }
  }

  /**
   * Exception thrown when Notification configuration doesn't contain required properties.
   */
  protected static class InvalidSnmpConfigurationException extends Exception {

    public InvalidSnmpConfigurationException(String message) {
      super(message);
    }
  }

  /**
   * Get list of recipients for notification
   * @param notification alerts notification
   * @return list of recipients
   * @throws InvalidSnmpConfigurationException if recipients is <code>null</code> or empty
   */
  private List<Recipient> getNotificationRecipients(Notification notification) throws InvalidSnmpConfigurationException {
    if (notification.Recipients == null || notification.Recipients.isEmpty()) {
      throw new InvalidSnmpConfigurationException("Destination addresses should be set.");
    }
    return notification.Recipients;
  }

  /**
   * Get dispatch property with specific key from dispatch properties.
   * @param dispatchProperties dispatch properties
   * @param key property key
   * @return property value
   * @throws InvalidSnmpConfigurationException if property with such key does not exist
   */
  protected static String getDispatchProperty(Map<String, String> dispatchProperties, String key) throws InvalidSnmpConfigurationException {
    if (dispatchProperties == null || !dispatchProperties.containsKey(key)) {
      throw new InvalidSnmpConfigurationException(String.format("Property \"%s\" should be set.", key));
    }
    return dispatchProperties.get(key);
  }

  /**
   * Returns {@link SnmpVersion} instance corresponding to dispatch property <code>ambari.dispatch.snmp.version</code> from dispatch properties.
   * @param dispatchProperties dispatch properties
   * @return corresponding SnmpVersion instance
   * @throws InvalidSnmpConfigurationException if dispatch properties doesn't contain required property
   */
  protected SnmpVersion getSnmpVersion(Map<String, String> dispatchProperties) throws InvalidSnmpConfigurationException {
    String snmpVersion = getDispatchProperty(dispatchProperties, SNMP_VERSION_PROPERTY);
    try {
      return SnmpVersion.valueOf(snmpVersion);
    } catch (IllegalArgumentException ex) {
      String errorMessage = String.format("Incorrect SNMP version - \"%s\". Possible values for \"%s\": %s",
          snmpVersion, SNMP_VERSION_PROPERTY, Arrays.toString(SnmpVersion.values()));
      throw new InvalidSnmpConfigurationException(errorMessage);
    }
  }

  /**
   * Returns {@link TrapSecurity} instance corresponding to dispatch property <code>ambari.dispatch.snmp.security.level</code> from dispatch properties.
   * @param dispatchProperties dispatch properties
   * @return corresponding TrapSecurity instance
   * @throws InvalidSnmpConfigurationException if dispatch properties doesn't contain required property
   */
  protected TrapSecurity getSecurityLevel(Map<String, String> dispatchProperties) throws InvalidSnmpConfigurationException {
    String securityLevel = getDispatchProperty(dispatchProperties, SECURITY_LEVEL_PROPERTY);
    try {
      return TrapSecurity.valueOf(securityLevel);
    } catch (IllegalArgumentException ex) {
      String errorMessage = String.format("Incorrect security level for trap - \"%s\". Possible values for \"%s\": %s",
          securityLevel, SECURITY_LEVEL_PROPERTY, Arrays.toString(TrapSecurity.values()));
      throw new InvalidSnmpConfigurationException(errorMessage);
    }
  }

  private void failureCallback(Notification notification) {
    if (notification.Callback != null) {
      notification.Callback.onFailure(notification.CallbackIds);
    }
  }

  private void successCallback(Notification notification) {
    if (notification.Callback != null) {
      notification.Callback.onSuccess(notification.CallbackIds);
    }
  }

  public Integer getPort() {
    return port;
  }

  protected UdpTransportMapping getTransportMapping() {
    return transportMapping;
  }
}
