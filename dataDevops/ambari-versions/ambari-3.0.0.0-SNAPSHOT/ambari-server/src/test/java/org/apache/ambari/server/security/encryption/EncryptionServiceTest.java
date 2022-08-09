/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.encryption;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.TextEncoding;
import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MasterKeyServiceImpl.class })
@PowerMockIgnore({ "javax.crypto.*" })
public class EncryptionServiceTest {

  @Rule
  private final TemporaryFolder tmpFolder = new TemporaryFolder();


  @Test
  public void testEncryptAndDecryptUsingCustomKeyWithBase64Encoding() throws Exception {
    testEncryptAndDecryptUsingCustomKey(TextEncoding.BASE_64);
  }

  @Test
  public void testEncryptAndDecryptUsingCustomKeyWithBinHex64Encoding() throws Exception {
    testEncryptAndDecryptUsingCustomKey(TextEncoding.BIN_HEX);
  }

  public void testEncryptAndDecryptUsingCustomKey(TextEncoding textEncoding) throws Exception {
    final String key = "mySuperS3cr3tMast3rKey!";
    final String toBeEncrypted = "mySuperS3cr3tP4ssW0rD!";
    EncryptionService encryptionService = new AESEncryptionService();
    final String encrypted = encryptionService.encrypt(toBeEncrypted, key, textEncoding);
    final String decrypted = encryptionService.decrypt(encrypted, key, textEncoding);
    assertEquals(toBeEncrypted, decrypted);
  }

  @Test
  public void testEncryptAndDecryptUsingPersistedMasterKey()throws Exception {
    final String fileDir = tmpFolder.newFolder("keys").getAbsolutePath();
    final File masterKeyFile = new File(fileDir, "master");
    final String masterKey = "mySuperS3cr3tMast3rKey!";
    final MasterKeyServiceImpl ms = new MasterKeyServiceImpl("dummyKey");
    Assert.assertTrue(ms.initializeMasterKeyFile(masterKeyFile, masterKey));

    final String toBeEncrypted = "mySuperS3cr3tP4ssW0rD!";

    Configuration configuration = new Configuration(new Properties());
    configuration.setProperty(Configuration.MASTER_KEY_LOCATION, masterKeyFile.getParent());
    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(Configuration.class).toInstance(configuration);
        bind(OsFamily.class).toInstance(EasyMock.createMock(OsFamily.class));
      }
    });
    EncryptionService encryptionService = new AESEncryptionService();
    injector.injectMembers(encryptionService);

    final String encrypted = encryptionService.encrypt(toBeEncrypted);
    final String decrypted = encryptionService.decrypt(encrypted);
    verifyAll();
    assertEquals(toBeEncrypted, decrypted);
  }

  @Test
  public void testEncryptAndDecryptUsingEnvDefinedMasterKey() throws Exception {
    final String fileDir = tmpFolder.newFolder("keys").getAbsolutePath();
    final File masterKeyFile = new File(fileDir, "master");
    final String masterKey = "mySuperS3cr3tMast3rKey!";
    final MasterKeyServiceImpl ms = new MasterKeyServiceImpl("dummyKey");
    Assert.assertTrue(ms.initializeMasterKeyFile(masterKeyFile, masterKey));

    final String toBeEncrypted = "mySuperS3cr3tP4ssW0rD!";

    setupEnvironmentVariableExpectations(masterKeyFile);
    EncryptionService encryptionService = new AESEncryptionService();
    final String encrypted = encryptionService.encrypt(toBeEncrypted);
    final String decrypted = encryptionService.decrypt(encrypted);
    verifyAll();
    assertEquals(toBeEncrypted, decrypted);
  }

  @Test(expected = SecurityException.class)
  public void shouldThrowSecurityExceptionInCaseOfEncryptingWithNonExistingPersistedMasterKey() throws Exception {
    final String toBeEncrypted = "mySuperS3cr3tP4ssW0rD!";
    EncryptionService encryptionService = new AESEncryptionService();
    encryptionService.encrypt(toBeEncrypted);
  }

  private void setupEnvironmentVariableExpectations(final File masterKeyFile) {
    final Map<String, String> sysEnvironment = new HashMap<>();
    sysEnvironment.put(Configuration.MASTER_KEY_LOCATION.getKey(), masterKeyFile.getAbsolutePath());
    mockStatic(System.class);
    expect(System.getenv()).andReturn(sysEnvironment);
    replayAll();
  }
}
