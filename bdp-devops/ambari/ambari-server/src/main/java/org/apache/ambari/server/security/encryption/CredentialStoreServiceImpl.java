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

package org.apache.ambari.server.security.encryption;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.SecurePasswordHelper;
import org.apache.ambari.server.security.credential.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CredentialStoreServiceImpl implements CredentialStoreService {

  private static final Logger LOG = LoggerFactory.getLogger(CredentialStoreServiceImpl.class);

  private SecurePasswordHelper securePasswordHelper;

  private FileBasedCredentialStore persistedCredentialStore = null;
  private InMemoryCredentialStore temporaryCredentialStore = null;


  @Inject
  public CredentialStoreServiceImpl(Configuration configuration, SecurePasswordHelper securePasswordHelper) {

    this.securePasswordHelper = securePasswordHelper;

    if (configuration != null) {
      try {
        initializeTemporaryCredentialStore(configuration.getTemporaryKeyStoreRetentionMinutes(),
            TimeUnit.MINUTES,
            configuration.isActivelyPurgeTemporaryKeyStore());
        LOG.info("Initialized the temporary credential store. KeyStore entries will be retained for {} minutes and {} be actively purged",
            configuration.getTemporaryKeyStoreRetentionMinutes(), (configuration.isActivelyPurgeTemporaryKeyStore()) ? "will" : "will not");
      } catch (AmbariException e) {
        LOG.error("Failed to initialize the temporary credential store.  Storage of temporary credentials will fail.", e);
      }

      // If the MasterKeyService is initialized, assume that we should be initializing the persistent
      // CredentialStore; else do not initialize it.
      MasterKeyService masterKeyService = new MasterKeyServiceImpl(configuration);
      if (masterKeyService.isMasterKeyInitialized()) {
        try {
          initializePersistedCredentialStore(configuration.getMasterKeyStoreLocation(), masterKeyService);
          LOG.info("Initialized the persistent credential store. Using KeyStore file at {}", persistedCredentialStore.getKeyStorePath().getAbsolutePath());
        } catch (AmbariException e) {
          LOG.error("Failed to initialize the persistent credential store.  Storage of persisted credentials will fail.", e);
        }
      }
    }
  }

  public synchronized void initializeTemporaryCredentialStore(long retentionDuration, TimeUnit units, boolean activelyPurge) throws AmbariException {
    if (isInitialized(CredentialStoreType.TEMPORARY)) {
      throw new AmbariException("This temporary CredentialStore has already been initialized");
    }

    temporaryCredentialStore = new InMemoryCredentialStore(retentionDuration, units, activelyPurge);
    temporaryCredentialStore.setMasterKeyService(new MasterKeyServiceImpl(securePasswordHelper.createSecurePassword()));
  }

  public synchronized void initializePersistedCredentialStore(File credentialStoreLocation, MasterKeyService masterKeyService) throws AmbariException {
    if (isInitialized(CredentialStoreType.PERSISTED)) {
      throw new AmbariException("This persisted CredentialStore has already been initialized");
    }

    persistedCredentialStore = new FileBasedCredentialStore(credentialStoreLocation);
    persistedCredentialStore.setMasterKeyService(masterKeyService);
  }

  /**
   * Adds a new credential to either the persistent or the temporary CredentialStore
   * <p/>
   * The supplied key will be converted into UTF-8 bytes before being stored.
   * <p/>
   * The alias name will be canonicalized as follows:
   * <ul>
   * <li>if a cluster name is supplied, then "cluster.name." will be prepended to it</li>
   * <li>the characters will converted to all lowercase</li>
   * </ul>
   * Only a single instance of the named credential will be stored, therefore if a credential with
   * alias ALIAS1 is stored in the persisted CredentialStore and a call is made to store a credentials
   * with alias ALIAS1 into the temporary CredentialStore, the instance in the persisted CredentialStore
   * will be removed.
   *
   * @param clusterName         the name of the cluster this credential is related to
   * @param alias               a string declaring the alias (or name) of the credential
   * @param credential          the credential value to store
   * @param credentialStoreType a CredentialStoreType indicating which credential store facility to use
   * @throws AmbariException
   */
  @Override
  public void setCredential(String clusterName, String alias, Credential credential, CredentialStoreType credentialStoreType) throws AmbariException {
    validateInitialized(credentialStoreType);

    // Ensure only one copy of this alias exists.. either in the persisted or the temporary CertificateStore
    removeCredential(clusterName, alias);
    getCredentialStore(credentialStoreType).addCredential(canonicalizeAlias(clusterName, alias), credential);
  }

  @Override
  public Credential getCredential(String clusterName, String alias) throws AmbariException {
    // First check the temporary CredentialStore
    Credential credential = getCredential(clusterName, alias, CredentialStoreType.TEMPORARY);

    if (credential == null) {
      // If needed, check the persisted CredentialStore
      credential = getCredential(clusterName, alias, CredentialStoreType.PERSISTED);
    }

    return credential;
  }

  @Override
  public Credential getCredential(String clusterName, String alias, CredentialStoreType credentialStoreType) throws AmbariException {
    return (isInitialized(credentialStoreType))
        ? getCredentialStore(credentialStoreType).getCredential(canonicalizeAlias(clusterName, alias))
        : null;
  }

  @Override
  public void removeCredential(String clusterName, String alias) throws AmbariException {
    removeCredential(clusterName, alias, CredentialStoreType.PERSISTED);
    removeCredential(clusterName, alias, CredentialStoreType.TEMPORARY);
  }

  @Override
  public void removeCredential(String clusterName, String alias, CredentialStoreType credentialStoreType) throws AmbariException {
    if (isInitialized(credentialStoreType)) {
      getCredentialStore(credentialStoreType).removeCredential(canonicalizeAlias(clusterName, alias));
    }
  }

  @Override
  public boolean containsCredential(String clusterName, String alias) throws AmbariException {
    return containsCredential(clusterName, alias, CredentialStoreType.TEMPORARY) ||
        containsCredential(clusterName, alias, CredentialStoreType.PERSISTED);
  }

  @Override
  public boolean containsCredential(String clusterName, String alias, CredentialStoreType credentialStoreType) throws AmbariException {
    return isInitialized(credentialStoreType) &&
        getCredentialStore(credentialStoreType).containsCredential(canonicalizeAlias(clusterName, alias));
  }

  @Override
  public CredentialStoreType getCredentialStoreType(String clusterName, String alias) throws AmbariException {
    if (containsCredential(clusterName, alias, CredentialStoreType.TEMPORARY)) {
      return CredentialStoreType.TEMPORARY;
    } else if (containsCredential(clusterName, alias, CredentialStoreType.PERSISTED)) {
      return CredentialStoreType.PERSISTED;
    } else {
      throw new AmbariException("The alias was not found in either the persisted or temporary credential stores");
    }
  }

  @Override
  public Map<String, CredentialStoreType> listCredentials(String clusterName) throws AmbariException {
    if (!isInitialized()) {
      throw new AmbariException("This CredentialStoreService has not yet been initialized");
    }

    Collection<String> persistedAliases = isInitialized(CredentialStoreType.PERSISTED)
        ? persistedCredentialStore.listCredentials()
        : null;

    Collection<String> temporaryAliases = isInitialized(CredentialStoreType.TEMPORARY)
        ? temporaryCredentialStore.listCredentials()
        : null;

    Map<String, CredentialStoreType> map = new HashMap<>();

    if (persistedAliases != null) {
      for (String alias : persistedAliases) {
        if (isAliasRequested(clusterName, alias)) {
          map.put(decanonicalizeAlias(clusterName, alias), CredentialStoreType.PERSISTED);
        }
      }
    }

    if (temporaryAliases != null) {
      for (String alias : temporaryAliases) {
        if (isAliasRequested(clusterName, alias)) {
          map.put(decanonicalizeAlias(clusterName, alias), CredentialStoreType.TEMPORARY);
        }
      }
    }

    return map;
  }

  @Override
  public synchronized boolean isInitialized() {
    return isInitialized(CredentialStoreType.PERSISTED) || isInitialized(CredentialStoreType.TEMPORARY);
  }

  @Override
  public synchronized boolean isInitialized(CredentialStoreType credentialStoreType) {
    if (CredentialStoreType.PERSISTED == credentialStoreType) {
      return persistedCredentialStore != null;
    } else if (CredentialStoreType.TEMPORARY == credentialStoreType) {
      return temporaryCredentialStore != null;
    } else {
      throw new IllegalArgumentException("Invalid or unexpected credential store type specified");
    }
  }

  /**
   * Canonicalizes an alias name by making sure that is contains the prefix indicating what cluster it belongs to.
   * This helps to reduce collisions of alias between clusters pointing to the same keystore files.
   * <p/>
   * Each alias is expected to have a prefix of <code>cluster.:clusterName.</code>, and the
   * combination is to be converted to have all lowercase characters.  For example if the alias was
   * "external.DB" and the cluster name is "c1", then the canonicalized alias name would be
   * "cluster.c1.external.db".
   *
   * @param clusterName the name of the cluster
   * @param alias       a string declaring the alias (or name) of the credential
   * @return a ccanonicalized alias name
   */
  public static String canonicalizeAlias(String clusterName, String alias) {
    String canonicaizedAlias;

    if ((clusterName == null) || clusterName.isEmpty() || (alias == null) || alias.isEmpty()) {
      canonicaizedAlias = alias;
    } else {
      String prefix = createAliasPrefix(clusterName);

      if (alias.toLowerCase().startsWith(prefix)) {
        canonicaizedAlias = alias;
      } else {
        canonicaizedAlias = prefix + alias;
      }
    }

    return (canonicaizedAlias == null)
        ? null
        : canonicaizedAlias.toLowerCase();
  }

  /**
   * Removes the prefix (if exists) from the front of a canonicalized alias
   *
   * @param clusterName       the name the name of the cluster
   * @param canonicaizedAlias the canonicalized alias to process
   * @return an alias name
   */
  public static String decanonicalizeAlias(String clusterName, String canonicaizedAlias) {
    if ((clusterName == null) || clusterName.isEmpty() || (canonicaizedAlias == null) || canonicaizedAlias.isEmpty()) {
      return canonicaizedAlias;
    } else {
      String prefix = createAliasPrefix(clusterName);

      if (canonicaizedAlias.startsWith(prefix)) {
        return canonicaizedAlias.substring(prefix.length());
      } else {
        return canonicaizedAlias;
      }
    }
  }

  /**
   * Creates the prefix that is to be set in a canonicalized alias name.
   *
   * @param clusterName the name of the cluster
   * @return the prefix value
   */
  private static String createAliasPrefix(String clusterName) {
    return ("cluster." + clusterName + ".").toLowerCase();
  }

  /**
   * Tests the canonicalized alias name to see if it should be returned within the set of credentials.
   * <p/>
   * This filters out all credentials not tagged for a specific cluster.
   *
   * @param clusterName        the name of the cluster
   * @param canonicalizedAlias the canonicalized alias
   * @return true if the alias is tagged for the requested cluster; otherwise false
   */
  private boolean isAliasRequested(String clusterName, String canonicalizedAlias) {
    return (clusterName == null) || canonicalizedAlias.toLowerCase().startsWith(createAliasPrefix(clusterName));
  }


  /**
   * Gets either the persisted or temporary CredentialStore as requested
   *
   * @param credentialStoreType a CredentialStoreType indicating which credential store facility to use
   * @return a CredentialStore implementation
   */
  private CredentialStore getCredentialStore(CredentialStoreType credentialStoreType) {
    if (CredentialStoreType.PERSISTED == credentialStoreType) {
      return persistedCredentialStore;
    } else if (CredentialStoreType.TEMPORARY == credentialStoreType) {
      return temporaryCredentialStore;
    } else {
      throw new IllegalArgumentException("Invalid or unexpected credential store type specified");
    }
  }

  /**
   * Validate the relevant storage facility is initialized.
   *
   * @param credentialStoreType a CredentialStoreType indicating which credential store facility to use
   * @throws AmbariException if the requested store has not been initialized
   */
  private void validateInitialized(CredentialStoreType credentialStoreType) throws AmbariException {
    if (!isInitialized(credentialStoreType)) {
      throw new AmbariException(String.format("The %s CredentialStore for this CredentialStoreService has not yet been initialized",
          credentialStoreType.name().toLowerCase())
      );
    }
  }
}
