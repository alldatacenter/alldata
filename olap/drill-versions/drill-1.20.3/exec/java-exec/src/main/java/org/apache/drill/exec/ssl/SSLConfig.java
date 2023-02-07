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
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.text.MessageFormat;

public abstract class SSLConfig {

  private static final Logger logger = LoggerFactory.getLogger(SSLConfig.class);

  public static final String DEFAULT_SSL_PROVIDER = "JDK"; // JDK or OPENSSL
  public static final String DEFAULT_SSL_PROTOCOL = "TLSv1.2";
  public static final int DEFAULT_SSL_HANDSHAKE_TIMEOUT_MS = 10 * 1000; // 10 seconds

  // Either the Netty SSL context or the JDK SSL context will be initialized
  // The JDK SSL context is use iff the useSystemTrustStore setting is enabled.
  protected SslContext nettySslContext;
  protected SSLContext jdkSSlContext;

  private static final boolean isWindows = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
  private static final boolean isMacOs = System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;

  public static final String HADOOP_SSL_CONF_TPL_KEY = "hadoop.ssl.{0}.conf";
  public static final String HADOOP_SSL_KEYSTORE_LOCATION_TPL_KEY = "ssl.{0}.keystore.location";
  public static final String HADOOP_SSL_KEYSTORE_PASSWORD_TPL_KEY = "ssl.{0}.keystore.password";
  public static final String HADOOP_SSL_KEYSTORE_TYPE_TPL_KEY = "ssl.{0}.keystore.type";
  public static final String HADOOP_SSL_KEYSTORE_KEYPASSWORD_TPL_KEY =
      "ssl.{0}.keystore.keypassword";
  public static final String HADOOP_SSL_TRUSTSTORE_LOCATION_TPL_KEY = "ssl.{0}.truststore.location";
  public static final String HADOOP_SSL_TRUSTSTORE_PASSWORD_TPL_KEY = "ssl.{0}.truststore.password";
  public static final String HADOOP_SSL_TRUSTSTORE_TYPE_TPL_KEY = "ssl.{0}.truststore.type";

  // copy of Hadoop's SSLFactory.Mode. Done so that we do not
  // need to include hadoop-common as a dependency in
  // jdbc-all-jar.
  public enum Mode { CLIENT, SERVER }

  public SSLConfig() {
  }

  public abstract void validateKeyStore() throws DrillException;

  // We need to use different SSLContext objects depending on what the user has chosen
  // For most uses we will use the Netty SslContext class. This allows us to use either
  // the JDK implementation or the OpenSSL implementation. However if the user wants to
  // use the system trust store, then the only way to access it is via the JDK's
  // SSLContext class. (See the createSSLEngine method below).

  public abstract SslContext initNettySslContext() throws DrillException;

  public abstract SSLContext initJDKSSLContext() throws DrillException;

  public abstract boolean isUserSslEnabled();

  public abstract boolean isHttpsEnabled();

  public abstract String getKeyStoreType();

  public abstract String getKeyStorePath();

  public abstract String getKeyStorePassword();

  public abstract String getKeyPassword();

  public abstract String getTrustStoreType();

  public abstract boolean hasTrustStorePath();

  public abstract String getTrustStorePath();

  public abstract boolean hasTrustStorePassword();

  public abstract String getTrustStorePassword();

  public abstract String getProtocol();

  public abstract SslProvider getProvider();

  public abstract int getHandshakeTimeout();

  public abstract Mode getMode();

  public abstract boolean disableHostVerification();

  public abstract boolean disableCertificateVerification();

  public abstract boolean useSystemTrustStore();

  public abstract boolean isSslValid();

  public SslContext getNettySslContext() {
    return nettySslContext;
  }

  public TrustManagerFactory initializeTrustManagerFactory() throws DrillException {
    TrustManagerFactory tmf;
    KeyStore ts = null;
    //Support Windows/MacOs system trust store
    try {
      String trustStoreType = getTrustStoreType();
      if ((isWindows || isMacOs) && useSystemTrustStore()) {
        // This is valid for MS-Windows and MacOs
        logger.debug("Initializing System truststore.");
        ts = KeyStore.getInstance(!trustStoreType.isEmpty() ? trustStoreType : KeyStore.getDefaultType());
        ts.load(null, null);
      } else if (!getTrustStorePath().isEmpty()) {
          // if truststore is not provided then we will use the default. Note that the default depends on
          // the TrustManagerFactory that in turn depends on the Security Provider.
          // Use null as the truststore which will result in the default truststore being picked up
          logger.debug("Initializing truststore {}.", getTrustStorePath());
          ts = KeyStore.getInstance(!trustStoreType.isEmpty() ? trustStoreType : KeyStore.getDefaultType());
          InputStream tsStream = new FileInputStream(getTrustStorePath());
          ts.load(tsStream, getTrustStorePassword().toCharArray());
      } else {
        logger.debug("Initializing default truststore.");
      }
      if (disableCertificateVerification()) {
        tmf = InsecureTrustManagerFactory.INSTANCE;
      } else {
        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      }
      tmf.init(ts);
    } catch (Exception e) {
      // Catch any SSL initialization Exceptions here and abort.
      throw new DrillException(
          new StringBuilder()
              .append("Exception while initializing the truststore: [")
              .append(e.getMessage())
              .append("]. ")
              .toString(), e);
    }
    return tmf;
  }

  public KeyManagerFactory initializeKeyManagerFactory() throws DrillException {
    KeyManagerFactory kmf;
    String keyStorePath = getKeyStorePath();
    String keyStorePassword = getKeyStorePassword();
    String keyStoreType = getKeyStoreType();
    try {
      if (keyStorePath.isEmpty()) {
        throw new DrillException("No Keystore provided.");
      }
      KeyStore ks =
          KeyStore.getInstance(!keyStoreType.isEmpty() ? keyStoreType : KeyStore.getDefaultType());
      //initialize the key manager factory
      // Will throw an exception if the file is not found/accessible.
      InputStream ksStream = new FileInputStream(keyStorePath);
      // A key password CANNOT be null or an empty string.
      if (keyStorePassword.isEmpty()) {
        throw new DrillException("The Keystore password cannot be empty.");
      }
      ks.load(ksStream, keyStorePassword.toCharArray());
      // Empty Keystore. (Remarkably, it is possible to do this).
      if (ks.size() == 0) {
        throw new DrillException("The Keystore has no entries.");
      }
      kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(ks, getKeyPassword().toCharArray());

    } catch (Exception e) {
      throw new DrillException(
          new StringBuilder()
              .append("Exception while initializing the keystore: [")
              .append(e.getMessage())
              .append("]. ")
              .toString());
    }
    return kmf;
  }

  public void initContext() throws DrillException {
    if ((isWindows || isMacOs) && useSystemTrustStore()) {
      initJDKSSLContext();
      logger.debug("Initialized Windows/MacOs SSL context using JDK.");
    } else {
      initNettySslContext();
      logger.debug("Initialized SSL context.");
    }
    return;
  }

  public SSLEngine createSSLEngine(BufferAllocator allocator, String peerHost, int peerPort) {
    SSLEngine engine;
    if ((isWindows || isMacOs) && useSystemTrustStore()) {
      if (peerHost != null) {
        engine = jdkSSlContext.createSSLEngine(peerHost, peerPort);
        logger.debug("Initializing Windows/MacOs SSLEngine with hostname.");
      } else {
        engine = jdkSSlContext.createSSLEngine();
        logger.debug("Initializing Windows/MacOs SSLEngine with no hostname.");
      }
    } else {
      if (peerHost != null) {
        engine = nettySslContext.newEngine(allocator.getAsByteBufAllocator(), peerHost, peerPort);
        logger.debug("Initializing SSLEngine with hostname.");
      } else {
        engine = nettySslContext.newEngine(allocator.getAsByteBufAllocator());
        logger.debug("Initializing SSLEngine with no hostname.");
      }
    }
    return engine;
  }

  abstract Configuration getHadoopConfig();

  String getPassword(String hadoopName) {
    String value = null;
    if (getHadoopConfig() != null) {
      try {
        char[] password = getHadoopConfig().getPassword(hadoopName);
        if (password != null) {
          value = String.valueOf(password);
        }
      } catch (IOException e) {
        logger.warn("Unable to obtain password {} from CredentialProvider API: {}", hadoopName, e.getMessage());
        // fallthrough
      }
    }
    return value;
  }

  String resolveHadoopPropertyName(String nameTemplate, Mode mode) {
    return MessageFormat.format(nameTemplate, mode.toString().toLowerCase());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SSL is ")
        .append(isUserSslEnabled()?"":" not ")
        .append("enabled.\n");
    sb.append("HTTPS is ")
        .append(isHttpsEnabled()?"":" not ")
        .append("enabled.\n");
    if(isUserSslEnabled() || isHttpsEnabled()) {
      sb.append("SSL Configuration :")
          .append("OS:").append(System.getProperty("os.name"))
          .append("\n\tUsing system trust store: ").append(useSystemTrustStore())
          .append("\n\tprotocol: ").append(getProtocol())
          .append("\n\tkeyStoreType: ").append(getKeyStoreType())
          .append("\n\tkeyStorePath: ").append(getKeyStorePath())
          .append("\n\tkeyStorePassword: ").append(getPrintablePassword(getKeyStorePassword()))
          .append("\n\tkeyPassword: ").append(getPrintablePassword(getKeyPassword()))
          .append("\n\ttrustStoreType: ").append(getTrustStoreType())
          .append("\n\ttrustStorePath: ").append(getTrustStorePath())
          .append("\n\ttrustStorePassword: ").append(getPrintablePassword(getTrustStorePassword()))
          .append("\n\thandshakeTimeout: ").append(getHandshakeTimeout())
          .append("\n\tdisableHostVerification: ").append(disableHostVerification())
          .append("\n\tdisableCertificateVerification: ").append(disableCertificateVerification());
    }
    return sb.toString();
  }

  private String getPrintablePassword(String password) {
    StringBuilder sb = new StringBuilder();
    if(password == null || password.length()<2 ){
      return password;
    }
    sb.append(password.charAt(0)).append("****").append(password.charAt(password.length()-1));
    return sb.toString();
  }
}
