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

import static org.easymock.EasyMock.createNiceMock;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.SecurePasswordHelper;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.state.stack.OsFamily;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class CredentialStoreServiceImplTest {

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  private CredentialStoreServiceImpl credentialStoreService;

  private static final String CLUSTER_NAME = "C1";

  @Before
  public void setUp() throws Exception {
    tmpFolder.create();
    final File masterKeyFile = tmpFolder.newFile(Configuration.MASTER_KEY_FILENAME_DEFAULT);
    Assert.assertTrue(new MasterKeyServiceImpl("dummyKey").initializeMasterKeyFile(masterKeyFile, "secret"));

    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();

        properties.setProperty(Configuration.MASTER_KEY_LOCATION.getKey(), tmpFolder.getRoot().getAbsolutePath());
        properties.setProperty(Configuration.MASTER_KEYSTORE_LOCATION.getKey(), tmpFolder.getRoot().getAbsolutePath());

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(SecurePasswordHelper.class).toInstance(new SecurePasswordHelper());
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });


    // Test to make sure the MasterKey file is ok...
    MasterKeyService masterKeyService = new MasterKeyServiceImpl(masterKeyFile);
    Assert.assertTrue(masterKeyService.isMasterKeyInitialized());

    credentialStoreService = injector.getInstance(CredentialStoreServiceImpl.class);
  }

  @After
  public void tearDown() throws Exception {
    credentialStoreService = null;
    tmpFolder.delete();
  }

  @Test
  public void testSetAndGetCredential_Temporary() throws Exception {
    PrincipalKeyCredential credential = new PrincipalKeyCredential("username", "password");
    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential, CredentialStoreType.TEMPORARY);

    Assert.assertEquals(credential, credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY));
    Assert.assertEquals(credential, credentialStoreService.getCredential(CLUSTER_NAME, "test1"));
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED));
  }

  @Test
  public void testSetAndGetCredential_Persisted() throws Exception {
    PrincipalKeyCredential credential = new PrincipalKeyCredential("username", "password");
    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential, CredentialStoreType.PERSISTED);

    Assert.assertEquals(credential, credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED));
    Assert.assertEquals(credential, credentialStoreService.getCredential(CLUSTER_NAME, "test1"));
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY));
  }

  @Test
  public void testRemoveCredential_Temporary() throws Exception {
    PrincipalKeyCredential credential1 = new PrincipalKeyCredential("username1", "password1");
    PrincipalKeyCredential credential2 = new PrincipalKeyCredential("username2", "password2");
    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential1, CredentialStoreType.TEMPORARY);
    credentialStoreService.setCredential(CLUSTER_NAME, "test2", credential2, CredentialStoreType.TEMPORARY);

    // Nothing should happen if forcing remove from persistent CredentialStore
    credentialStoreService.removeCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED);
    Assert.assertEquals(credential1, credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY));
    Assert.assertEquals(credential1, credentialStoreService.getCredential(CLUSTER_NAME, "test1"));

    // Remove should happen if forcing remove from temporary CredentialStore
    credentialStoreService.removeCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY);
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY));
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1"));

    // The other credentials should remain untouched
    Assert.assertEquals(credential2, credentialStoreService.getCredential(CLUSTER_NAME, "test2"));
  }

  @Test
  public void testRemoveCredential_Persisted() throws Exception {
    PrincipalKeyCredential credential1 = new PrincipalKeyCredential("username1", "password1");
    PrincipalKeyCredential credential2 = new PrincipalKeyCredential("username2", "password2");
    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential1, CredentialStoreType.PERSISTED);
    credentialStoreService.setCredential(CLUSTER_NAME, "test2", credential2, CredentialStoreType.PERSISTED);

    // Nothing should happen if forcing remove from temporary CredentialStore
    credentialStoreService.removeCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY);
    Assert.assertEquals(credential1, credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED));
    Assert.assertEquals(credential1, credentialStoreService.getCredential(CLUSTER_NAME, "test1"));

    // Remove should happen if forcing remove from persistent CredentialStore
    credentialStoreService.removeCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED);
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED));
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1"));

    // The other credentials should remain untouched
    Assert.assertEquals(credential2, credentialStoreService.getCredential(CLUSTER_NAME, "test2"));
  }

  @Test
  public void testRemoveCredential_Either() throws Exception {
    PrincipalKeyCredential credential1 = new PrincipalKeyCredential("username1", "password1");
    PrincipalKeyCredential credential2 = new PrincipalKeyCredential("username2", "password2");
    PrincipalKeyCredential credential3 = new PrincipalKeyCredential("username3", "password3");
    PrincipalKeyCredential credential4 = new PrincipalKeyCredential("username4", "password4");
    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential1, CredentialStoreType.PERSISTED);
    credentialStoreService.setCredential(CLUSTER_NAME, "test2", credential2, CredentialStoreType.PERSISTED);
    credentialStoreService.setCredential(CLUSTER_NAME, "test3", credential3, CredentialStoreType.TEMPORARY);
    credentialStoreService.setCredential(CLUSTER_NAME, "test4", credential4, CredentialStoreType.TEMPORARY);

    credentialStoreService.removeCredential(CLUSTER_NAME, "test1");
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED));
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1"));
    Assert.assertEquals(credential2, credentialStoreService.getCredential(CLUSTER_NAME, "test2"));
    Assert.assertEquals(credential3, credentialStoreService.getCredential(CLUSTER_NAME, "test3"));
    Assert.assertEquals(credential4, credentialStoreService.getCredential(CLUSTER_NAME, "test4"));

    credentialStoreService.removeCredential(CLUSTER_NAME, "test3");
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1"));
    Assert.assertEquals(credential2, credentialStoreService.getCredential(CLUSTER_NAME, "test2"));
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test3"));
    Assert.assertEquals(credential4, credentialStoreService.getCredential(CLUSTER_NAME, "test4"));
  }


  @Test
  public void testUpdateCredential() throws Exception {
    PrincipalKeyCredential credential1 = new PrincipalKeyCredential("username1", "password1");
    PrincipalKeyCredential credential2 = new PrincipalKeyCredential("username2", "password2");

    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential1, CredentialStoreType.PERSISTED);
    Assert.assertEquals(credential1, credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED));
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY));
    Assert.assertEquals(credential1, credentialStoreService.getCredential(CLUSTER_NAME, "test1"));

    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential1, CredentialStoreType.TEMPORARY);
    Assert.assertEquals(credential1, credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY));
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED));
    Assert.assertEquals(credential1, credentialStoreService.getCredential(CLUSTER_NAME, "test1"));

    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential2, CredentialStoreType.PERSISTED);
    Assert.assertEquals(credential2, credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED));
    Assert.assertNull(credentialStoreService.getCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY));
    Assert.assertEquals(credential2, credentialStoreService.getCredential(CLUSTER_NAME, "test1"));
  }


  @Test
  public void testContainsCredential() throws Exception {
    PrincipalKeyCredential credential1 = new PrincipalKeyCredential("username1", "password1");
    PrincipalKeyCredential credential2 = new PrincipalKeyCredential("username2", "password2");
    PrincipalKeyCredential credential3 = new PrincipalKeyCredential("username3", "password3");
    PrincipalKeyCredential credential4 = new PrincipalKeyCredential("username4", "password4");
    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential1, CredentialStoreType.PERSISTED);
    credentialStoreService.setCredential(CLUSTER_NAME, "test2", credential2, CredentialStoreType.PERSISTED);
    credentialStoreService.setCredential(CLUSTER_NAME, "test3", credential3, CredentialStoreType.TEMPORARY);
    credentialStoreService.setCredential(CLUSTER_NAME, "test4", credential4, CredentialStoreType.TEMPORARY);


    Assert.assertTrue(credentialStoreService.containsCredential(CLUSTER_NAME, "test1", CredentialStoreType.PERSISTED));
    Assert.assertFalse(credentialStoreService.containsCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY));
    Assert.assertFalse(credentialStoreService.containsCredential(CLUSTER_NAME, "test3", CredentialStoreType.PERSISTED));

    Assert.assertFalse(credentialStoreService.containsCredential(CLUSTER_NAME, "test1", CredentialStoreType.TEMPORARY));
    Assert.assertFalse(credentialStoreService.containsCredential(CLUSTER_NAME, "test3", CredentialStoreType.PERSISTED));
    Assert.assertTrue(credentialStoreService.containsCredential(CLUSTER_NAME, "test3", CredentialStoreType.TEMPORARY));

    Assert.assertTrue(credentialStoreService.containsCredential(CLUSTER_NAME, "test1"));
    Assert.assertTrue(credentialStoreService.containsCredential(CLUSTER_NAME, "test3"));
  }


  @Test
  public void testIsCredentialPersisted() throws Exception {
    PrincipalKeyCredential credential1 = new PrincipalKeyCredential("username1", "password1");
    PrincipalKeyCredential credential2 = new PrincipalKeyCredential("username2", "password2");
    PrincipalKeyCredential credential3 = new PrincipalKeyCredential("username3", "password3");
    PrincipalKeyCredential credential4 = new PrincipalKeyCredential("username4", "password4");
    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential1, CredentialStoreType.PERSISTED);
    credentialStoreService.setCredential(CLUSTER_NAME, "test2", credential2, CredentialStoreType.PERSISTED);
    credentialStoreService.setCredential(CLUSTER_NAME, "test3", credential3, CredentialStoreType.TEMPORARY);
    credentialStoreService.setCredential(CLUSTER_NAME, "test4", credential4, CredentialStoreType.TEMPORARY);

    Assert.assertEquals(CredentialStoreType.PERSISTED, credentialStoreService.getCredentialStoreType(CLUSTER_NAME, "test1"));
    Assert.assertEquals(CredentialStoreType.PERSISTED, credentialStoreService.getCredentialStoreType(CLUSTER_NAME, "test2"));
    Assert.assertEquals(CredentialStoreType.TEMPORARY, credentialStoreService.getCredentialStoreType(CLUSTER_NAME, "test3"));
    Assert.assertEquals(CredentialStoreType.TEMPORARY, credentialStoreService.getCredentialStoreType(CLUSTER_NAME, "test4"));

    try {
      credentialStoreService.getCredentialStoreType(CLUSTER_NAME, "test5");
      Assert.fail("Expected AmbariException to be thrown");
    } catch (AmbariException e) {
      // expected
    }
  }

  @Test
  public void testListCredentials() throws Exception {
    PrincipalKeyCredential credential1 = new PrincipalKeyCredential("username1", "password1");
    PrincipalKeyCredential credential2 = new PrincipalKeyCredential("username2", "password2");
    PrincipalKeyCredential credential3 = new PrincipalKeyCredential("username3", "password3");
    PrincipalKeyCredential credential4 = new PrincipalKeyCredential("username4", "password4");
    credentialStoreService.setCredential(CLUSTER_NAME, "test1", credential1, CredentialStoreType.PERSISTED);
    credentialStoreService.setCredential(CLUSTER_NAME, "test2", credential2, CredentialStoreType.PERSISTED);
    credentialStoreService.setCredential(CLUSTER_NAME, "test3", credential3, CredentialStoreType.TEMPORARY);
    credentialStoreService.setCredential(CLUSTER_NAME, "test4", credential4, CredentialStoreType.TEMPORARY);

    Map<String, CredentialStoreType> credentials = credentialStoreService.listCredentials(CLUSTER_NAME);
    Assert.assertNotNull(credentials);
    Assert.assertEquals(4, credentials.size());
    Assert.assertEquals(CredentialStoreType.PERSISTED, credentials.get("test1"));
    Assert.assertEquals(CredentialStoreType.PERSISTED, credentials.get("test2"));
    Assert.assertEquals(CredentialStoreType.TEMPORARY, credentials.get("test3"));
    Assert.assertEquals(CredentialStoreType.TEMPORARY, credentials.get("test4"));
  }

  @Test(expected = AmbariException.class)
  public void testFailToReinitialize_Persisted() throws Exception {
    // This should throw an exception not matter what the arguments are....
    credentialStoreService.initializePersistedCredentialStore(null, null);
  }

  @Test(expected = AmbariException.class)
  public void testFailToReinitialize_Temporary() throws Exception {
    // This should throw an exception not matter what the arguments are....
    credentialStoreService.initializeTemporaryCredentialStore(1, TimeUnit.MINUTES, false);
  }

  @Test
  public void testFailNotInitialized() throws Exception {
    Configuration configuration = new Configuration(new Properties());
    CredentialStoreService uninitializedCredentialStoreService = new CredentialStoreServiceImpl(configuration, new SecurePasswordHelper());
    PrincipalKeyCredential credential1 = new PrincipalKeyCredential("username1", "password1");

    // The temporary store should always be initialized.... this should succeed.
    uninitializedCredentialStoreService.setCredential(CLUSTER_NAME, "test1", credential1, CredentialStoreType.TEMPORARY);

    try {
      uninitializedCredentialStoreService.setCredential(CLUSTER_NAME, "test1", credential1, CredentialStoreType.PERSISTED);
      Assert.fail("AmbariException should have been thrown");
    } catch (AmbariException e) {
      // This is expected...
    }
  }
}