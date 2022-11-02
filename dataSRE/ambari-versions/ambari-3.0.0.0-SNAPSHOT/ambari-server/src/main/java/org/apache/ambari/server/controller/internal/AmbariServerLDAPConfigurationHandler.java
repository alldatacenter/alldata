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

package org.apache.ambari.server.controller.internal;

import static org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType.LDAP_CONFIGURATIONS;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.LDAP_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.AMBARI_MANAGES_LDAP_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.LDAP_ENABLED_SERVICES;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.configuration.AmbariServerConfigurationCategory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * AmbariServerLDAPConfigurationHandler handles Ambari server LDAP-specific configuration properties.
 */
@Singleton
public class AmbariServerLDAPConfigurationHandler extends AmbariServerStackAdvisorAwareConfigurationHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerLDAPConfigurationHandler.class);

  private final LdapFacade ldapFacade;
  private final Encryptor<AmbariServerConfiguration> encryptor;

  @Inject
  AmbariServerLDAPConfigurationHandler(Clusters clusters, ConfigHelper configHelper, AmbariManagementController managementController,
      StackAdvisorHelper stackAdvisorHelper, AmbariConfigurationDAO ambariConfigurationDAO, AmbariEventPublisher publisher,
      LdapFacade ldapFacade, @Named("AmbariServerConfigurationEncryptor") Encryptor<AmbariServerConfiguration> encryptor) {
    super(ambariConfigurationDAO, publisher, clusters, configHelper, managementController, stackAdvisorHelper);
    this.ldapFacade = ldapFacade;
    this.encryptor = encryptor;
  }
  
  @Override
  public void updateComponentCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified) throws AmbariException {
    final AmbariLdapConfiguration ldapConfiguration = new AmbariLdapConfiguration(properties);
    encryptor.encryptSensitiveData(ldapConfiguration);
    super.updateComponentCategory(categoryName, ldapConfiguration.toMap(), removePropertiesIfNotSpecified);
    if (ldapConfiguration.isAmbariManagesLdapConfiguration()) {
      processClusters(LDAP_CONFIGURATIONS);
    }
  }

  /**
   * Gets the set of services for which the user declared  Ambari to enable LDAP integration.
   * <p>
   * If Ambari is not managing LDAP integration configuration for services the set of names will be empty.
   *
   * @return a set of service names
   */
  public Set<String> getLDAPEnabledServices() {
    return getEnabledServices(LDAP_CONFIGURATION.getCategoryName(), AMBARI_MANAGES_LDAP_CONFIGURATION.key(), LDAP_ENABLED_SERVICES.key());
  }

  @Override
  public OperationResult performOperation(String categoryName, Map<String, String> properties,
                                          boolean mergeExistingProperties, String operation, Map<String, Object> operationParameters) throws SystemException {

    if (!AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName().equals(categoryName)) {
      throw new SystemException(String.format("Unexpected category name for Ambari server LDAP properties: %s", categoryName));
    }

    OperationType operationType;
    try {
      operationType = OperationType.translate(operation);
    } catch (IllegalArgumentException e) {
      throw new SystemException(String.format("The requested operation is not supported for this category: %s", categoryName), e);
    }

    Map<String, String> ldapConfigurationProperties = new HashMap<>();

    // If we need to merge with the properties of an existing ldap-configuration property set, attempt
    // to retrieve if. If one does not exist, that is ok.
    if (mergeExistingProperties) {
      Map<String, String> _ldapProperties = getConfigurationProperties(categoryName);
      if (_ldapProperties != null) {
        ldapConfigurationProperties.putAll(_ldapProperties);
      }
    }

    if (properties != null) {
      ldapConfigurationProperties.putAll(properties);
    }

    AmbariLdapConfiguration ambariLdapConfiguration = new AmbariLdapConfiguration(ldapConfigurationProperties);

    boolean success = true;
    String message = null;
    Object resultData = null;

    try {
      switch (operationType) {
        case TEST_CONNECTION:
          LOGGER.debug("Testing connection to the LDAP server ...");
          ldapFacade.checkConnection(ambariLdapConfiguration);
          break;

        case TEST_ATTRIBUTES:
          LOGGER.debug("Testing LDAP attributes ....");
          Set<String> groups = ldapFacade.checkLdapAttributes(operationParameters, ambariLdapConfiguration);
          resultData = Collections.singletonMap("groups", groups);
          break;

        case DETECT_ATTRIBUTES:
          LOGGER.info("Detecting LDAP attributes ...");
          ambariLdapConfiguration = ldapFacade.detectAttributes(ambariLdapConfiguration);
          resultData = Collections.singletonMap("attributes", ambariLdapConfiguration.toMap());
          break;

        default:
          LOGGER.warn("No action provided ...");
          throw new IllegalArgumentException("No request action provided");
      }
    } catch (AmbariLdapException e) {
      success = false;
      message = determineCause(e);
      if (StringUtils.isEmpty(message)) {
        message = "An unexpected error has occurred.";
      }

      LOGGER.warn(String.format("Failed to perform %s: %s", operationType.name(), message), e);
    }

    return new OperationResult(operationType.getOperation(), success, message, resultData);
  }

  private String determineCause(Throwable throwable) {
    if (throwable == null) {
      return null;
    } else {
      Throwable cause = throwable.getCause();
      if ((cause == null) || (cause == throwable)) {
        return throwable.getMessage();
      } else {
        String message = determineCause(cause);
        return (message == null) ? throwable.getMessage() : message;
      }
    }
  }

  @Override
  protected String getServiceVersionNote() {
    return "Ambari managed LDAP configurations";
  }

  enum OperationType {
    TEST_CONNECTION("test-connection"),
    TEST_ATTRIBUTES("test-attributes"),
    DETECT_ATTRIBUTES("detect-attributes");

    private final String operation;

    OperationType(String operation) {
      this.operation = operation;
    }

    public String getOperation() {
      return operation;
    }

    public static OperationType translate(String operation) {
      if (!StringUtils.isEmpty(operation)) {
        operation = operation.trim();
        for (OperationType category : values()) {
          if (category.getOperation().equals(operation)) {
            return category;
          }
        }
      }

      throw new IllegalArgumentException(String.format("Invalid operation for %s: %s", AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName(), operation));
    }

    public static String translate(OperationType operation) {
      return (operation == null) ? null : operation.getOperation();
    }
  }
}
