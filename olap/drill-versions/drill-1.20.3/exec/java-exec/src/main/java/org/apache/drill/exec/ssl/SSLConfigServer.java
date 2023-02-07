/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.ssl;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

public class SSLConfigServer extends SSLConfig {

  private static final Logger logger = LoggerFactory.getLogger(SSLConfigServer.class);

  private final DrillConfig config;
  private final Configuration hadoopConfig;
  private final boolean userSslEnabled;
  private final boolean httpsEnabled;
  private final String keyStoreType;
  private final String keyStorePath;
  private final String keyStorePassword;
  private final String keyPassword;
  private final String trustStoreType;
  private final String trustStorePath;
  private final String trustStorePassword;
  private final String protocol;
  private final String provider;

  public SSLConfigServer(DrillConfig config, Configuration hadoopConfig) throws DrillException {
    this.config = config;
    Mode mode = Mode.SERVER;
    httpsEnabled =
        config.hasPath(ExecConstants.HTTP_ENABLE_SSL) && config.getBoolean(ExecConstants.HTTP_ENABLE_SSL);
    // For testing we will mock up a hadoop configuration, however for regular use, we find the actual hadoop config.
    boolean enableHadoopConfig = config.getBoolean(ExecConstants.SSL_USE_HADOOP_CONF);
    if (enableHadoopConfig) {
      if (hadoopConfig == null) {
        this.hadoopConfig = new Configuration(); // get hadoop configuration
      } else {
        this.hadoopConfig = hadoopConfig;
      }
      String hadoopSSLConfigFile =
          this.hadoopConfig.get(resolveHadoopPropertyName(HADOOP_SSL_CONF_TPL_KEY, getMode()));
      logger.debug("Using Hadoop configuration for SSL");
      logger.debug("Hadoop SSL configuration file: {}", hadoopSSLConfigFile);
      this.hadoopConfig.addResource(hadoopSSLConfigFile);
    } else {
      this.hadoopConfig = null;
    }
    userSslEnabled =
        config.hasPath(ExecConstants.USER_SSL_ENABLED) && config.getBoolean(ExecConstants.USER_SSL_ENABLED);
    SSLCredentialsProvider credentialsProvider = SSLCredentialsProvider.getSSLCredentialsProvider(
        this::getConfigParam,
        this::getPasswordConfigParam,
        Mode.SERVER,
        config.getBoolean(ExecConstants.SSL_USE_MAPR_CONFIG));
    trustStoreType = credentialsProvider.getTrustStoreType(
        ExecConstants.SSL_TRUSTSTORE_TYPE, resolveHadoopPropertyName(HADOOP_SSL_TRUSTSTORE_TYPE_TPL_KEY, mode));
    trustStorePath = credentialsProvider.getTrustStoreLocation(
        ExecConstants.SSL_TRUSTSTORE_PATH, resolveHadoopPropertyName(HADOOP_SSL_TRUSTSTORE_LOCATION_TPL_KEY, mode));
    trustStorePassword = credentialsProvider.getTrustStorePassword(
        ExecConstants.SSL_TRUSTSTORE_PASSWORD, resolveHadoopPropertyName(HADOOP_SSL_TRUSTSTORE_PASSWORD_TPL_KEY, mode));
    keyStoreType = credentialsProvider.getKeyStoreType(
        ExecConstants.SSL_KEYSTORE_TYPE, resolveHadoopPropertyName(HADOOP_SSL_KEYSTORE_TYPE_TPL_KEY, mode));
    keyStorePath = credentialsProvider.getKeyStoreLocation(
        ExecConstants.SSL_KEYSTORE_PATH, resolveHadoopPropertyName(HADOOP_SSL_KEYSTORE_LOCATION_TPL_KEY, mode));
    keyStorePassword = credentialsProvider.getKeyStorePassword(
        ExecConstants.SSL_KEYSTORE_PASSWORD, resolveHadoopPropertyName(HADOOP_SSL_KEYSTORE_PASSWORD_TPL_KEY, mode));
    String keyPass = credentialsProvider.getKeyPassword(
        ExecConstants.SSL_KEY_PASSWORD, resolveHadoopPropertyName(HADOOP_SSL_KEYSTORE_KEYPASSWORD_TPL_KEY, mode));
    // if no keypassword specified, use keystore password
    keyPassword = keyPass.isEmpty() ? keyStorePassword : keyPass;
    protocol = config.getString(ExecConstants.SSL_PROTOCOL);
    // If provider is OPENSSL then to debug or run this code in an IDE, you will need to enable
    // the dependency on netty-tcnative with the correct classifier for the platform you use.
    // This can be done by enabling the openssl profile.
    // If the IDE is Eclipse, it requires you to install an additional Eclipse plugin available here:
    // http://repo1.maven.org/maven2/kr/motd/maven/os-maven-plugin/1.6.1/os-maven-plugin-1.6.1.jar
    // or from your local maven repository:
    // ~/.m2/repository/kr/motd/maven/os-maven-plugin/1.6.1/os-maven-plugin-1.6.1.jar
    // Note that installing this plugin may require you to start with a new workspace
    provider = config.getString(ExecConstants.SSL_PROVIDER);
  }

  @Override
  public void validateKeyStore() throws DrillException {
    //HTTPS validates the keystore is not empty. User Server SSL context initialization also validates keystore, but
    // much more strictly. User Client context initialization does not validate keystore.
    /*If keystorePath or keystorePassword is provided in the configuration file use that*/
    if ((isUserSslEnabled() || isHttpsEnabled())) {
      if (!keyStorePath.isEmpty() || !keyStorePassword.isEmpty()) {
        if (keyStorePath.isEmpty()) {
          throw new DrillException(
              " *.ssl.keyStorePath in the configuration file is empty, but *.ssl.keyStorePassword is set");
        } else if (keyStorePassword.isEmpty()) {
          throw new DrillException(
              " *.ssl.keyStorePassword in the configuration file is empty, but *.ssl.keyStorePath is set ");
        }
      }
    }
  }

  @Override
  public SslContext initNettySslContext() throws DrillException {
    final SslContext sslCtx;

    if (!userSslEnabled) {
      return null;
    }

    KeyManagerFactory kmf;
    TrustManagerFactory tmf;
    try {
      if (keyStorePath.isEmpty()) {
        throw new DrillException("No Keystore provided.");
      }
      kmf = initializeKeyManagerFactory();
      tmf = initializeTrustManagerFactory();
      sslCtx = SslContextBuilder.forServer(kmf)
          .trustManager(tmf)
          .protocols(protocol)
          .sslProvider(getProvider())
          .build(); // Will throw an exception if the key password is not correct
    } catch (Exception e) {
      // Catch any SSL initialization Exceptions here and abort.
      throw new DrillException(new StringBuilder()
          .append("SSL is enabled but cannot be initialized - ")
          .append("[ ")
          .append(e.getMessage())
          .append("]. ")
          .toString());
    }
    this.nettySslContext = sslCtx;
    return sslCtx;
  }

  @Override
  public SSLContext initJDKSSLContext() throws DrillException {
    final SSLContext sslCtx;

    if (!userSslEnabled) {
      return null;
    }

    KeyManagerFactory kmf;
    TrustManagerFactory tmf;
    try {
      if (keyStorePath.isEmpty()) {
        throw new DrillException("No Keystore provided.");
      }
      kmf = initializeKeyManagerFactory();
      tmf = initializeTrustManagerFactory();
      sslCtx = SSLContext.getInstance(protocol);
      sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    } catch (Exception e) {
      // Catch any SSL initialization Exceptions here and abort.
      throw new DrillException(
          new StringBuilder().append("SSL is enabled but cannot be initialized - ")
              .append("[ ")
              .append(e.getMessage())
              .append("]. ")
              .toString());
    }
    this.jdkSSlContext = sslCtx;
    return sslCtx;
  }

  @Override
  public SSLEngine createSSLEngine(BufferAllocator allocator, String peerHost, int peerPort) {
    SSLEngine engine = super.createSSLEngine(allocator, peerHost, peerPort);

    engine.setUseClientMode(false);

    // No need for client side authentication (HTTPS like behaviour)
    engine.setNeedClientAuth(false);

    try {
      engine.setEnableSessionCreation(true);
    } catch (Exception e) {
      // Openssl implementation may throw this.
      logger.debug("Session creation not enabled. Exception: {}", e.getMessage());
    }

    return engine;
  }

  private String getConfigParam(String name, String hadoopName) {
    String value = "";
    if (hadoopConfig != null) {
      value = getHadoopConfigParam(hadoopName);
    }
    if (value.isEmpty() && config.hasPath(name)) {
      value = config.getString(name);
    }
    value = value.trim();
    return value;
  }

  private String getHadoopConfigParam(String name) {
    Preconditions.checkArgument(this.hadoopConfig != null);
    String value = hadoopConfig.get(name, "");
    value = value.trim();
    return value;
  }

  private String getPasswordConfigParam(String name, String hadoopName) {
    String value = getPassword(hadoopName);

    if (value == null) {
      value = getConfigParam(name, hadoopName);
    }

    return value;
  }

  @Override
  public boolean isUserSslEnabled() {
    return userSslEnabled;
  }

  @Override
  public boolean isHttpsEnabled() {
    return httpsEnabled;
  }

  @Override
  public String getKeyStoreType() {
    return keyStoreType;
  }

  @Override
  public String getKeyStorePath() {
    return keyStorePath;
  }

  @Override
  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  @Override
  public String getKeyPassword() {
    return keyPassword;
  }

  @Override
  public String getTrustStoreType() {
    return trustStoreType;
  }

  @Override
  public boolean hasTrustStorePath() {
    return !trustStorePath.isEmpty();
  }

  @Override
  public String getTrustStorePath() {
    return trustStorePath;
  }

  @Override
  public boolean hasTrustStorePassword() {
    return !trustStorePassword.isEmpty();
  }

  @Override
  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  @Override
  public String getProtocol() {
    return protocol;
  }

  @Override
  public SslProvider getProvider() {
    return provider.equalsIgnoreCase("JDK") ? SslProvider.JDK : SslProvider.OPENSSL;
  }

  @Override
  public int getHandshakeTimeout() {
    // A value of 0 is interpreted by Netty as "no timeout". This is hard coded
    // here instead being read from {@link ExecConstants.SSL_HANDSHAKE_TIMEOUT}
    // because the SSL handshake timeout is managed from the client end only
    // (see {@link SSLConfigClient}).
    return 0;
  }

  @Override
  public Mode getMode() {
    return Mode.SERVER;
  }

  @Override
  public boolean disableHostVerification() {
    return false;
  }

  @Override
  public boolean disableCertificateVerification() {
    return false;
  }

  @Override
  public boolean useSystemTrustStore() {
    return false; // Client only, notsupported by the server
  }

  @Override
  public boolean isSslValid() {
    return !keyStorePath.isEmpty() && !keyStorePassword.isEmpty();
  }

  @Override
  Configuration getHadoopConfig() {
    return hadoopConfig;
  }
}
