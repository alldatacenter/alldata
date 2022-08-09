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
package org.apache.ambari.server.bootstrap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.bootstrap.BootStrapStatus.BSStat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ncole
 *
 */
class BSRunner extends Thread {
  private static final Logger LOG = LoggerFactory.getLogger(BSRunner.class);

  private static final String DEFAULT_USER = "root";
  private static final String DEFAULT_SSHPORT = "22";

  private  boolean finished = false;
  private SshHostInfo sshHostInfo;
  private File bootDir;
  private String bsScript;
  private File requestIdDir;
  private File sshKeyFile;
  private File passwordFile;
  private int requestId;
  private String agentSetupScript;
  private String agentSetupPassword;
  private String ambariHostname;
  private boolean verbose;
  private BootStrapImpl bsImpl;
  private final String clusterOsFamily;
  private String projectVersion;
  private int serverPort;

  public BSRunner(BootStrapImpl impl, SshHostInfo sshHostInfo, String bootDir,
      String bsScript, String agentSetupScript, String agentSetupPassword,
      int requestId, long timeout, String hostName, boolean isVerbose, String clusterOsFamily,
      String projectVersion, int serverPort)
  {
    this.requestId = requestId;
    this.sshHostInfo = sshHostInfo;
    this.bsScript = bsScript;
    this.bootDir = new File(bootDir);
    this.requestIdDir = new File(bootDir, Integer.toString(requestId));
    this.sshKeyFile = new File(this.requestIdDir, "sshKey");
    this.agentSetupScript = agentSetupScript;
    this.agentSetupPassword = agentSetupPassword;
    this.ambariHostname = hostName;
    this.verbose = isVerbose;
    this.clusterOsFamily = clusterOsFamily;
    this.projectVersion = projectVersion;
    this.bsImpl = impl;
    this.serverPort = serverPort;
    BootStrapStatus status = new BootStrapStatus();
    status.setLog("RUNNING");
    status.setStatus(BSStat.RUNNING);
    bsImpl.updateStatus(requestId, status);
  }

  /**
   * Update the gathered data from reading output
   *
   */
  private class BSStatusCollector implements Runnable {
    @Override
    public void run() {
      BSHostStatusCollector collector = new BSHostStatusCollector(requestIdDir,
          sshHostInfo.getHosts());
      collector.run();
      List<BSHostStatus> hostStatus = collector.getHostStatus();
      BootStrapStatus status = new BootStrapStatus();
      status.setHostsStatus(hostStatus);
      status.setLog("");
      status.setStatus(BSStat.RUNNING);
      bsImpl.updateStatus(requestId, status);
    }
  }

  private String createHostString(List<String> list) {
    return list != null ? String.join(",", list) : StringUtils.EMPTY;
  }

  /** Create request id dir for each bootstrap call **/
  private void createRunDir() throws IOException {
    if (!bootDir.exists()) {
      // create the bootdir directory.
      if (! bootDir.mkdirs()) {
        throw new IOException("Cannot create " + bootDir);
      }
    }
    /* create the request id directory */
    if (requestIdDir.exists()) {
      /* delete the directory and make sure we start back */
      FileUtils.deleteDirectory(requestIdDir);
    }
    /* create the directory for the run dir */
    if (! requestIdDir.mkdirs()) {
      throw new IOException("Cannot create " + requestIdDir);
    }
  }

  private void writeSshKeyFile(String data) throws IOException {
    FileUtils.writeStringToFile(sshKeyFile, data, Charset.defaultCharset());
  }

  private void writePasswordFile(String data) throws IOException {
    FileUtils.writeStringToFile(passwordFile, data, Charset.defaultCharset());
  }

  /**
   * Waits until the process has terminated or waiting time elapses.
   * @param timeout time to wait in miliseconds
   * @return true if process has exited, false otherwise
   */
  private boolean waitForProcessTermination(Process process, long timeout) throws InterruptedException {
    long startTime = System.currentTimeMillis();
    do {
      try {
        process.exitValue();
        return true;
      } catch (IllegalThreadStateException ignored) {}
      // Check if process has terminated once per second
      Thread.sleep(1000);
    } while (System.currentTimeMillis() - startTime < timeout);
    return false;
  }

  /**
   * Calculates bootstrap timeout as a function of number of hosts.
   * @return timeout in milliseconds
   */
  private long calculateBSTimeout(int hostCount) {
    final int PARALLEL_BS_COUNT = 20; // bootstrap.py bootstraps 20 hosts in parallel
    final long HOST_BS_TIMEOUT = 300000L; // 5 minutes timeout for a host (average). Same as in bootstrap.py

    return Math.max(HOST_BS_TIMEOUT, HOST_BS_TIMEOUT * hostCount / PARALLEL_BS_COUNT);
  }

  public synchronized void finished() {
    this.finished = true;
  }

  @Override
  public void run() {
    String hostString = createHostString(sshHostInfo.getHosts());
    long bootstrapTimeout = calculateBSTimeout(sshHostInfo.getHosts().size());
    // Startup a scheduled executor service to look through the logs
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    BSStatusCollector statusCollector = new BSStatusCollector();
    ScheduledFuture<?> handle = null;
    LOG.info("Kicking off the scheduler for polling on logs in " +
    this.requestIdDir);
    String user = sshHostInfo.getUser();
    String userRunAs = sshHostInfo.getUserRunAs();
    if (user == null || user.isEmpty()) {
      user = DEFAULT_USER;
    }

    String sshPort = sshHostInfo.getSshPort();
    if(sshPort == null || sshPort.isEmpty()){
       sshPort = DEFAULT_SSHPORT;
    }

    String command[] = new String[13];
    BSStat stat = BSStat.RUNNING;
    String scriptlog = "";
    try {
      createRunDir();
      handle = scheduler.scheduleWithFixedDelay(statusCollector,
        0, 10, TimeUnit.SECONDS);
      if (LOG.isDebugEnabled()) {
        // FIXME needs to be removed later
        // security hole
        LOG.debug("Using ssh key=\"{}\"", sshHostInfo.getSshKey());
      }

      String password = sshHostInfo.getPassword();
      if (password != null && !password.isEmpty()) {
        this.passwordFile = new File(this.requestIdDir, "host_pass");
        // TODO : line separator should be changed
        // if we are going to support multi platform server-agent solution
        String lineSeparator = System.getProperty("line.separator");
        password = password + lineSeparator;
        writePasswordFile(password);
      }

      writeSshKeyFile(sshHostInfo.getSshKey());
      /* Running command:
       * script hostlist bsdir user sshkeyfile
       */
      command[0] = this.bsScript;
      command[1] = hostString;
      command[2] = this.requestIdDir.toString();
      command[3] = user;
      command[4] = sshPort;
      command[5] = this.sshKeyFile.toString();
      command[6] = this.agentSetupScript.toString();
      command[7] = this.ambariHostname;
      command[8] = this.clusterOsFamily;
      command[9] = this.projectVersion;
      command[10] = this.serverPort+"";
      command[11] = userRunAs;
      command[12] = (this.passwordFile==null) ? "null" : this.passwordFile.toString();

      Map<String, String> envVariables = new HashMap<>();

      if (System.getProperty("os.name").contains("Windows")) {
        String command2[] = new String[command.length + 1];
        command2[0] = "python";
        System.arraycopy(command, 0, command2, 1, command.length);
        command = command2;

        Map<String, String> envVarsWin = System.getenv();
        if (envVarsWin != null) {
          envVariables.putAll(envVarsWin);  //envVarsWin is non-modifiable
        }
      }

      LOG.info("Host= " + hostString + " bs=" + this.bsScript + " requestDir=" +
          requestIdDir + " user=" + user + " sshPort=" + sshPort + " keyfile=" + this.sshKeyFile +
          " passwordFile " + this.passwordFile + " server=" + this.ambariHostname +
          " version=" + projectVersion + " serverPort=" + this.serverPort + " userRunAs=" + userRunAs +
          " timeout=" + bootstrapTimeout / 1000);

      envVariables.put("AMBARI_PASSPHRASE", agentSetupPassword);
      if (this.verbose)
        envVariables.put("BS_VERBOSE", "\"-vvv\"");

      if (LOG.isDebugEnabled()) {
        LOG.debug(Arrays.toString(command));
      }

      String bootStrapOutputFilePath = requestIdDir + File.separator + "bootstrap.out";
      String bootStrapErrorFilePath = requestIdDir + File.separator + "bootstrap.err";

      ProcessBuilder pb = new ProcessBuilder(command);
      pb.redirectOutput(new File(bootStrapOutputFilePath));
      pb.redirectError(new File(bootStrapErrorFilePath));
      Map<String, String> env = pb.environment();
      env.putAll(envVariables);

      Process process = pb.start();

      try {
        String logInfoMessage = "Bootstrap output, log="
              + bootStrapErrorFilePath + " " + bootStrapOutputFilePath + " at " + this.ambariHostname;
        LOG.info(logInfoMessage);

        int exitCode = 1;
        boolean timedOut = false;
        if (waitForProcessTermination(process, bootstrapTimeout)){
          exitCode = process.exitValue();
        } else {
          LOG.warn("Bootstrap process timed out. It will be destroyed.");
          process.destroy();
          timedOut = true;
        }

        String outMesg = "";
        String errMesg = "";       
        try {
          outMesg = FileUtils
                  .readFileToString(new File(bootStrapOutputFilePath), Charset.defaultCharset());
          errMesg = FileUtils
                  .readFileToString(new File(bootStrapErrorFilePath), Charset.defaultCharset());
        } catch(IOException io) {
          LOG.info("Error in reading files ", io);
        }
        scriptlog = outMesg + "\n\n" + errMesg;
        if (timedOut) {
          scriptlog += "\n\n Bootstrap process timed out. It was destroyed.";
        }
        LOG.info("Script log Mesg " + scriptlog);
        if (exitCode != 0) {
          stat = BSStat.ERROR;
          interuptSetupAgent(99, scriptlog);
        } else {
          stat = BSStat.SUCCESS;
        }

        scheduler.schedule(new BSStatusCollector(), 0, TimeUnit.SECONDS);
        long startTime = System.currentTimeMillis();
        while (true) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Waiting for hosts status to be updated");
          }
          boolean pendingHosts = false;
          BootStrapStatus tmpStatus = bsImpl.getStatus(requestId);
          List <BSHostStatus> hostStatusList = tmpStatus.getHostsStatus();
          if (hostStatusList != null) {
            for (BSHostStatus status : hostStatusList) {
              if (status.getStatus().equals("RUNNING")) {
                pendingHosts = true;
              }
            }
          } else {
            //Failed to get host status, waiting for hosts status to be updated
            pendingHosts = true;
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("Whether hosts status yet to be updated, pending={}", pendingHosts);
          }
          if (!pendingHosts) {
            break;
          }
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            // continue
          }
          long now = System.currentTimeMillis();
          if (now >= (startTime+15000)) {
            LOG.warn("Gave up waiting for hosts status to be updated");
            break;
          }
        }
      } catch (InterruptedException e) {
        throw new IOException(e);
      } finally {
        if (handle != null) {
          handle.cancel(true);
        }
        /* schedule a last update */
        scheduler.schedule(new BSStatusCollector(), 0, TimeUnit.SECONDS);
        scheduler.shutdownNow();
        try {
          scheduler.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          LOG.info("Interruped while waiting for scheduler");
        }
        process.destroy();
      }
    } catch(IOException io) {
      LOG.info("Error executing bootstrap " + io.getMessage());
      stat = BSStat.ERROR;
      interuptSetupAgent(99, io.getMessage()); 
    }
    finally {
      /* get the bstatus */
      BootStrapStatus tmpStatus = bsImpl.getStatus(requestId);
      List <BSHostStatus> hostStatusList = tmpStatus.getHostsStatus();
      if (hostStatusList != null) {
        for (BSHostStatus hostStatus : hostStatusList) {
          if ("FAILED".equals(hostStatus.getStatus())) {
            stat = BSStat.ERROR;
            break;
          }
        }
      } else {
        stat = BSStat.ERROR;
      }

      // creating new status instance to avoid modifying exposed object
      BootStrapStatus newStat = new BootStrapStatus();
      newStat.setHostsStatus(hostStatusList);
      newStat.setLog(scriptlog);
      newStat.setStatus(stat);

      // Remove private ssh key after bootstrap is complete
      try {
        FileUtils.forceDelete(sshKeyFile);
      } catch (IOException io) {
        LOG.warn(io.getMessage());
      }
      if (passwordFile != null) {
        // Remove password file after bootstrap is complete
        try {
          FileUtils.forceDelete(passwordFile);
        } catch (IOException io) {
          LOG.warn(io.getMessage());
        }
      }

      bsImpl.updateStatus(requestId, newStat);
      bsImpl.reset();

      finished();
    }
  }

  public synchronized void interuptSetupAgent(int exitCode, String errMesg){
    PrintWriter setupAgentDoneWriter = null;
    PrintWriter setupAgentLogWriter  = null;
    try {
      for (String host : sshHostInfo.getHosts()) {
        File doneFile = new File(requestIdDir, host + BSHostStatusCollector.doneFileFilter);

        // Do not rewrite finished host statuses
        if (!doneFile.exists()) {
          setupAgentDoneWriter = new PrintWriter(doneFile);
          setupAgentDoneWriter.print(exitCode);
          setupAgentDoneWriter.close();
        }

        File logFile = new File(requestIdDir, host + BSHostStatusCollector.logFileFilter);
        if (!logFile.exists()) {
          setupAgentLogWriter = new PrintWriter(logFile);
          setupAgentLogWriter.print("Error while bootstrapping:\n" + errMesg);
          setupAgentLogWriter.close();
        }
      }
    } catch (FileNotFoundException ex) {
      LOG.error(ex.toString());
    } finally {
      if (setupAgentDoneWriter != null) {
        setupAgentDoneWriter.close();
      }

      if (setupAgentLogWriter != null) {
        setupAgentLogWriter.close();
      }
    }    
  }
  
  public synchronized boolean isRunning() {
    return !this.finished;
  }
}
