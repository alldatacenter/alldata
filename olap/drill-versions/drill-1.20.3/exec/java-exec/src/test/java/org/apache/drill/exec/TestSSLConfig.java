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
package org.apache.drill.exec;


import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.ssl.SSLConfig;
import org.apache.drill.exec.ssl.SSLConfigBuilder;
import org.apache.drill.test.BaseTest;
import org.apache.drill.test.ConfigBuilder;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;
import org.junit.experimental.categories.Category;


import java.text.MessageFormat;

import static junit.framework.TestCase.fail;
import static org.apache.drill.exec.ssl.SSLConfig.HADOOP_SSL_CONF_TPL_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(SecurityTest.class)
public class TestSSLConfig extends BaseTest {

  @Test
  public void testMissingKeystorePath() throws Exception {

    ConfigBuilder config = new ConfigBuilder();
    config.put(ExecConstants.HTTP_KEYSTORE_PATH, "");
    config.put(ExecConstants.HTTP_KEYSTORE_PASSWORD, "root");
    config.put(ExecConstants.SSL_USE_HADOOP_CONF, false);
    config.put(ExecConstants.USER_SSL_ENABLED, true);
    try {
      SSLConfig sslv = new SSLConfigBuilder()
          .config(config.build())
          .mode(SSLConfig.Mode.SERVER)
          .initializeSSLContext(false)
          .validateKeyStore(true)
          .build();
      fail();
      //Expected
    } catch (Exception e) {
      assertTrue(e instanceof DrillException);
    }
  }

  @Test
  public void testMissingKeystorePassword() throws Exception {

    ConfigBuilder config = new ConfigBuilder();
    config.put(ExecConstants.HTTP_KEYSTORE_PATH, "/root");
    config.put(ExecConstants.HTTP_KEYSTORE_PASSWORD, "");
    config.put(ExecConstants.SSL_USE_HADOOP_CONF, false);
    config.put(ExecConstants.USER_SSL_ENABLED, true);
    try {
      SSLConfig sslv = new SSLConfigBuilder()
          .config(config.build())
          .mode(SSLConfig.Mode.SERVER)
          .initializeSSLContext(false)
          .validateKeyStore(true)
          .build();
      fail();
      //Expected
    } catch (Exception e) {
      assertTrue(e instanceof DrillException);
    }
  }

  @Test
  public void testForKeystoreConfig() throws Exception {

    ConfigBuilder config = new ConfigBuilder();
    config.put(ExecConstants.HTTP_KEYSTORE_PATH, "/root");
    config.put(ExecConstants.HTTP_KEYSTORE_PASSWORD, "root");
    try {
      SSLConfig sslv = new SSLConfigBuilder()
          .config(config.build())
          .mode(SSLConfig.Mode.SERVER)
          .initializeSSLContext(false)
          .validateKeyStore(true)
          .build();
      assertEquals("/root", sslv.getKeyStorePath());
      assertEquals("root", sslv.getKeyStorePassword());
    } catch (Exception e) {
      fail();

    }
  }

  @Test
  public void testForBackwardCompatability() throws Exception {

    ConfigBuilder config = new ConfigBuilder();
    config.put("javax.net.ssl.keyStore", "/root");
    config.put("javax.net.ssl.keyStorePassword", "root");
    SSLConfig sslv = new SSLConfigBuilder()
        .config(config.build())
        .mode(SSLConfig.Mode.SERVER)
        .initializeSSLContext(false)
        .validateKeyStore(true)
        .build();
    assertEquals("/root",sslv.getKeyStorePath());
    assertEquals("root", sslv.getKeyStorePassword());
  }

  @Test
  public void testForTrustStore() throws Exception {

    ConfigBuilder config = new ConfigBuilder();
    config.put(ExecConstants.HTTP_TRUSTSTORE_PATH, "/root");
    config.put(ExecConstants.HTTP_TRUSTSTORE_PASSWORD, "root");
    config.put(ExecConstants.SSL_USE_HADOOP_CONF, false);
    SSLConfig sslv = new SSLConfigBuilder()
        .config(config.build())
        .mode(SSLConfig.Mode.SERVER)
        .initializeSSLContext(false)
        .validateKeyStore(true)
        .build();
    assertEquals(true, sslv.hasTrustStorePath());
    assertEquals(true,sslv.hasTrustStorePassword());
    assertEquals("/root",sslv.getTrustStorePath());
    assertEquals("root",sslv.getTrustStorePassword());
  }

  @Test
  public void testInvalidHadoopKeystore() throws Exception {
    Configuration hadoopConfig = new Configuration();
    String hadoopSSLFileProp = MessageFormat
        .format(HADOOP_SSL_CONF_TPL_KEY, SSLConfig.Mode.SERVER.toString().toLowerCase());
    hadoopConfig.set(hadoopSSLFileProp, "ssl-server-invalid.xml");
    ConfigBuilder config = new ConfigBuilder();
    config.put(ExecConstants.USER_SSL_ENABLED, true);
    config.put(ExecConstants.SSL_USE_HADOOP_CONF, true);
    SSLConfig sslv;
    try {
      sslv = new SSLConfigBuilder()
          .config(config.build())
          .mode(SSLConfig.Mode.SERVER)
          .initializeSSLContext(false)
          .validateKeyStore(true)
          .hadoopConfig(hadoopConfig)
          .build();
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof DrillException);
    }
  }

}
