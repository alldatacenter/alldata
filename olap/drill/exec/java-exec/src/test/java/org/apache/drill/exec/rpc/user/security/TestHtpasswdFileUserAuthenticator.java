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

import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(SecurityTest.class)
public class TestHtpasswdFileUserAuthenticator extends ClusterTest {
  private File tempPasswdFile;


  private void setupCluster(String passwdContent) throws IOException {
    tempPasswdFile = new File(dirTestWatcher.getTmpDir(), "htpasswd." + System.currentTimeMillis());
    Files.write(tempPasswdFile.toPath(), passwdContent.getBytes());

    cluster = ClusterFixture.bareBuilder(dirTestWatcher)
      .clusterSize(3)
      .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true)
      .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
      .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, "htpasswd")
      .configProperty(ExecConstants.HTPASSWD_AUTHENTICATOR_PATH, tempPasswdFile.toString())
      .build();
  }


  @Test
  public void passwordChecksGiveCorrectResults() throws Exception {
    String passwdContent = "alice:pass1\n" +
      "bob:buzzkill\n" +
      "jane:$apr1$PrwDfXy9$ajkhotQW6RFnoVQtPKoW4/\n" +
      "john:$apr1$UxZgBU8k$K4UzdubNa741TnWAZY2QV0\n";
    setupCluster(passwdContent);


    assertTrue(true);

    tryCredentials("alice", "pass1", cluster, true);
    tryCredentials("bob", "buzzkill", cluster, true);
    tryCredentials("notalice", "pass1", cluster, false);
    tryCredentials("notbob", "buzzkill", cluster, false);
    tryCredentials("alice", "wrong", cluster, false);
    tryCredentials("bob", "incorrect", cluster, false);
    tryCredentials("jane", "pass", cluster, true);
    tryCredentials("john", "foobar", cluster, true);
    tryCredentials("jane", "wrong", cluster, false);
    tryCredentials("john", "incorrect1", cluster, false);
  }

  @Test
  public void rejectsLoginsWhenHtpasswdFileMissing() throws Exception {
    cluster = ClusterFixture.bareBuilder(dirTestWatcher)
      .clusterSize(3)
      .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true)
      .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
      .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, "htpasswd")
      .configProperty(ExecConstants.HTPASSWD_AUTHENTICATOR_PATH, "/nonexistant-file")
      .build();
    tryCredentials("bob", "bob", cluster, false);
  }

  @Test
  public void detectsChanges() throws Exception {
    String passwdContent = "alice:pass1\nbob:buzzkill\n";
    setupCluster(passwdContent);

    tryCredentials("alice", "pass1", cluster, true);
    tryCredentials("alice", "pass2", cluster, false);
    tryCredentials("bob", "buzzkill", cluster, true);
    tryCredentials("bob", "yolo", cluster, false);

    String passwdContent2 = "alice:pass2\nbob:yolo\n";
    Files.write(tempPasswdFile.toPath(), passwdContent2.getBytes());

    tryCredentials("alice", "pass1", cluster, false);
    tryCredentials("alice", "pass2", cluster, true);
    tryCredentials("bob", "buzzkill", cluster, false);
    tryCredentials("bob", "yolo", cluster, true);

    // Invalid file is treated as empty
    String passwdContent3 = "invalid file";
    Files.write(tempPasswdFile.toPath(), passwdContent3.getBytes());

    tryCredentials("alice", "pass1", cluster, false);
    tryCredentials("alice", "pass2", cluster, false);

    // Missing file is treated as empty
    Files.delete(tempPasswdFile.toPath());

    tryCredentials("alice", "pass1", cluster, false);
    tryCredentials("alice", "pass2", cluster, false);

  }

  private static void tryCredentials(String user, String password, ClusterFixture cluster, boolean shouldSucceed) throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, user)
        .property(DrillProperties.PASSWORD, password)
        .build();

      // Run few queries using the new client
      List<String> queries = Arrays.asList(
        "SHOW SCHEMAS",
        "USE INFORMATION_SCHEMA",
        "SHOW TABLES",
        "SELECT * FROM INFORMATION_SCHEMA.`TABLES` WHERE TABLE_NAME LIKE 'COLUMNS'",
        "SELECT * FROM cp.`region.json` LIMIT 5");

      for (String query : queries) {
        client.queryBuilder().sql(query).run();
      }

      if (!shouldSucceed) {
        fail("Expected connect to fail because of incorrect username / password combination, but it succeeded");
      }
    } catch (IllegalStateException e) {
      if (shouldSucceed) {
        throw e;
      }
    }
  }

}
