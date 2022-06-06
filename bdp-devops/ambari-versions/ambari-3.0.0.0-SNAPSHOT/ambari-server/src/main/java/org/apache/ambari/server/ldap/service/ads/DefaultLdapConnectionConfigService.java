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

package org.apache.ambari.server.ldap.service.ads;

import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.LdapConnectionConfigService;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultLdapConnectionConfigService implements LdapConnectionConfigService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLdapConnectionConfigService.class);

  @Inject
  public DefaultLdapConnectionConfigService() {
  }

  @Override
  public LdapConnectionConfig createLdapConnectionConfig(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {

    LOG.debug("Assembling ldap connection config based on: {}", ambariLdapConfiguration);

    LdapConnectionConfig config = new LdapConnectionConfig();
    config.setLdapHost(ambariLdapConfiguration.serverHost());
    config.setLdapPort(ambariLdapConfiguration.serverPort());
    config.setName(ambariLdapConfiguration.bindDn());
    config.setCredentials(ambariLdapConfiguration.bindPassword());
    config.setUseSsl(ambariLdapConfiguration.useSSL());

    if ("custom".equals(ambariLdapConfiguration.trustStore())) {
      LOG.info("Using custom trust manager configuration");
      config.setTrustManagers(trustManagers(ambariLdapConfiguration));
    }

    return config;
  }


  /**
   * Configure the trust managers to use the custom keystore.
   *
   * @param ambariLdapConfiguration congiguration instance holding current values
   * @return the array of trust managers
   * @throws AmbariLdapException if an error occurs while setting up the connection
   */
  private TrustManager[] trustManagers(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    try {

      TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
      tmFactory.init(keyStore(ambariLdapConfiguration));
      return tmFactory.getTrustManagers();

    } catch (Exception e) {

      LOG.error("Failed to initialize trust managers", e);
      throw new AmbariLdapException(e);

    }

  }

  private KeyStore keyStore(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {

    // validating configuration settings
    if (Strings.isEmpty(ambariLdapConfiguration.trustStoreType())) {
      throw new AmbariLdapException("Key Store Type must be specified");
    }

    if (Strings.isEmpty(ambariLdapConfiguration.trustStorePath())) {
      throw new AmbariLdapException("Key Store Path must be specified");
    }

    try {

      KeyStore ks = KeyStore.getInstance(ambariLdapConfiguration.trustStoreType());
      FileInputStream fis = new FileInputStream(ambariLdapConfiguration.trustStorePath());
      ks.load(fis, ambariLdapConfiguration.trustStorePassword().toCharArray());
      return ks;

    } catch (Exception e) {

      LOG.error("Failed to create keystore", e);
      throw new AmbariLdapException(e);

    }
  }
}
