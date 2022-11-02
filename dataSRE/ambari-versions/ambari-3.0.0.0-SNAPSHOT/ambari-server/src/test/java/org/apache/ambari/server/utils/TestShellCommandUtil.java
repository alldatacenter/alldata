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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.Assert;

public class TestShellCommandUtil {

  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    try {
      temp.create();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @After
  public void tearDown() throws IOException {
    temp.delete();
  }

  @Test
  public void testOSDetection() throws Exception {
    // At least check, that only one OS is selected
    Assert.assertTrue(ShellCommandUtil.LINUX ^ ShellCommandUtil.WINDOWS
            ^ ShellCommandUtil.MAC);
    Assert.assertTrue(ShellCommandUtil.LINUX || ShellCommandUtil.MAC ==
            ShellCommandUtil.UNIX_LIKE);
  }

  @Test
  public void testUnixFilePermissions() throws Exception {
    File dummyFile = new File(temp.getRoot() + File.separator + "dummy");
    new FileOutputStream(dummyFile).close();
    if (ShellCommandUtil.LINUX) {
      ShellCommandUtil.setUnixFilePermissions("600",
              dummyFile.getAbsolutePath());
      String p = ShellCommandUtil.getUnixFilePermissions(
              dummyFile.getAbsolutePath());
      Assert.assertEquals("600", p);

      ShellCommandUtil.setUnixFilePermissions("444",
              dummyFile.getAbsolutePath());
      p = ShellCommandUtil.getUnixFilePermissions(
              dummyFile.getAbsolutePath());
      Assert.assertEquals("444", p);

      ShellCommandUtil.setUnixFilePermissions("777",
              dummyFile.getAbsolutePath());
      p = ShellCommandUtil.getUnixFilePermissions(
              dummyFile.getAbsolutePath());
      Assert.assertEquals("777", p);

    } else {
      // Next command is silently ignored, it's OK
      ShellCommandUtil.setUnixFilePermissions(ShellCommandUtil.MASK_OWNER_ONLY_RW,
              dummyFile.getAbsolutePath());
      // On Windows/Mac, output is always MASK_EVERYBODY_RWX
      String p = ShellCommandUtil.getUnixFilePermissions(
              dummyFile.getAbsolutePath());
      Assert.assertEquals(p, ShellCommandUtil.MASK_EVERYBODY_RWX);
    }
  }


  @Test
  public void testRunCommand() throws Exception {
    ShellCommandUtil.Result result;
    if (ShellCommandUtil.LINUX) {
      result = ShellCommandUtil.
              runCommand(new String [] {"echo", "dummy"});
      Assert.assertEquals(0, result.getExitCode());
      Assert.assertEquals("dummy\n", result.getStdout());
      Assert.assertEquals("", result.getStderr());
      Assert.assertTrue(result.isSuccessful());

      result = ShellCommandUtil.
              runCommand(new String [] {"false"});
      Assert.assertEquals(1, result.getExitCode());
      Assert.assertFalse(result.isSuccessful());
    } else {
      // Skipping this test under Windows/Mac
    }
  }

  @Test
  public void testRunInteractiveCommand() throws Exception {

    ShellCommandUtil.InteractiveHandler interactiveHandler = new ShellCommandUtil.InteractiveHandler() {
      boolean done = false;

      @Override
      public boolean done() {
        return done;
      }

      @Override
      public String getResponse(String query) {
        if (query.contains("Arg1")) {
          return "a1";
        } else if (query.contains("Arg2")) {
          done = true; // this is the last expected prompt
          return "a2";
        } else {
          return null;
        }
      }

      @Override
      public void start() {

      }
    };

    ShellCommandUtil.Result result = ShellCommandUtil.runCommand(new String[]{"./src/test/resources/interactive_shell_test.sh"}, null, interactiveHandler, false);
    Assert.assertEquals(0, result.getExitCode());
    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals("a1\na2\n", result.getStdout());
  }

  @Test
  public void testHideOpenSslPassword(){
    String command_pass = "openssl ca -config ca.config -in agent_hostname1.csr -out "+
            "agent_hostname1.crt -batch -passin pass:1234 -keyfile ca.key -cert ca.crt";
    String command_key = "openssl ca -create_serial -out /var/lib/ambari-server/keys/ca.crt -days 365 -keyfile /var/lib/ambari-server/keys/ca.key " +
        "-key 1234 -selfsign -extensions jdk7_ca " +
        "-config /var/lib/ambari-server/keys/ca.config -batch " +
        "-infiles /var/lib/ambari-server/keys/ca.csr";
    Assert.assertFalse(ShellCommandUtil.hideOpenSslPassword(command_pass).contains("1234"));
    Assert.assertFalse(ShellCommandUtil.hideOpenSslPassword(command_key).contains("1234"));
  }

  @Test
  public void testResultsClassIsPublic() throws Exception {
    Class resultClass = ShellCommandUtil.Result.class;

    Assert.assertEquals(Modifier.PUBLIC, resultClass.getModifiers() & Modifier.PUBLIC);

    for(Method method : resultClass.getMethods()) {
      Assert.assertEquals(method.getName(), Modifier.PUBLIC, (method.getModifiers() & Modifier.PUBLIC));
    }
  }

  @Test
  public void testPathExists() throws Exception {
    File nonExisting = new File(temp.getRoot(), "i_do_not_exist");
    File existing = temp.newFolder();

    ShellCommandUtil.Result result;

    result = ShellCommandUtil.pathExists(existing.getAbsolutePath(), false);
    Assert.assertTrue(existing.exists());
    Assert.assertTrue(result.isSuccessful());

    result = ShellCommandUtil.pathExists(nonExisting.getAbsolutePath(), false);
    Assert.assertFalse(nonExisting.exists());
    Assert.assertFalse(result.isSuccessful());
  }

  @Test
  public void testMkdir() throws Exception {
    File directory = new File(temp.getRoot(), "newdir");

    ShellCommandUtil.Result result = ShellCommandUtil.mkdir(directory.getAbsolutePath(), false);

    Assert.assertTrue(result.isSuccessful());
    Assert.assertTrue(directory.exists());
  }

  @Test
  public void testCopy() throws Exception {
    File srcFile = temp.newFile();
    File destFile = new File(srcFile.getParentFile(), "copied_file");

    FileWriter writer = new FileWriter(srcFile);
    writer.write("Hello World!!!!!");
    writer.close();

    ShellCommandUtil.Result result = ShellCommandUtil.copyFile(srcFile.getAbsolutePath(), destFile.getAbsolutePath(), false, false);

    Assert.assertTrue(result.isSuccessful());
    Assert.assertTrue(destFile.exists());
    Assert.assertTrue(destFile.length() > 0);
    Assert.assertEquals(destFile.length(), srcFile.length());
  }

  @Test
  public void deleteExistingFile() throws Exception {
    File file = temp.newFile();

    ShellCommandUtil.Result result = ShellCommandUtil.delete(file.getAbsolutePath(), false, false);

    Assert.assertTrue(result.getStderr(), result.isSuccessful());
    Assert.assertFalse(file.exists());
  }

  @Test
  public void deleteNonexistentFile() throws Exception {
    File file = temp.newFile();

    if (file.delete()) {
      ShellCommandUtil.Result result = ShellCommandUtil.delete(file.getAbsolutePath(), false, false);

      Assert.assertFalse(result.getStderr(), result.isSuccessful());
      Assert.assertFalse(file.exists());
    }
  }

  @Test
  public void forceDeleteNonexistentFile() throws Exception {
    File file = temp.newFile();

    if (file.delete()) {
      ShellCommandUtil.Result result = ShellCommandUtil.delete(file.getAbsolutePath(), true, false);

      Assert.assertTrue(result.getStderr(), result.isSuccessful());
      Assert.assertFalse(file.exists());
    }
  }

}
