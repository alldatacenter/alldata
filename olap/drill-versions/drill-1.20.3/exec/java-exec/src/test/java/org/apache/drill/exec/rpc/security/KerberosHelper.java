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
package org.apache.drill.exec.rpc.security;


import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;

public class KerberosHelper {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KerberosHelper.class);

  public File workspace;

  private File kdcDir;
  private SimpleKdcServer kdc;
  private int kdcPort;

  private final String HOSTNAME = "localhost";

  public final String CLIENT_SHORT_NAME = "testUser";
  public final String CLIENT_PRINCIPAL;

  public String SERVER_PRINCIPAL;
  private final String testName;

  private File keytabDir;
  public File clientKeytab;
  public File serverKeytab;

  private boolean kdcStarted;

  public KerberosHelper(final String testName, String serverShortName) {
    final String realm = "EXAMPLE.COM";
    CLIENT_PRINCIPAL = CLIENT_SHORT_NAME + "@" + realm;

    if (serverShortName == null) {
      serverShortName = System.getProperty("user.name");
    }

    SERVER_PRINCIPAL = serverShortName + "/" + HOSTNAME + "@" + realm;
    this.testName = testName;
  }

  public void setupKdc(File workspace) throws Exception {
    this.workspace = workspace;
    kdc = new SimpleKdcServer();

    kdcDir = new File(workspace, testName);
    if(!kdcDir.mkdirs()) {
      throw new Exception(String.format("Failed to create the kdc directory %s", kdcDir.getName()));
    }
    kdc.setWorkDir(kdcDir);

    kdc.setKdcHost(HOSTNAME);
    kdcPort = getFreePort();
    kdc.setAllowTcp(true);
    kdc.setAllowUdp(false);
    kdc.setKdcTcpPort(kdcPort);

    logger.debug("Starting KDC server at {}:{}", HOSTNAME, kdcPort);

    kdc.init();
    kdc.start();
    kdcStarted = true;


    keytabDir = new File(workspace, testName + "_keytabs");
    if(!keytabDir.mkdirs()) {
      throw new Exception(String.format("Failed to create the keytab directory %s", keytabDir.getName()));
    }
    setupUsers(keytabDir);

    // Kerby sets "java.security.krb5.conf" for us!
    System.clearProperty("java.security.auth.login.config");
    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
    // Uncomment the following lines for debugging.
    // System.setProperty("sun.security.spnego.debug", "true");
    // System.setProperty("sun.security.krb5.debug", "true");
  }

  private int getFreePort() throws IOException {
    ServerSocket s = null;
    try {
      s = new ServerSocket(0);
      s.setReuseAddress(true);
      return s.getLocalPort();
    } finally {
      if (s != null) {
        s.close();
      }
    }
  }

  private void setupUsers(File keytabDir) throws KrbException {
    // Create the client user
    String clientPrincipal = CLIENT_PRINCIPAL.substring(0, CLIENT_PRINCIPAL.indexOf('@'));
    clientKeytab = new File(keytabDir, clientPrincipal.replace('/', '_') + ".keytab");
    logger.debug("Creating {} with keytab {}", clientPrincipal, clientKeytab);
    setupUser(kdc, clientKeytab, clientPrincipal);

    // Create the server user
    String serverPrincipal = SERVER_PRINCIPAL.substring(0, SERVER_PRINCIPAL.indexOf('@'));
    serverKeytab = new File(keytabDir, serverPrincipal.replace('/', '_') + ".keytab");
    logger.debug("Creating {} with keytab {}", SERVER_PRINCIPAL, serverKeytab);
    setupUser(kdc, serverKeytab, SERVER_PRINCIPAL);
  }

  private void setupUser(SimpleKdcServer kdc, File keytab, String principal)
    throws KrbException {
    kdc.createPrincipal(principal);
    kdc.exportPrincipal(principal, keytab);
  }

  /**
   * Workspace is owned by test using this helper
   * @throws Exception
   */
  public void stopKdc() throws Exception {
    if (kdcStarted) {
      logger.info("Stopping KDC on {}", kdcPort);
      kdc.stop();
    }

    deleteIfExists(clientKeytab);
    deleteIfExists(serverKeytab);
    deleteIfExists(keytabDir);
    deleteIfExists(kdcDir);
  }

  private void deleteIfExists(File file) throws IOException {
    if (file != null) {
      Files.deleteIfExists(file.toPath());
    }
  }
}
