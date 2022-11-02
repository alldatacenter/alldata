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

package org.apache.ambari.server.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.encryption.CredentialProvider;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PasswordUtils.class)
public class PasswordUtilsTest extends EasyMockSupport {

  private final static String CS_ALIAS = "${alias=testAlias}";

  private PasswordUtils passwordUtils;
  
  private Injector injector;
  private Configuration configuration;

  @Before
  public void setUp() {
    injector = createInjector();
    configuration = injector.getInstance(Configuration.class);
    passwordUtils = PasswordUtils.getInstance();
  }

  @Test
  public void shouldReadPasswordFromCredentialStoreOfAnAlias() throws Exception {
    final CredentialProvider credentialProvider = PowerMock.createNiceMock(CredentialProvider.class);
    setupBasicCredentialProviderExpectations(credentialProvider);
    credentialProvider.getPasswordForAlias(CS_ALIAS);
    PowerMock.expectLastCall().andReturn("testPassword".toCharArray()).once();
    PowerMock.replay(credentialProvider, CredentialProvider.class);
    replayAll();
    assertEquals("testPassword", passwordUtils.readPassword(CS_ALIAS, "testPassword"));
    verifyAll();
  }
  
  @Test
  public void shouldReadPasswordFromFileIfPasswordPropertyIsPasswordFilePath() throws Exception {
    final String testPassword = "ambariTest";
    final File passwordFile = writeTestPasswordFile(testPassword);
    assertEquals("ambariTest", passwordUtils.readPassword(passwordFile.getAbsolutePath(), "testPasswordDefault"));
  }
  
  @Test
  public void shouldReadDefaultPasswordIfPasswordPropertyIsPasswordFilePathButItDoesNotExists() throws Exception {
    final File passwordFile = new File("/my/test/password/file.dat");
    assertEquals("testPasswordDefault", passwordUtils.readPassword(passwordFile.getAbsolutePath(), "testPasswordDefault"));
  }
  
  @Test
  @Ignore("until we fix fo for ANY kind of users including root")
  public void shouldReadDefaultPasswordIfPasswordPropertyIsPasswordFilePathButItIsNotReadable() throws Exception {
    final String testPassword = "ambariTest";
    final File passwordFile = writeTestPasswordFile(testPassword);
    passwordFile.setReadable(false);
    assertEquals("testPasswordDefault", passwordUtils.readPassword(passwordFile.getAbsolutePath(), "testPasswordDefault"));
  }

  private File writeTestPasswordFile(final String testPassword) throws IOException {
    final TemporaryFolder tempFolder = new TemporaryFolder();
    tempFolder.create();
    final File passwordFile = tempFolder.newFile();
    passwordFile.deleteOnExit();
    FileUtils.writeStringToFile(passwordFile, testPassword, Charset.defaultCharset());
    return passwordFile;
  }

  private void setupBasicCredentialProviderExpectations(CredentialProvider credentialProvider) throws Exception {
    PowerMock.expectNew(CredentialProvider.class, null, null, configuration).andReturn(credentialProvider);
  }

  private Injector createInjector() {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(createNiceMock(Configuration.class));
        
        binder().requestStaticInjection(PasswordUtils.class);
      }
    });
  }

}
