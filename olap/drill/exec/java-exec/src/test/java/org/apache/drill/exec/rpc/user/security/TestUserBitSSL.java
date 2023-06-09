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
package org.apache.drill.exec.rpc.user.security;

import com.typesafe.config.ConfigValueFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import junit.framework.TestCase;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.BaseTestQuery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.Properties;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class TestUserBitSSL extends BaseTestQuery {
  private static DrillConfig newConfig;
  private static Properties initProps; // initial client properties
  private static ClassLoader classLoader;
  private static String ksPath;
  private static String tsPath;
  private static String emptyTSPath;
  private static String unknownKsPath;

  @BeforeClass
  public static void setupTest() throws Exception {

    // Create a new DrillConfig
    classLoader = TestUserBitSSL.class.getClassLoader();
    ksPath = new File(classLoader.getResource("ssl/keystore.ks").getFile()).getAbsolutePath();
    unknownKsPath = new File(classLoader.getResource("ssl/unknownkeystore.ks").getFile()).getAbsolutePath();
    tsPath = new File(classLoader.getResource("ssl/truststore.ks").getFile()).getAbsolutePath();
    emptyTSPath = new File(classLoader.getResource("ssl/emptytruststore.ks").getFile()).getAbsolutePath();
    newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.SSL_USE_HADOOP_CONF,
            ConfigValueFactory.fromAnyRef(false))
        .withValue(ExecConstants.USER_SSL_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.SSL_KEYSTORE_TYPE,
            ConfigValueFactory.fromAnyRef("JKS"))
        .withValue(ExecConstants.SSL_KEYSTORE_PATH,
            ConfigValueFactory.fromAnyRef(ksPath))
        .withValue(ExecConstants.SSL_KEYSTORE_PASSWORD,
            ConfigValueFactory.fromAnyRef("drill123"))
        .withValue(ExecConstants.SSL_KEY_PASSWORD,
            ConfigValueFactory.fromAnyRef("drill123"))
        .withValue(ExecConstants.SSL_TRUSTSTORE_TYPE,
            ConfigValueFactory.fromAnyRef("JKS"))
        .withValue(ExecConstants.SSL_TRUSTSTORE_PATH,
            ConfigValueFactory.fromAnyRef(tsPath))
        .withValue(ExecConstants.SSL_TRUSTSTORE_PASSWORD,
            ConfigValueFactory.fromAnyRef("drill123"))
        .withValue(ExecConstants.SSL_PROTOCOL,
            ConfigValueFactory.fromAnyRef("TLSv1.2")));

    initProps = new Properties();
    initProps.setProperty(DrillProperties.ENABLE_TLS, "true");
    initProps.setProperty(DrillProperties.TRUSTSTORE_PATH, tsPath);
    initProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "drill123");
    initProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "true");

    // Start an SSL enabled cluster
    updateTestCluster(1, newConfig, initProps);
  }

  @AfterClass
  public static void cleanTest() throws Exception {
    DrillConfig restoreConfig =
        new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties()));
    updateTestCluster(1, restoreConfig);
  }

  @Test
  public void testSSLConnection() throws Exception {
    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.ENABLE_TLS, "true");
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PATH, tsPath);
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "drill123");
    connectionProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "true");
    try {
      updateClient(connectionProps);
    } catch (Exception e) {
      TestCase.fail( new StringBuilder()
          .append("SSL Connection failed with exception [" )
          .append( e.getMessage() )
          .append("]")
          .toString());
    }
  }

  @Test
  public void testSSLConnectionWithKeystore() throws Exception {
    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.ENABLE_TLS, "true");
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PATH, ksPath);
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "drill123");
    connectionProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "true");
    try {
      updateClient(connectionProps);
    } catch (Exception e) {
      TestCase.fail( new StringBuilder()
          .append("SSL Connection failed with exception [" )
          .append( e.getMessage() )
          .append("]")
          .toString());
    }
  }

  @Test
  public void testSSLConnectionFailBadTrustStore() throws Exception {
    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.ENABLE_TLS, "true");
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PATH, ""); // NO truststore
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "drill123");
    connectionProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "true");
    boolean failureCaught = false;
    try {
      updateClient(connectionProps);
    } catch (Exception e) {
      failureCaught = true;
    }
    assertEquals(failureCaught, true);
  }

  @Test
  public void testSSLConnectionFailBadPassword() throws Exception {
    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.ENABLE_TLS, "true");
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PATH, tsPath);
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "bad_password");
    connectionProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "true");
    boolean failureCaught = false;
    try {
      updateClient(connectionProps);
    } catch (Exception e) {
      failureCaught = true;
    }
    assertEquals(failureCaught, true);
  }

  @Test
  public void testSSLConnectionFailEmptyTrustStore() throws Exception {
    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.ENABLE_TLS, "true");
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PATH, emptyTSPath);
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "drill123");
    connectionProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "true");
    boolean failureCaught = false;
    try {
      updateClient(connectionProps);
    } catch (Exception e) {
      failureCaught = true;
    }
    assertEquals(failureCaught, true);
  }

  @Test
  public void testSSLQuery() throws Exception {
    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.ENABLE_TLS, "true");
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PATH, tsPath);
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "drill123");
    connectionProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "true");
    try {
      updateClient(connectionProps);
    } catch (Exception e) {
      TestCase.fail( new StringBuilder()
          .append("SSL Connection failed with exception [" )
          .append( e.getMessage() )
          .append("]")
          .toString());
    }
    test("SELECT * FROM cp.`region.json`");
  }

  @Ignore("This test fails in some cases where the host name may be set up inconsistently.")
  @Test
  public void testClientConfigHostnameVerification() {
    String password = "test_password";
    String trustStoreFileName = "drillTestTrustStore";
    String keyStoreFileName = "drillTestKeyStore";
    KeyStore ts, ks;
    File tempFile1, tempFile2;
    String trustStorePath;
    String keyStorePath;

    try {
      String fqdn = InetAddress.getLocalHost().getHostName();
      SelfSignedCertificate certificate = new SelfSignedCertificate(fqdn);

      tempFile1 = File.createTempFile(trustStoreFileName, ".ks");
      tempFile1.deleteOnExit();
      trustStorePath = tempFile1.getAbsolutePath();
      //generate a truststore.
      ts = KeyStore.getInstance(KeyStore.getDefaultType());
      ts.load(null, password.toCharArray());
      ts.setCertificateEntry("drillTest", certificate.cert());
      // Store away the truststore.
      try (FileOutputStream fos1 = new FileOutputStream(tempFile1);) {
        ts.store(fos1, password.toCharArray());
      } catch (Exception e) {
        fail(e.getMessage());
      }

      tempFile2 = File.createTempFile(keyStoreFileName, ".ks");
      tempFile2.deleteOnExit();
      keyStorePath = tempFile2.getAbsolutePath();
      //generate a keystore.
      ts = KeyStore.getInstance(KeyStore.getDefaultType());
      ts.load(null, password.toCharArray());
      ts.setKeyEntry("drillTest", certificate.key(), password.toCharArray(), new java.security.cert.Certificate[]{certificate.cert()});
      // Store away the keystore.
      try (FileOutputStream fos2 = new FileOutputStream(tempFile2);) {
        ts.store(fos2, password.toCharArray());
      } catch (Exception e) {
        fail(e.getMessage());
      }

      final Properties connectionProps = new Properties();
      connectionProps.setProperty(DrillProperties.ENABLE_TLS, "true");
      connectionProps.setProperty(DrillProperties.TRUSTSTORE_PATH, trustStorePath);
      connectionProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, password);
      connectionProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "false");

      DrillConfig sslConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
          .withValue(ExecConstants.USER_SSL_ENABLED, ConfigValueFactory.fromAnyRef(true))
          .withValue(ExecConstants.SSL_KEYSTORE_TYPE, ConfigValueFactory.fromAnyRef("JKS"))
          .withValue(ExecConstants.SSL_KEYSTORE_PATH, ConfigValueFactory.fromAnyRef(keyStorePath))
          .withValue(ExecConstants.SSL_KEYSTORE_PASSWORD, ConfigValueFactory.fromAnyRef("test_password"))
          .withValue(ExecConstants.SSL_PROTOCOL, ConfigValueFactory.fromAnyRef("TLSv1.2")));

      updateTestCluster(1, sslConfig, connectionProps);

    } catch (Exception e) {
      fail(e.getMessage());
    }
    //reset cluster
    updateTestCluster(1, newConfig, initProps);

  }

  @Test
  public void testClientConfigHostNameVerificationFail() throws Exception {
    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.ENABLE_TLS, "true");
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PATH, tsPath);
    connectionProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "password");
    connectionProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "false");
    boolean failureCaught = false;
    try {
      updateClient(connectionProps);
    } catch (Exception e) {
      failureCaught = true;
    }
    assertEquals(failureCaught, true);
  }

  @Test
  public void testClientConfigCertificateVerification() {
    // Fail if certificate is not valid
    boolean failureCaught = false;
    try {
      final Properties connectionProps = new Properties();
      connectionProps.setProperty(DrillProperties.ENABLE_TLS, "true");
      connectionProps.setProperty(DrillProperties.TRUSTSTORE_PATH, tsPath);
      connectionProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "drill123");
      connectionProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "true");
      //connectionProps.setProperty(DrillProperties.DISABLE_CERT_VERIFICATION, "true");

      DrillConfig sslConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
          .withValue(ExecConstants.USER_SSL_ENABLED, ConfigValueFactory.fromAnyRef(true))
          .withValue(ExecConstants.SSL_KEYSTORE_TYPE, ConfigValueFactory.fromAnyRef("JKS"))
          .withValue(ExecConstants.SSL_KEYSTORE_PATH, ConfigValueFactory.fromAnyRef(unknownKsPath))
          .withValue(ExecConstants.SSL_KEYSTORE_PASSWORD, ConfigValueFactory.fromAnyRef("drill123"))
          .withValue(ExecConstants.SSL_PROTOCOL, ConfigValueFactory.fromAnyRef("TLSv1.2")));

      updateTestCluster(1, sslConfig, connectionProps);

    } catch (Exception e) {
      failureCaught = true;
    }
    //reset cluster
    updateTestCluster(1, newConfig, initProps);
    assertEquals(failureCaught, true);
  }

  @Test
  public void testClientConfigNoCertificateVerification() {
    // Pass if certificate is not valid, but mode is insecure.
    try {
      final Properties connectionProps = new Properties();
      connectionProps.setProperty(DrillProperties.ENABLE_TLS, "true");
      connectionProps.setProperty(DrillProperties.TRUSTSTORE_PATH, tsPath);
      connectionProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "drill123");
      connectionProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "true");
      connectionProps.setProperty(DrillProperties.DISABLE_CERT_VERIFICATION, "true");

      DrillConfig sslConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
          .withValue(ExecConstants.USER_SSL_ENABLED, ConfigValueFactory.fromAnyRef(true))
          .withValue(ExecConstants.SSL_KEYSTORE_TYPE, ConfigValueFactory.fromAnyRef("JKS"))
          .withValue(ExecConstants.SSL_KEYSTORE_PATH, ConfigValueFactory.fromAnyRef(unknownKsPath))
          .withValue(ExecConstants.SSL_KEYSTORE_PASSWORD, ConfigValueFactory.fromAnyRef("drill123"))
          .withValue(ExecConstants.SSL_PROTOCOL, ConfigValueFactory.fromAnyRef("TLSv1.2")));

      updateTestCluster(1, sslConfig, connectionProps);

    } catch (Exception e) {
      fail(e.getMessage());
    }
    //reset cluster
    updateTestCluster(1, newConfig, initProps);

  }

}
