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

package org.apache.ambari.server.serveraction.kerberos;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.apache.ambari.server.utils.ShellCommandUtil;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.Assert;

public class CreateKeytabFilesServerActionTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void testEnsureAmbariOnlyAccess() throws Exception {
    Assume.assumeTrue(ShellCommandUtil.UNIX_LIKE);

    Path path;
    Set<PosixFilePermission> permissions;

    File directory = testFolder.newFolder();
    Assert.assertNotNull(directory);

    new CreateKeytabFilesServerAction().ensureAmbariOnlyAccess(directory);

    // The directory is expected to have the following permissions: rwx------ (700)
    path = Paths.get(directory.getAbsolutePath());
    Assert.assertNotNull(path);

    permissions = Files.getPosixFilePermissions(path);
    Assert.assertNotNull(permissions);

    Assert.assertNotNull(permissions);
    Assert.assertEquals(3, permissions.size());
    Assert.assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
    Assert.assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
    Assert.assertTrue(permissions.contains(PosixFilePermission.OWNER_EXECUTE));
    Assert.assertFalse(permissions.contains(PosixFilePermission.GROUP_READ));
    Assert.assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
    Assert.assertFalse(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
    Assert.assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
    Assert.assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
    Assert.assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));

    File file = File.createTempFile("temp_", "", directory);
    Assert.assertNotNull(file);
    Assert.assertTrue(file.exists());

    new CreateKeytabFilesServerAction().ensureAmbariOnlyAccess(file);

    // The file is expected to have the following permissions: rw------- (600)
    path = Paths.get(file.getAbsolutePath());
    Assert.assertNotNull(path);

    permissions = Files.getPosixFilePermissions(path);
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
  }
}