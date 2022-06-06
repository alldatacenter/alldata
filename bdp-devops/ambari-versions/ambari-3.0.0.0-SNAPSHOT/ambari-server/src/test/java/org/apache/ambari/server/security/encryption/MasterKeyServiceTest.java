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
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.configuration.Configuration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;
import junit.framework.TestCase;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*", "org.apache.log4j.*"})
@PrepareForTest({MasterKeyServiceImpl.class})
public class MasterKeyServiceTest extends TestCase {
  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();
  private String fileDir;
  private static final Logger LOG = LoggerFactory.getLogger
      (MasterKeyServiceTest.class);

  @Override
  protected void setUp() throws Exception {
    tmpFolder.create();
    fileDir = tmpFolder.newFolder("keys").getAbsolutePath();
    LOG.info("Setting temp folder to: " + fileDir);
  }

  @Test
  public void testInitializeMasterKey() throws Exception {
    File masterKeyFile = new File(fileDir, "master");
    MasterKeyServiceImpl ms = new MasterKeyServiceImpl("dummyKey");
    Assert.assertTrue(ms.initializeMasterKeyFile(masterKeyFile, "ThisisSomePassPhrase"));

    ms = new MasterKeyServiceImpl(masterKeyFile);
    Assert.assertTrue(ms.isMasterKeyInitialized());

    Assert.assertTrue(masterKeyFile.exists());

    // Make sure the created file is readable and writable only by the process owner.
    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(Paths.get(masterKeyFile.getAbsolutePath()));
    Assert.assertNotNull(permissions);
    Assert.assertEquals(2, permissions.size());
    Assert.assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
    Assert.assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
    Assert.assertFalse(permissions.contains(PosixFilePermission.OWNER_EXECUTE));
    Assert.assertFalse(permissions.contains(PosixFilePermission.GROUP_READ));
    Assert.assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
    Assert.assertFalse(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
    Assert.assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
    Assert.assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
    Assert.assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));

    // Re-initialize master from file
    MasterKeyService ms1 = new MasterKeyServiceImpl(masterKeyFile);
    Assert.assertTrue(ms1.isMasterKeyInitialized());

    Assert.assertEquals("ThisisSomePassPhrase", new String(ms1.getMasterSecret()));
    Assert.assertEquals(new String(ms.getMasterSecret()), new String(ms1.getMasterSecret()));
  }

  @Test
  public void testReadFromEnvAsKey() throws Exception {
    Map<String, String> mapRet = new HashMap<>();
    mapRet.put("AMBARI_SECURITY_MASTER_KEY", "ThisisSomePassPhrase");
    mockStatic(System.class);
    expect(System.getenv()).andReturn(mapRet);
    replayAll();
    Configuration configuration = new Configuration(new Properties());
    MasterKeyService ms = new MasterKeyServiceImpl(configuration);
    verifyAll();
    Assert.assertTrue(ms.isMasterKeyInitialized());
    Assert.assertNotNull(ms.getMasterSecret());
    Assert.assertEquals("ThisisSomePassPhrase",
        new String(ms.getMasterSecret()));
  }

  @Test
  public void testReadFromEnvAsPath() throws Exception {
    // Create a master key
    File masterKeyFile = new File(fileDir, "master");
    MasterKeyServiceImpl ms = new MasterKeyServiceImpl("dummyKey");
    Assert.assertTrue(ms.initializeMasterKeyFile(masterKeyFile, "ThisisSomePassPhrase"));

    ms = new MasterKeyServiceImpl(masterKeyFile);
    Assert.assertTrue(ms.isMasterKeyInitialized());
    Assert.assertTrue(masterKeyFile.exists());

    Map<String, String> mapRet = new HashMap<>();
    mapRet.put(Configuration.MASTER_KEY_LOCATION.getKey(), masterKeyFile.getAbsolutePath());
    mockStatic(System.class);
    expect(System.getenv()).andReturn(mapRet);
    replayAll();
    Configuration configuration = new Configuration(new Properties());
    ms = new MasterKeyServiceImpl(configuration);
    verifyAll();
    Assert.assertTrue(ms.isMasterKeyInitialized());
    Assert.assertNotNull(ms.getMasterSecret());
    Assert.assertEquals("ThisisSomePassPhrase", new String(ms.getMasterSecret()));
    Assert.assertTrue(masterKeyFile.exists());
  }

  @Override
  protected void tearDown() throws Exception {
    tmpFolder.delete();
  }

}
