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
package org.apache.ambari.server.security.encryption;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.credential.GenericKeyCredential;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Ticker;

import junit.framework.Assert;

public class CredentialStoreTest {

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    tmpFolder.create();
  }

  @After
  public void cleanUp() throws Exception {
    tmpFolder.delete();
  }

  @Test
  public void testFileBasedCredentialStoreService_AddCredentialToStoreWithPersistMaster() throws Exception {
    addCredentialToStoreWithPersistMasterTest(new FileBasedCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testFileBasedCredentialStoreService_AddCredentialToStore() throws Exception {
    addCredentialToStoreTest(new FileBasedCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testFileBasedCredentialStoreService_GetCredential() throws Exception {
    getCredentialTest(new FileBasedCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testFileBasedCredentialStoreService_RemoveCredential() throws Exception {
    removeCredentialTest(new FileBasedCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testInMemoryCredentialStoreService_AddCredentialToStoreWithPersistMaster() throws Exception {
    addCredentialToStoreWithPersistMasterTest(new InMemoryCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testInMemoryCredentialStoreService_AddCredentialToStore() throws Exception {
    addCredentialToStoreTest(new InMemoryCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testInMemoryCredentialStoreService_GetCredential() throws Exception {
    getCredentialTest(new InMemoryCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testInMemoryCredentialStoreService_RemoveCredential() throws Exception {
    removeCredentialTest(new InMemoryCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  @Test
  public void testInMemoryCredentialStoreService_CredentialExpired() throws Exception {
    getExpiredCredentialTest(new InMemoryCredentialStoreServiceFactory(), new DefaultMasterKeyServiceFactory());
  }

  private void addCredentialToStoreWithPersistMasterTest(CredentialStoreServiceFactory credentialStoreServiceFactory,
                                                         MasterKeyServiceFactory masterKeyServiceFactory) throws Exception {
    File directory = tmpFolder.getRoot();

    String masterKey = "ThisIsSomeSecretPassPhrase1234";
    File masterKeyFile = new File(directory, "master");

    MasterKeyService masterKeyService = masterKeyServiceFactory.createPersisted(masterKeyFile, masterKey);
    CredentialStore credentialStore = credentialStoreServiceFactory.create(directory, masterKeyService);

    String password = "mypassword";
    credentialStore.addCredential("myalias", new GenericKeyCredential(password.toCharArray()));
    Credential credential = credentialStore.getCredential("myalias");
    Assert.assertEquals(password, new String(credential.toValue()));

    Assert.assertTrue(masterKeyFile.exists());
  }

  private void addCredentialToStoreTest(CredentialStoreServiceFactory credentialStoreServiceFactory,
                                        MasterKeyServiceFactory masterKeyServiceFactory) throws Exception {
    File directory = tmpFolder.getRoot();

    String masterKey = "ThisIsSomeSecretPassPhrase1234";
    File masterKeyFile = new File(directory, "master");

    MasterKeyService masterKeyService = masterKeyServiceFactory.create(masterKey);
    CredentialStore credentialStore = credentialStoreServiceFactory.create(directory, masterKeyService);

    String password = "mypassword";
    credentialStore.addCredential("password", new GenericKeyCredential(password.toCharArray()));
    Credential credential = credentialStore.getCredential("password");
    Assert.assertEquals(password, new String(credential.toValue()));

    credentialStore.addCredential("null_password", null);
    Assert.assertNull(credentialStore.getCredential("null_password"));

    credentialStore.addCredential("empty_password", new GenericKeyCredential(new char[0]));
    Assert.assertNull(credentialStore.getCredential("empty_password"));

    Assert.assertFalse(masterKeyFile.exists());
  }

  private void getCredentialTest(CredentialStoreServiceFactory credentialStoreServiceFactory,
                                 MasterKeyServiceFactory masterKeyServiceFactory) throws Exception {
    File directory = tmpFolder.getRoot();

    String masterKey = "ThisIsSomeSecretPassPhrase1234";

    MasterKeyService masterKeyService = masterKeyServiceFactory.create(masterKey);
    CredentialStore credentialStore = credentialStoreServiceFactory.create(directory, masterKeyService);

    Assert.assertNull(credentialStore.getCredential(""));
    Assert.assertNull(credentialStore.getCredential(null));

    String password = "mypassword";
    credentialStore.addCredential("myalias", new GenericKeyCredential(password.toCharArray()));
    Credential credential = credentialStore.getCredential("myalias");
    Assert.assertEquals(password, new String(credential.toValue()));

    Assert.assertNull(credentialStore.getCredential("does_not_exist"));
  }

  private void getExpiredCredentialTest(CredentialStoreServiceFactory credentialStoreServiceFactory,
                                 MasterKeyServiceFactory masterKeyServiceFactory) throws Exception {
    File directory = tmpFolder.getRoot();

    String masterKey = "ThisIsSomeSecretPassPhrase1234";

    MasterKeyService masterKeyService = masterKeyServiceFactory.create(masterKey);
    CredentialStore credentialStore = credentialStoreServiceFactory.create(directory, masterKeyService);

    // replace cache ticker with manual to avoid timeout issues during check
    TestTicker ticker = new TestTicker(0);

    Field cacheFiled = InMemoryCredentialStore.class.getDeclaredField("cache");
    cacheFiled.setAccessible(true);

    Object cache = cacheFiled.get(credentialStore);

    Class localManualCacheClass = Class.forName("com.google.common.cache.LocalCache$LocalManualCache");
    Field localCacheField = localManualCacheClass.getDeclaredField("localCache");
    localCacheField.setAccessible(true);

    Class localCacheClass = Class.forName("com.google.common.cache.LocalCache");
    Object localCache = localCacheField.get(cache);

    Field tickerField = localCacheClass.getDeclaredField("ticker");
    tickerField.setAccessible(true);
    tickerField.set(localCache, ticker);

    String password = "mypassword";
    credentialStore.addCredential("myalias", new GenericKeyCredential(password.toCharArray()));
    Assert.assertEquals(password, new String(credentialStore.getCredential("myalias").toValue()));

    // 250 msec
    ticker.setCurrentNanos(250*1000*1000);
    Assert.assertEquals(password, new String(credentialStore.getCredential("myalias").toValue()));

    // 550 msec
    ticker.setCurrentNanos(550*1000*1000);
    Assert.assertNull(password, credentialStore.getCredential("myalias"));

  }

  private void removeCredentialTest(CredentialStoreServiceFactory credentialStoreServiceFactory,
                                    MasterKeyServiceFactory masterKeyServiceFactory) throws Exception {
    File directory = tmpFolder.getRoot();

    String masterKey = "ThisIsSomeSecretPassPhrase1234";

    MasterKeyService masterKeyService = masterKeyServiceFactory.create(masterKey);
    CredentialStore credentialStore = credentialStoreServiceFactory.create(directory, masterKeyService);

    String password = "mypassword";
    credentialStore.addCredential("myalias", new GenericKeyCredential(password.toCharArray()));

    Credential credential = credentialStore.getCredential("myalias");
    Assert.assertEquals(password, new String(credential.toValue()));

    credentialStore.removeCredential("myalias");
    Assert.assertNull(credentialStore.getCredential("myalias"));

    credentialStore.addCredential("myalias", new GenericKeyCredential(password.toCharArray()));
    credential = credentialStore.getCredential("myalias");
    Assert.assertEquals(password, new String(credential.toValue()));

    credentialStore = credentialStoreServiceFactory.create(directory, masterKeyService);
    credentialStore.setMasterKeyService(masterKeyService);

    credentialStore.removeCredential("myalias");
    Assert.assertNull(credentialStore.getCredential("myalias"));

    credentialStore.removeCredential("does_not_exist");
  }

  private interface CredentialStoreServiceFactory {
    CredentialStore create(File path, MasterKeyService masterKeyService);
  }

  private class FileBasedCredentialStoreServiceFactory implements CredentialStoreServiceFactory {
    @Override
    public CredentialStore create(File path, MasterKeyService masterKeyService) {
      CredentialStore credentialStore = new FileBasedCredentialStore(path);
      credentialStore.setMasterKeyService(masterKeyService);
      return credentialStore;
    }
  }

  private class InMemoryCredentialStoreServiceFactory implements CredentialStoreServiceFactory {
    @Override
    public CredentialStore create(File path, MasterKeyService masterKeyService) {
      CredentialStore credentialStore = new InMemoryCredentialStore(500, TimeUnit.MILLISECONDS, false);
      credentialStore.setMasterKeyService(masterKeyService);
      return credentialStore;
    }
  }

  private interface MasterKeyServiceFactory {
    MasterKeyService create(String masterKey);

    MasterKeyService createPersisted(File masterKeyFile, String masterKey);
  }

  private class DefaultMasterKeyServiceFactory implements MasterKeyServiceFactory {

    @Override
    public MasterKeyService create(String masterKey) {
      return new MasterKeyServiceImpl(masterKey);
    }

    @Override
    public MasterKeyService createPersisted(File masterKeyFile, String masterKey) {
      new MasterKeyServiceImpl("dummyKey").initializeMasterKeyFile(masterKeyFile, masterKey);
      return new MasterKeyServiceImpl(masterKeyFile);
    }
  }

  private class TestTicker extends Ticker {

    private long currentNanos;

    public TestTicker(long currentNanos) {
      this.currentNanos = currentNanos;
    }

    @Override
    public long read() {
      return currentNanos;
    }

    public void setCurrentNanos(long currentNanos) {
      this.currentNanos = currentNanos;
    }
  }

}
