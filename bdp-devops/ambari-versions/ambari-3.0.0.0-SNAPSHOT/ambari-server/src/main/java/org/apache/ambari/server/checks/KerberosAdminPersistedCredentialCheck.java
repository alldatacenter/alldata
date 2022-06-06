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
package org.apache.ambari.server.checks;

import static org.apache.ambari.server.stack.upgrade.Task.Type.REGENERATE_KEYTABS;

import java.util.Collections;
import java.util.Set;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * The {@link KerberosAdminPersistedCredentialCheck} class is used to check that the Kerberos
 * administrator credentials are stored in the persisted credential store when Kerberos is enabled.
 * <p>
 * This is needed so that if Kerberos principals and/or keytab files need to be created during a stack
 * upgrade, the KDC administrator credentials are guaranteed to be available.  If the temporary store
 * is used, the credential may have expired before needed.
 */
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.KERBEROS,
    required = {UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED})
public class KerberosAdminPersistedCredentialCheck extends ClusterCheck {
  private static final Logger LOG = LoggerFactory.getLogger(KerberosAdminPersistedCredentialCheck.class);
  public static final String KEY_PERSISTED_STORE_NOT_CONFIGURED = "persisted_store_no_configured";

  public static final String KEY_CREDENTIAL_NOT_STORED = "admin_credential_not_stored";

  @Inject
  private CredentialStoreService credentialStoreService;

  @Inject
  private UpgradeHelper upgradeHelper;

  static final UpgradeCheckDescription KERBEROS_ADMIN_CREDENTIAL_CHECK = new UpgradeCheckDescription("KERBEROS_ADMIN_CREDENTIAL_CHECK",
      UpgradeCheckType.CLUSTER,
      "The KDC administrator credentials need to be stored in Ambari persisted credential store.",
      new ImmutableMap.Builder<String, String>()
          .put(KerberosAdminPersistedCredentialCheck.KEY_PERSISTED_STORE_NOT_CONFIGURED,
              "Ambari's credential store has not been configured.  " +
                  "This is needed so the KDC administrator credential may be stored long enough to ensure it will be around if needed during the upgrade process.")
          .put(KerberosAdminPersistedCredentialCheck.KEY_CREDENTIAL_NOT_STORED,
              "The KDC administrator credential has not been stored in the persisted credential store. " +
                  "Visit the Kerberos administrator page to set the credential. " +
                  "This is needed so the KDC administrator credential may be stored long enough to ensure it will be around if needed during the upgrade process.")
          .build());

  /**
   * Constructor.
   */
  @Inject
  public KerberosAdminPersistedCredentialCheck() {
    super(KERBEROS_ADMIN_CREDENTIAL_CHECK);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getApplicableServices() {
    return Collections.emptySet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);

    // Perform the check only if Kerberos is enabled
    if (cluster.getSecurityType() != SecurityType.KERBEROS) {
      return result;
    }

    if (!upgradePack(request).anyGroupTaskMatch(task -> task.getType() == REGENERATE_KEYTABS)) {
      LOG.info("Skipping upgrade check {} because there is no {} in the upgrade pack.", this.getClass().getSimpleName(), REGENERATE_KEYTABS);
      return result;
    }

    // Perform the check only if Ambari is managing the Kerberos identities
    if (!"true".equalsIgnoreCase(getProperty(request, "kerberos-env", "manage_identities"))) {
      return result;
    }

    if (!credentialStoreService.isInitialized(CredentialStoreType.PERSISTED)) {
      // The persisted store is not available
      result.setFailReason(getFailReason(KEY_PERSISTED_STORE_NOT_CONFIGURED, result, request));
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.getFailedOn().add(request.getClusterName());
    } else if (credentialStoreService.getCredential(clusterName, KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS, CredentialStoreType.PERSISTED) == null) {
      // The KDC administrator credential has not been stored in the persisted credential store
      result.setFailReason(getFailReason(KEY_CREDENTIAL_NOT_STORED, result, request));
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.getFailedOn().add(request.getClusterName());
    }

    return result;
  }

  private UpgradePack upgradePack(UpgradeCheckRequest request) throws AmbariException {
    Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());

    return upgradeHelper.suggestUpgradePack(
      request.getClusterName(),
      cluster.getCurrentStackVersion(),
      new StackId(request.getTargetRepositoryVersion().getStackId()),
      Direction.UPGRADE,
      request.getUpgradeType(),
      null);
  }

}
