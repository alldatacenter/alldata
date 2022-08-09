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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.utils.HostUtils;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Ambari security.
 * Manages server and agent certificates
 */
@Singleton
public class CertificateManager {

  private static final Logger LOG = LoggerFactory.getLogger(CertificateManager.class);

  @Inject Configuration configs;

  private static final String GEN_SRVR_KEY = "openssl genrsa -des3 " +
      "-passout pass:{0} -out {1}" + File.separator + "{2} 4096 ";
  private static final String GEN_SRVR_REQ = "openssl req -passin pass:{0} " +
      "-new -key {1}" + File.separator + "{2} -out {1}" + File.separator + "{5} -batch";
  private static final String SIGN_SRVR_CRT = "openssl ca -create_serial " +
    "-out {1}" + File.separator + "{3} -days 365 -keyfile {1}" + File.separator + "{2} -key {0} -selfsign " +
    "-extensions jdk7_ca -config {1}" + File.separator + "ca.config -batch " +
    "-infiles {1}" + File.separator + "{5}";
  private static final String EXPRT_KSTR = "openssl pkcs12 -export" +
      " -in {1}" + File.separator + "{3} -inkey {1}" + File.separator + "{2} -certfile {1}" + File.separator + "{3} -out {1}" + File.separator + "{4} " +
      "-password pass:{0} -passin pass:{0} \n";
  private static final String REVOKE_AGENT_CRT = "openssl ca " +
      "-config {0}" + File.separator + "ca.config -keyfile {0}" + File.separator + "{4} -revoke {0}" + File.separator + "{2} -batch " +
      "-passin pass:{3} -cert {0}" + File.separator + "{5}";
  private static final String SIGN_AGENT_CRT = "openssl ca -config " +
      "{0}" + File.separator + "ca.config -in {0}" + File.separator + "{1} -out {0}" + File.separator + "{2} -batch -passin pass:{3} " +
      "-keyfile {0}" + File.separator + "{4} -cert {0}" + File.separator + "{5}"; /**
       * Verify that root certificate exists, generate it otherwise.
       */
  private static final String SET_PERMISSIONS = "find %s -type f -exec chmod 700 {} +";

  private static final String SET_SERVER_PASS_FILE_PERMISSIONS = "chmod 600 %s";

  public void initRootCert() {
    LOG.info("Initialization of root certificate");
    boolean certExists = isCertExists();
    LOG.info("Certificate exists:" + certExists);

    if (!certExists) {
      generateServerCertificate();
    }
  }

  /**
   * Checks root certificate state.
   * @return "true" if certificate exists
   */
  private boolean isCertExists() {

    Map<String, String> configsMap = configs.getConfigsMap();
    String srvrKstrDir = configsMap.get(Configuration.SRVR_KSTR_DIR.getKey());
    String srvrCrtName = configsMap.get(Configuration.SRVR_CRT_NAME.getKey());
    File certFile = new File(srvrKstrDir + File.separator + srvrCrtName);
    LOG.debug("srvrKstrDir = {}", srvrKstrDir);
    LOG.debug("srvrCrtName = {}", srvrCrtName);
    LOG.debug("certFile = {}", certFile.getAbsolutePath());

    return certFile.exists();
  }


  /**
   * Runs os command
   *
   * @return command execution exit code
   */
  protected int runCommand(String command) {
    String line = null;
    Process process = null;
    BufferedReader br= null;
    try {
      process = Runtime.getRuntime().exec(command);
      br = new BufferedReader(new InputStreamReader(
          process.getInputStream(), Charset.forName("UTF8")));

      while ((line = br.readLine()) != null) {
        LOG.info(line);
      }

      try {
        process.waitFor();
        ShellCommandUtil.logOpenSslExitCode(command, process.exitValue());
        return process.exitValue(); //command is executed
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
    }

    return -1;//some exception occurred

  }

  private void generateServerCertificate() {
    LOG.info("Generation of server certificate");

    Map<String, String> configsMap = configs.getConfigsMap();
    String srvrKstrDir = configsMap.get(Configuration.SRVR_KSTR_DIR.getKey());
    String srvrCrtName = configsMap.get(Configuration.SRVR_CRT_NAME.getKey());
    String srvrCsrName = configsMap.get(Configuration.SRVR_CSR_NAME.getKey());
    String srvrKeyName = configsMap.get(Configuration.SRVR_KEY_NAME.getKey());
    String kstrName = configsMap.get(Configuration.KSTR_NAME.getKey());
    String srvrCrtPass = configsMap.get(Configuration.SRVR_CRT_PASS.getKey());
    String srvrCrtPassFile =  configsMap.get(Configuration.SRVR_CRT_PASS_FILE.getKey());

    Object[] scriptArgs = {srvrCrtPass, srvrKstrDir, srvrKeyName,
        srvrCrtName, kstrName, srvrCsrName};

    String command = MessageFormat.format(GEN_SRVR_KEY,scriptArgs);
    runCommand(command);

    command = MessageFormat.format(GEN_SRVR_REQ,scriptArgs);
    runCommand(command);

    command = MessageFormat.format(SIGN_SRVR_CRT,scriptArgs);
    runCommand(command);

    command = MessageFormat.format(EXPRT_KSTR,scriptArgs);
    runCommand(command);

    command = String.format(SET_PERMISSIONS,srvrKstrDir);
    runCommand(command);

    command = String.format(SET_SERVER_PASS_FILE_PERMISSIONS, srvrKstrDir + File.separator + srvrCrtPassFile);
    runCommand(command);
  }

  /**
   * Returns server's PEM-encoded CA chain file content
   * @return string server's PEM-encoded CA chain file content
   */
  public String getCACertificateChainContent() {
    String serverCertDir = configs.getProperty(Configuration.SRVR_KSTR_DIR);

    // Attempt to send the explicit CA certificate chain file.
    String serverCertChainName = configs.getProperty(Configuration.SRVR_CRT_CHAIN_NAME);
    File certChainFile = new File(serverCertDir, serverCertChainName);
    if(certChainFile.exists()) {
      try {
        return new String(Files.readAllBytes(certChainFile.toPath()));
      } catch (IOException e) {
        LOG.error(e.getMessage());
      }
    }

    // Fall back to the original way things were done and send the server's SSL certificate as the
    // Certificate chain file.
    String serverCertName = configs.getProperty(Configuration.SRVR_CRT_NAME);
    File certFile = new File(serverCertDir, serverCertName);
    if(certFile.canRead()) {
      try {
        return new String(Files.readAllBytes(certFile.toPath()));
      } catch (IOException e) {
        LOG.error(e.getMessage());
      }
    }

    // If all else fails, send nothing...
    return null;
  }

  /**
   * Signs agent certificate
   * Adds agent certificate to server keystore
   * @return string with agent signed certificate content
   */
  public synchronized SignCertResponse signAgentCrt(String agentHostname, String agentCrtReqContent, String passphraseAgent) {
    SignCertResponse response = new SignCertResponse();
    LOG.info("Signing agent certificate");

    // Ensure the hostname is not empty or null...
    agentHostname = StringUtils.trim(agentHostname);

    if(StringUtils.isEmpty(agentHostname)) {
      LOG.warn("The agent hostname is missing");
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage("The agent hostname is missing");
      return response;
    }

    // Optionally check the supplied hostname to make sure it is a valid hostname.
    // By default, this feature is turned on.  If this check is not desired (maybe the validation
    // rules are too strict), the feature may be turned off by setting the following
    // property in the ambari.properties file:
    //
    //    security.agent.hostname.validate = "false"
    //
    if(configs.validateAgentHostnames()) {
      LOG.info("Validating agent hostname: {}", agentHostname);
      if(!HostUtils.isValidHostname(agentHostname)) {
        LOG.warn("The agent hostname is not a valid hostname");
        response.setResult(SignCertResponse.ERROR_STATUS);
        response.setMessage("The agent hostname is not a valid hostname");
        return response;
      }
    }
    else {
      LOG.info("Skipping validation of agent hostname: {}", agentHostname);
    }

    LOG.info("Verifying passphrase");

    String passphraseSrvr = configs.getConfigsMap().get(Configuration.
        PASSPHRASE.getKey()).trim();

    if (!passphraseSrvr.equals(passphraseAgent.trim())) {
      LOG.warn("Incorrect passphrase from the agent");
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage("Incorrect passphrase from the agent");
      return response;
    }

    Map<String, String> configsMap = configs.getConfigsMap();
    String srvrKstrDir = configsMap.get(Configuration.SRVR_KSTR_DIR.getKey());
    String srvrCrtPass = configsMap.get(Configuration.SRVR_CRT_PASS.getKey());
    String srvrCrtName = configsMap.get(Configuration.SRVR_CRT_NAME.getKey());
    String srvrKeyName = configsMap.get(Configuration.SRVR_KEY_NAME.getKey());
    String agentCrtReqName = agentHostname + ".csr";
    String agentCrtName = agentHostname + ".crt";

    Object[] scriptArgs = {srvrKstrDir, agentCrtReqName, agentCrtName,
        srvrCrtPass, srvrKeyName, srvrCrtName};

    //Revoke previous agent certificate if exists
    File agentCrtFile = new File(srvrKstrDir + File.separator + agentCrtName);

    if (agentCrtFile.exists()) {
      LOG.info("Revoking of " + agentHostname + " certificate.");
      String command = MessageFormat.format(REVOKE_AGENT_CRT, scriptArgs);
      int commandExitCode = runCommand(command);
      if (commandExitCode != 0) {
        response.setResult(SignCertResponse.ERROR_STATUS);
        response.setMessage(ShellCommandUtil.getOpenSslCommandResult(command, commandExitCode));
        return response;
      }
    }

    File agentCrtReqFile = new File(srvrKstrDir + File.separator +
        agentCrtReqName);
    try {
      FileUtils.writeStringToFile(agentCrtReqFile, agentCrtReqContent, Charset.defaultCharset());
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    String command = MessageFormat.format(SIGN_AGENT_CRT, scriptArgs);

    LOG.debug(ShellCommandUtil.hideOpenSslPassword(command));

    int commandExitCode = runCommand(command); // ssl command execution
    if (commandExitCode != 0) {
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage(ShellCommandUtil.getOpenSslCommandResult(command, commandExitCode));
      //LOG.warn(ShellCommandUtil.getOpenSslCommandResult(command, commandExitCode));
      return response;
    }

    String agentCrtContent = "";
    try {
      agentCrtContent = FileUtils.readFileToString(agentCrtFile, Charset.defaultCharset());
    } catch (IOException e) {
      e.printStackTrace();
      LOG.error("Error reading signed agent certificate");
      response.setResult(SignCertResponse.ERROR_STATUS);
      response.setMessage("Error reading signed agent certificate");
      return response;
    }
    response.setResult(SignCertResponse.OK_STATUS);
    response.setSignedCa(agentCrtContent);
    //LOG.info(ShellCommandUtil.getOpenSslCommandResult(command, commandExitCode));
    return response;
  }
}
