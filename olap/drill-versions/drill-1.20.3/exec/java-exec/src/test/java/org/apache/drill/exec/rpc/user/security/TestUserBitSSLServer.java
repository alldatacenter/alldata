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
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.ExecConstants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@Category({SecurityTest.class})
public class TestUserBitSSLServer extends BaseTestQuery {
  private static DrillConfig sslConfig;
  private static Properties initProps; // initial client properties
  private static ClassLoader classLoader;
  private static String ksPath;
  private static String tsPath;

  @BeforeClass
  public static void setupTest() throws Exception {
    classLoader = TestUserBitSSLServer.class.getClassLoader();
    ksPath = new File(classLoader.getResource("ssl/keystore.ks").getFile()).getAbsolutePath();
    tsPath = new File(classLoader.getResource("ssl/truststore.ks").getFile()).getAbsolutePath();
    sslConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_SSL_ENABLED, ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.SSL_KEYSTORE_TYPE, ConfigValueFactory.fromAnyRef("JKS"))
        .withValue(ExecConstants.SSL_KEYSTORE_PATH, ConfigValueFactory.fromAnyRef(ksPath))
        .withValue(ExecConstants.SSL_KEYSTORE_PASSWORD, ConfigValueFactory.fromAnyRef("drill123"))
        .withValue(ExecConstants.SSL_KEY_PASSWORD, ConfigValueFactory.fromAnyRef("drill123"))
        .withValue(ExecConstants.SSL_PROTOCOL, ConfigValueFactory.fromAnyRef("TLSv1.2")));
    initProps = new Properties();
    initProps.setProperty(DrillProperties.ENABLE_TLS, "true");
    initProps.setProperty(DrillProperties.TRUSTSTORE_PATH, tsPath);
    initProps.setProperty(DrillProperties.TRUSTSTORE_PASSWORD, "drill123");
    initProps.setProperty(DrillProperties.DISABLE_HOST_VERIFICATION, "true");
  }

  @AfterClass
  public static void cleanTest() throws Exception {
    DrillConfig restoreConfig =
        new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties()));
    updateTestCluster(1, restoreConfig);
  }

  @Test
  public void testInvalidKeystorePath() throws Exception {
    DrillConfig testConfig = new DrillConfig(DrillConfig.create(sslConfig)
        .withValue(ExecConstants.SSL_KEYSTORE_PATH, ConfigValueFactory.fromAnyRef("/bad/path")));

    // Start an SSL enabled cluster
    boolean failureCaught = false;
    try {
      updateTestCluster(1, testConfig, initProps);
    } catch (Exception e) {
      failureCaught = true;
    }
    assertEquals(failureCaught, true);
  }

  @Test
  public void testInvalidKeystorePassword() throws Exception {
    DrillConfig testConfig = new DrillConfig(DrillConfig.create(sslConfig)
        .withValue(ExecConstants.SSL_KEYSTORE_PASSWORD, ConfigValueFactory.fromAnyRef("badpassword")));

    // Start an SSL enabled cluster
    boolean failureCaught = false;
    try {
      updateTestCluster(1, testConfig, initProps);
    } catch (Exception e) {
      failureCaught = true;
    }
    assertEquals(failureCaught, true);
  }

  @Test
  public void testInvalidKeyPassword() throws Exception {
    DrillConfig testConfig = new DrillConfig(DrillConfig.create(sslConfig)
        .withValue(ExecConstants.SSL_KEY_PASSWORD, ConfigValueFactory.fromAnyRef("badpassword")));

    // Start an SSL enabled cluster
    boolean failureCaught = false;
    try {
      updateTestCluster(1, testConfig, initProps);
    } catch (Exception e) {
      failureCaught = true;
    }
    assertEquals(failureCaught, true);
  }

  @Test
  // Should pass because the keystore password will be used.
  public void testNoKeyPassword() throws Exception {
    DrillConfig testConfig = new DrillConfig(DrillConfig.create(sslConfig)
        .withValue(ExecConstants.SSL_KEY_PASSWORD, ConfigValueFactory.fromAnyRef("")));

    // Start an SSL enabled cluster
    boolean failureCaught = false;
    try {
      updateTestCluster(1, testConfig, initProps);
    } catch (Exception e) {
      failureCaught = true;
    }
    assertEquals(failureCaught, false);
  }
}
