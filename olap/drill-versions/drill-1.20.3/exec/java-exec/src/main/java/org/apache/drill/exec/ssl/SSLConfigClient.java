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

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.util.Properties;

public class SSLConfigClient extends SSLConfig {

  private static final Logger logger = LoggerFactory.getLogger(SSLConfigClient.class);

  private final Properties properties;
  private final Configuration hadoopConfig;
  private final boolean userSslEnabled;
  private final String trustStoreType;
  private final String trustStorePath;
  private final String trustStorePassword;
  private final boolean disableHostVerification;
  private final boolean disableCertificateVerification;
  private final boolean useSystemTrustStore;
  private final String protocol;
  private final int handshakeTimeout;
  private final String provider;

  private final String emptyString = "";

  public SSLConfigClient(Properties properties, Configuration hadoopConfig) throws DrillException {
    this.properties = properties;
    this.hadoopConfig = hadoopConfig;
    userSslEnabled = getBooleanProperty(DrillProperties.ENABLE_TLS);
    SSLCredentialsProvider credentialsProvider = SSLCredentialsProvider.getSSLCredentialsProvider(
        this::getStringProperty,
        this::getPasswordStringProperty,
        getMode(),
        getBooleanProperty(DrillProperties.USE_MAPR_SSL_CONFIG)
    );
    trustStoreType = credentialsProvider.getTrustStoreType(DrillProperties.TRUSTSTORE_TYPE, "JKS");
    trustStorePath = credentialsProvider.getTrustStoreLocation(DrillProperties.TRUSTSTORE_PATH, "");
    trustStorePassword = credentialsProvider.getTrustStorePassword(DrillProperties.TRUSTSTORE_PASSWORD,
        resolveHadoopPropertyName(HADOOP_SSL_TRUSTSTORE_PASSWORD_TPL_KEY, getMode()));
    disableHostVerification = getBooleanProperty(DrillProperties.DISABLE_HOST_VERIFICATION);
    disableCertificateVerification = getBooleanProperty(DrillProperties.DISABLE_CERT_VERIFICATION);
    useSystemTrustStore = getBooleanProperty(DrillProperties.USE_SYSTEM_TRUSTSTORE);
    protocol = getStringProperty(DrillProperties.TLS_PROTOCOL, DEFAULT_SSL_PROTOCOL);
    int hsTimeout = getIntProperty(DrillProperties.TLS_HANDSHAKE_TIMEOUT, DEFAULT_SSL_HANDSHAKE_TIMEOUT_MS);
    if (hsTimeout <= 0) {
      hsTimeout = DEFAULT_SSL_HANDSHAKE_TIMEOUT_MS;
    }
    handshakeTimeout = hsTimeout;
    // If provider is OPENSSL then to debug or run this code in an IDE, you will need to enable
    // the dependency on netty-tcnative with the correct classifier for the platform you use.
    // This can be done by enabling the openssl profile.
    // If the IDE is Eclipse, it requires you to install an additional Eclipse plugin available here:
    // http://repo1.maven.org/maven2/kr/motd/maven/os-maven-plugin/1.6.1/os-maven-plugin-1.6.1.jar
    // or from your local maven repository:
    // ~/.m2/repository/kr/motd/maven/os-maven-plugin/1.6.1/os-maven-plugin-1.6.1.jar
    // Note that installing this plugin may require you to start with a new workspace
    provider = getStringProperty(DrillProperties.TLS_PROVIDER, DEFAULT_SSL_PROVIDER);
  }

  private boolean getBooleanProperty(String propName) {
    return (properties != null) && (properties.containsKey(propName))
        && (properties.getProperty(propName).compareToIgnoreCase("true") == 0);
  }

  private String getStringProperty(String name, String defaultValue) {
    String value = "";
    if ( (properties != null) && (properties.containsKey(name))) {
      value = properties.getProperty(name);
    }
    if (value.isEmpty()) {
      value = defaultValue;
    }
    value = value.trim();
    return value;
  }

  private String getPasswordStringProperty(String name, String hadoopName) {
    String value = getPassword(hadoopName);

    if (value == null) {
      value = getStringProperty(name, "");
    }
    return value;
  }

  private int getIntProperty(String name, int defaultValue) {
    int value = defaultValue;
    if (properties != null) {
      String property = properties.getProperty(name);
      if (property != null && property.length() > 0) {
        value = Integer.decode(property);
      }
    }
    return value;
  }

  @Override
  public void validateKeyStore() throws DrillException {
  }

  @Override
  public SslContext initNettySslContext() throws DrillException {
    final SslContext sslCtx;

    if (!userSslEnabled) {
      return null;
    }

    TrustManagerFactory tmf;
    try {
      tmf = initializeTrustManagerFactory();
      sslCtx = SslContextBuilder.forClient()
          .sslProvider(getProvider())
          .trustManager(tmf)
          .protocols(protocol)
          .build();
    } catch (Exception e) {
      // Catch any SSL initialization Exceptions here and abort.
      throw new DrillException(new StringBuilder()
          .append("SSL is enabled but cannot be initialized due to the following exception: ")
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

    TrustManagerFactory tmf;
    try {
      tmf = initializeTrustManagerFactory();
      sslCtx = SSLContext.getInstance(protocol);
      sslCtx.init(null, tmf.getTrustManagers(), null);
    } catch (Exception e) {
      // Catch any SSL initialization Exceptions here and abort.
      throw new DrillException(new StringBuilder()
          .append("SSL is enabled but cannot be initialized due to the following exception: ")
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

    if (!this.disableHostVerification()) {
      SSLParameters sslParameters = engine.getSSLParameters();
      // only available since Java 7
      sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
      engine.setSSLParameters(sslParameters);
    }

    engine.setUseClientMode(true);

    try {
      engine.setEnableSessionCreation(true);
    } catch (Exception e) {
      // Openssl implementation may throw this.
      logger.debug("Session creation not enabled. Exception: {}", e.getMessage());
    }

    return engine;
  }

  @Override
  public boolean isUserSslEnabled() {
    return userSslEnabled;
  }

  @Override
  public boolean isHttpsEnabled() {
    return false;
  }

  @Override
  public String getKeyStoreType() {
    return emptyString;
  }

  @Override
  public String getKeyStorePath() {
    return emptyString;
  }

  @Override
  public String getKeyStorePassword() {
    return emptyString;
  }

  @Override
  public String getKeyPassword() {
    return emptyString;
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
    return handshakeTimeout;
  }

  @Override
  public Mode getMode() {
    return Mode.CLIENT;
  }

  @Override
  public boolean disableHostVerification() {
    return disableHostVerification;
  }

  @Override
  public boolean disableCertificateVerification() {
    return disableCertificateVerification;
  }

  @Override
  public boolean useSystemTrustStore() {
    return useSystemTrustStore;
  }

  @Override
  public boolean isSslValid() {
    return true;
  }

  @Override
  Configuration getHadoopConfig() {
    return hadoopConfig;
  }
}
