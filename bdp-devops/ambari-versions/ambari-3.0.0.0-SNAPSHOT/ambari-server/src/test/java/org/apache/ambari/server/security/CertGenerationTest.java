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

package org.apache.ambari.server.security;

import static org.easymock.EasyMock.createNiceMock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.TestCase;

public class CertGenerationTest {

  private static final int PASS_FILE_NAME_LEN = 20;
  private static final float MAX_PASS_LEN = 100;

  private static final Logger LOG = LoggerFactory.getLogger(CertGenerationTest.class);
  public static TemporaryFolder temp = new TemporaryFolder();

  private static Injector injector;

  private static CertificateManager certMan;
  private static String passFileName;
  private static int passLen;

  @Inject
  static void init(CertificateManager instance) {
    certMan = instance;
  }


  private static class SecurityModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(Properties.class).toInstance(buildTestProperties());
      bind(Configuration.class).toConstructor(getConfigurationConstructor());
      bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      requestStaticInjection(CertGenerationTest.class);
    }
  }

  protected static Properties buildTestProperties() {
    try {
      temp.create();
    } catch (IOException e) {
      e.printStackTrace();
    }
	  Properties properties = new Properties();
	  properties.setProperty(Configuration.SRVR_KSTR_DIR.getKey(),
      temp.getRoot().getAbsolutePath());
    passLen = (int) Math.abs((new Random().nextFloat() * MAX_PASS_LEN));

    properties.setProperty(Configuration.SRVR_CRT_PASS_LEN.getKey(),
      String.valueOf(passLen));

    passFileName = RandomStringUtils.randomAlphabetic(PASS_FILE_NAME_LEN);
    properties.setProperty(Configuration.SRVR_CRT_PASS_FILE.getKey(), passFileName);

	  return properties;
  }

  protected static Constructor<Configuration> getConfigurationConstructor() {
    try {
      return Configuration.class.getConstructor(Properties.class);
	} catch (NoSuchMethodException e) {
	    throw new RuntimeException("Expected constructor not found in Configuration.java", e);
	   }
	}

  @BeforeClass
  public static void setUpBeforeClass() throws IOException {


    injector = Guice.createInjector(new SecurityModule());
    certMan = injector.getInstance(CertificateManager.class);

    //Test using actual ca.config.
    try {
      File caConfig = new File("conf/unix/ca.config");
      if (System.getProperty("os.name").contains("Windows")) {
        caConfig = new File(new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).getParentFile().getParentFile(), "conf\\windows\\ca.config");
      }
      File caConfigTest = new File(temp.getRoot().getAbsolutePath(), "ca.config");
      File newCertsDir = new File(temp.getRoot().getAbsolutePath(), "newcerts");
      newCertsDir.mkdirs();
      File indexTxt = new File(temp.getRoot().getAbsolutePath(), "index.txt");
      indexTxt.createNewFile();

      String content = IOUtils.toString(new FileInputStream(caConfig));
      if (System.getProperty("os.name").contains("Windows")) {
        content = content.replace("keystore\\\\db", temp.getRoot().getAbsolutePath().replace("\\", "\\\\"));
      }
      else {
        content = content.replaceAll("/var/lib/ambari-server/keys/db", temp.getRoot().getAbsolutePath());
      }
      IOUtils.write(content, new FileOutputStream(caConfigTest));
    } catch (IOException e) {
      e.printStackTrace();
      TestCase.fail();
    }

    certMan.initRootCert();
  }

  @AfterClass
  public static void tearDownAfterClass() throws IOException {
    temp.delete();
  }

  @Ignore // randomly fails on BAO (e.g. https://builds.apache.org/job/Ambari-branch-2.2/155/console)
  @Test
  public void testServerCertGen() throws Exception {
    File serverCrt = new File(temp.getRoot().getAbsoluteFile() + File.separator + Configuration.SRVR_CRT_NAME.getDefaultValue());
    Assert.assertTrue(serverCrt.exists());
  }

  @Test
  public void testServerKeyGen() throws Exception {
    File serverKey = new File(temp.getRoot().getAbsoluteFile() + File.separator + Configuration.SRVR_KEY_NAME.getDefaultValue());
    Assert.assertTrue(serverKey.exists());
  }

  @Ignore // randomly fails on BAO (e.g. https://builds.apache.org/job/Ambari-branch-2.2/155/console)
  @Test
  public void testServerKeystoreGen() throws Exception {
    File serverKeyStrore = new File(temp.getRoot().getAbsoluteFile() + File.separator + Configuration.KSTR_NAME.getDefaultValue());
    Assert.assertTrue(serverKeyStrore.exists());
  }

  @Ignore // randomly fails on BAO (e.g. https://builds.apache.org/job/Ambari-branch-2.2/155/console)
  @Test
  public void testRevokeExistingAgentCert() throws Exception {

    Map<String,String> config = certMan.configs.getConfigsMap();
    config.put(Configuration.PASSPHRASE.getKey(),"passphrase");

    String agentHostname = "agent_hostname";
    SignCertResponse scr = certMan.signAgentCrt(agentHostname,
      "incorrect_agentCrtReqContent", "passphrase");
    //Revoke command wasn't executed
    Assert.assertFalse(scr.getMessage().contains("-revoke"));

    //Emulate existing agent certificate
    File fakeAgentCertFile = new File(temp.getRoot().getAbsoluteFile() +
      File.separator + agentHostname + ".crt");
    Assert.assertTrue(fakeAgentCertFile.exists());

    //Revoke command was executed
    scr = certMan.signAgentCrt(agentHostname,
      "incorrect_agentCrtReqContent", "passphrase");
    Assert.assertTrue(scr.getMessage().contains("-revoke"));
  }

  @Test
  public void testPassFileGen() throws Exception {

    File passFile = new File(temp.getRoot().getAbsolutePath() + File.separator
      + passFileName);

    Assert.assertTrue(passFile.exists());

    String pass = FileUtils.readFileToString(passFile, Charset.defaultCharset());

    Assert.assertEquals(pass.length(), passLen);

    if (ShellCommandUtil.LINUX) {
      String permissions = ShellCommandUtil.
        getUnixFilePermissions(passFile.getAbsolutePath());
      Assert.assertEquals(ShellCommandUtil.MASK_OWNER_ONLY_RW, permissions);
    }

  }
}
