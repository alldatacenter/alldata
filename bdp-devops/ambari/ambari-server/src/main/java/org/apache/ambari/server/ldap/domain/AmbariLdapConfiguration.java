/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.domain;

import static java.lang.Boolean.parseBoolean;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.configuration.AmbariServerConfigurationCategory;
import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.configuration.LdapUsernameCollisionHandlingBehavior;
import org.apache.ambari.server.security.authorization.LdapServerProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * This class is a representation of all the LDAP related configuration properties.
 * <p>
 * <strong>IMPORTANT: </strong>in case you declare any new LDAP related property
 * please do it in the Python class
 * <code>stacks.ambari_configuration.AmbariLDAPConfiguration</code> too.
 */
public class AmbariLdapConfiguration extends AmbariServerConfiguration {

  public AmbariLdapConfiguration() {
    this(null);
  }

  public AmbariLdapConfiguration(Map<String, String> configurationMap) {
    super(configurationMap);
  }
  
  @Override
  protected AmbariServerConfigurationCategory getCategory() {
    return AmbariServerConfigurationCategory.LDAP_CONFIGURATION;
  }

  public boolean isAmbariManagesLdapConfiguration() {
    return Boolean.valueOf(configValue(AmbariServerConfigurationKey.AMBARI_MANAGES_LDAP_CONFIGURATION));
  }

  public String getLdapEnabledServices() {
    return configValue(AmbariServerConfigurationKey.LDAP_ENABLED_SERVICES);
  }

  public boolean ldapEnabled() {
    return Boolean.valueOf(configValue(AmbariServerConfigurationKey.LDAP_ENABLED));
  }

  public String serverHost() {
    return configValue(AmbariServerConfigurationKey.SERVER_HOST);
  }

  public int serverPort() {
    return Integer.parseInt(configValue(AmbariServerConfigurationKey.SERVER_PORT));
  }

  public String serverUrl() {
    return serverHost() + ":" + serverPort();
  }

  public String secondaryServerHost() {
    return configValue(AmbariServerConfigurationKey.SECONDARY_SERVER_HOST);
  }

  public int secondaryServerPort() {
    final String secondaryServerPort = configValue(AmbariServerConfigurationKey.SECONDARY_SERVER_PORT);
    return secondaryServerPort == null ? 0 : Integer.parseInt(secondaryServerPort);
  }

  public String secondaryServerUrl() {
    return secondaryServerHost() + ":" + secondaryServerPort();
  }

  public boolean useSSL() {
    return Boolean.valueOf(configValue(AmbariServerConfigurationKey.USE_SSL));
  }

  public String trustStore() {
    return configValue(AmbariServerConfigurationKey.TRUST_STORE);
  }

  public String trustStoreType() {
    return configValue(AmbariServerConfigurationKey.TRUST_STORE_TYPE);
  }

  public String trustStorePath() {
    return configValue(AmbariServerConfigurationKey.TRUST_STORE_PATH);
  }

  public String trustStorePassword() {
    return configValue(AmbariServerConfigurationKey.TRUST_STORE_PASSWORD);
  }

  public boolean anonymousBind() {
    return Boolean.valueOf(configValue(AmbariServerConfigurationKey.ANONYMOUS_BIND));
  }

  public String bindDn() {
    return configValue(AmbariServerConfigurationKey.BIND_DN);
  }

  public String bindPassword() {
    return configValue(AmbariServerConfigurationKey.BIND_PASSWORD);
  }

  public String attributeDetection() {
    return configValue(AmbariServerConfigurationKey.ATTR_DETECTION);
  }

  public String dnAttribute() {
    return configValue(AmbariServerConfigurationKey.DN_ATTRIBUTE);
  }

  public String userObjectClass() {
    return configValue(AmbariServerConfigurationKey.USER_OBJECT_CLASS);
  }

  public String userNameAttribute() {
    return configValue(AmbariServerConfigurationKey.USER_NAME_ATTRIBUTE);
  }

  public String userSearchBase() {
    return configValue(AmbariServerConfigurationKey.USER_SEARCH_BASE);
  }

  public String groupObjectClass() {
    return configValue(AmbariServerConfigurationKey.GROUP_OBJECT_CLASS);
  }

  public String groupNameAttribute() {
    return configValue(AmbariServerConfigurationKey.GROUP_NAME_ATTRIBUTE);
  }

  public String groupMemberAttribute() {
    return configValue(AmbariServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE);
  }

  public String groupSearchBase() {
    return configValue(AmbariServerConfigurationKey.GROUP_SEARCH_BASE);
  }

  public String groupMappingRules() {
    return configValue(AmbariServerConfigurationKey.GROUP_MAPPING_RULES);
  }

  public String userSearchFilter() {
    return configValue(AmbariServerConfigurationKey.USER_SEARCH_FILTER);
  }

  public String userMemberReplacePattern() {
    return configValue(AmbariServerConfigurationKey.USER_MEMBER_REPLACE_PATTERN);
  }

  public String userMemberFilter() {
    return configValue(AmbariServerConfigurationKey.USER_MEMBER_FILTER);
  }

  public String groupSearchFilter() {
    return configValue(AmbariServerConfigurationKey.GROUP_SEARCH_FILTER);
  }

  public String groupMemberReplacePattern() {
    return configValue(AmbariServerConfigurationKey.GROUP_MEMBER_REPLACE_PATTERN);
  }

  public String groupMemberFilter() {
    return configValue(AmbariServerConfigurationKey.GROUP_MEMBER_FILTER);
  }

  public boolean forceLowerCaseUserNames() {
    return Boolean.valueOf(configValue(AmbariServerConfigurationKey.FORCE_LOWERCASE_USERNAMES));
  }

  public boolean paginationEnabled() {
    return Boolean.valueOf(configValue(AmbariServerConfigurationKey.PAGINATION_ENABLED));
  }

  public String referralHandling() {
    return configValue(AmbariServerConfigurationKey.REFERRAL_HANDLING);
  }

  public boolean disableEndpointIdentification() {
    return Boolean.valueOf(configValue(AmbariServerConfigurationKey.DISABLE_ENDPOINT_IDENTIFICATION));
  }

  @Override
  public Map<String, String> toMap() {
    return new HashMap<>(configurationMap);
  }

  @Override
  public String toString() {
    return configurationMap.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AmbariLdapConfiguration that = (AmbariLdapConfiguration) o;

    return new EqualsBuilder().append(configurationMap, that.configurationMap).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(configurationMap).toHashCode();
  }

  public boolean isLdapAlternateUserSearchEnabled() {
    return Boolean.valueOf(configValue(AmbariServerConfigurationKey.ALTERNATE_USER_SEARCH_ENABLED));
  }

  public LdapServerProperties getLdapServerProperties() {
    final LdapServerProperties ldapServerProperties = new LdapServerProperties();

    ldapServerProperties.setPrimaryUrl(serverUrl());
    if (StringUtils.isNotBlank(secondaryServerHost())) {
      ldapServerProperties.setSecondaryUrl(secondaryServerUrl());
    }
    ldapServerProperties.setUseSsl(parseBoolean(configValue(AmbariServerConfigurationKey.USE_SSL)));
    ldapServerProperties.setAnonymousBind(parseBoolean(configValue(AmbariServerConfigurationKey.ANONYMOUS_BIND)));
    ldapServerProperties.setManagerDn(configValue(AmbariServerConfigurationKey.BIND_DN));
    ldapServerProperties.setManagerPassword(configValue(AmbariServerConfigurationKey.BIND_PASSWORD));
    ldapServerProperties.setBaseDN(configValue(AmbariServerConfigurationKey.USER_SEARCH_BASE));
    ldapServerProperties.setUsernameAttribute(configValue(AmbariServerConfigurationKey.USER_NAME_ATTRIBUTE));
    ldapServerProperties.setForceUsernameToLowercase(parseBoolean(configValue(AmbariServerConfigurationKey.FORCE_LOWERCASE_USERNAMES)));
    ldapServerProperties.setUserBase(configValue(AmbariServerConfigurationKey.USER_BASE));
    ldapServerProperties.setUserObjectClass(configValue(AmbariServerConfigurationKey.USER_OBJECT_CLASS));
    ldapServerProperties.setDnAttribute(configValue(AmbariServerConfigurationKey.DN_ATTRIBUTE));
    ldapServerProperties.setGroupBase(configValue(AmbariServerConfigurationKey.GROUP_BASE));
    ldapServerProperties.setGroupObjectClass(configValue(AmbariServerConfigurationKey.GROUP_OBJECT_CLASS));
    ldapServerProperties.setGroupMembershipAttr(configValue(AmbariServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE));
    ldapServerProperties.setGroupNamingAttr(configValue(AmbariServerConfigurationKey.GROUP_NAME_ATTRIBUTE));
    ldapServerProperties.setAdminGroupMappingRules(configValue(AmbariServerConfigurationKey.GROUP_MAPPING_RULES));
    ldapServerProperties.setAdminGroupMappingMemberAttr("");
    ldapServerProperties.setUserSearchFilter(configValue(AmbariServerConfigurationKey.USER_SEARCH_FILTER));
    ldapServerProperties.setAlternateUserSearchFilter(configValue(AmbariServerConfigurationKey.ALTERNATE_USER_SEARCH_FILTER));
    ldapServerProperties.setGroupSearchFilter(configValue(AmbariServerConfigurationKey.GROUP_SEARCH_FILTER));
    ldapServerProperties.setReferralMethod(configValue(AmbariServerConfigurationKey.REFERRAL_HANDLING));
    ldapServerProperties.setSyncUserMemberReplacePattern(configValue(AmbariServerConfigurationKey.USER_MEMBER_REPLACE_PATTERN));
    ldapServerProperties.setSyncGroupMemberReplacePattern(configValue(AmbariServerConfigurationKey.GROUP_MEMBER_REPLACE_PATTERN));
    ldapServerProperties.setSyncUserMemberFilter(configValue(AmbariServerConfigurationKey.USER_MEMBER_FILTER));
    ldapServerProperties.setSyncGroupMemberFilter(configValue(AmbariServerConfigurationKey.GROUP_MEMBER_FILTER));
    ldapServerProperties.setPaginationEnabled(parseBoolean(configValue(AmbariServerConfigurationKey.PAGINATION_ENABLED)));
    ldapServerProperties.setDisableEndpointIdentification(disableEndpointIdentification());

    if (hasAnyValueWithKey(AmbariServerConfigurationKey.GROUP_BASE, AmbariServerConfigurationKey.GROUP_OBJECT_CLASS, AmbariServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE,
        AmbariServerConfigurationKey.GROUP_NAME_ATTRIBUTE, AmbariServerConfigurationKey.GROUP_MAPPING_RULES, AmbariServerConfigurationKey.GROUP_SEARCH_FILTER)) {
      ldapServerProperties.setGroupMappingEnabled(true);
    }

    return ldapServerProperties;
  }

  private boolean hasAnyValueWithKey(AmbariServerConfigurationKey... ambariServerConfigurationKey) {
    for (AmbariServerConfigurationKey key : ambariServerConfigurationKey) {
      if (configurationMap.containsKey(key.key())) {
        return true;
      }
    }
    return false;
  }

  public LdapUsernameCollisionHandlingBehavior syncCollisionHandlingBehavior() {
    if ("skip".equalsIgnoreCase(configValue(AmbariServerConfigurationKey.COLLISION_BEHAVIOR))) {
      return LdapUsernameCollisionHandlingBehavior.SKIP;
    }
    return LdapUsernameCollisionHandlingBehavior.CONVERT;
  }

  private String configValue(AmbariServerConfigurationKey ambariManagesLdapConfiguration) {
    return getValue(ambariManagesLdapConfiguration, configurationMap);
  }

}
