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

import java.util.Collection;
import java.util.Map;

import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Ambari-server class that is called as an entry point to encrypt service configuration sensitive data
 */
public class SensitiveDataEncryption {
  private static final Logger LOG = LoggerFactory.getLogger
      (SensitiveDataEncryption.class);

  private final PersistService persistService;
  private final Injector injector;


  @Inject
  public SensitiveDataEncryption(Injector injector,
                                 PersistService persistService) {
    this.injector = injector;
    this.persistService = persistService;
  }

  /**
   * Extension of main controller module
   */
  private static class EncryptionHelperControllerModule extends ControllerModule {

    public EncryptionHelperControllerModule() throws Exception {
    }

    @Override
    protected void configure() {
      super.configure();
      EventBusSynchronizer.synchronizeAmbariEventPublisher(binder());
    }
  }

  /**
   * Extension of audit logger module
   */
  private static class EncryptionHelperAuditModule extends AuditLoggerModule {

    @Override
    protected void configure() {
      super.configure();
    }

  }

  public void startPersistenceService() {
    persistService.start();
  }

  public void stopPersistenceService() {
    persistService.stop();
  }

  /**
   * Iterates thought all configs and encryption/decryption sensitive properties according to action
   * using the default configured masterkey
   * @param args "encryption" or "decryption" action expected
   */
  public static void main(String[] args) {
    if (args.length < 1 || (!"encryption".equals(args[0]) && !"decryption".equals(args[0] ))){
      LOG.error("The action parameter (\"encryption\" or \"decryption\") is required");
      System.exit(-1);
    }
    boolean encrypt = "encryption".equals(args[0]);
    SensitiveDataEncryption sensitiveDataEncryption = null;
    try {
      Injector injector = Guice.createInjector(new EncryptionHelperControllerModule(), new EncryptionHelperAuditModule(), new LdapModule());
      sensitiveDataEncryption = injector.getInstance(SensitiveDataEncryption.class);
      sensitiveDataEncryption.startPersistenceService();
      sensitiveDataEncryption.doEncryption(encrypt);
    } catch (Throwable e) {
      LOG.error("Exception occurred during config encryption/decryption:", e);
    } finally {
      if (sensitiveDataEncryption != null) {
        sensitiveDataEncryption.stopPersistenceService();
      }
    }
  }

  /**
   * @param encrypt selects mode: true=encrypt, false=decrypt
   */
  public void doEncryption(boolean encrypt) {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Encryptor<Config> configEncryptor = injector.getInstance(ConfigPropertiesEncryptor.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Collection<Config> configs = cluster.getAllConfigs();
          for (Config config : configs) {
            if (encrypt) {
              configEncryptor.encryptSensitiveData(config);
            } else {
              configEncryptor.decryptSensitiveData(config);
            }
            config.save();
          }
        }
      }
    }
  }
}
