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

package org.apache.ambari.server.serveraction.users;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.hooks.users.UserHookParams;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

@Singleton
public class PostUserCreationHookServerAction extends AbstractServerAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(PostUserCreationHookServerAction.class);
  private static final int MAX_SYMBOLS_PER_LOG_MESSAGE = 7900;

  @Inject
  private ShellCommandUtilityWrapper shellCommandUtilityWrapper;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private CollectionPersisterServiceFactory collectionPersisterServiceFactory;

  @Inject
  public PostUserCreationHookServerAction() {
    super();
  }

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
    LOGGER.debug("Executing custom script server action; Context: {}", requestSharedDataContext);
    ShellCommandUtil.Result result = null;
    CommandReport cmdReport = null;

    try {

      Map<String, String> commandParams = getCommandParameters();
      validateCommandParams(commandParams);

      //persist user data to csv
      CollectionPersisterService<String, List<String>> csvPersisterService = collectionPersisterServiceFactory.createCsvFilePersisterService(commandParams.get(UserHookParams.CMD_INPUT_FILE.param()));
      csvPersisterService.persistMap(getPayload(commandParams));

      String[] cmd = assembleCommand(commandParams);

      result = shellCommandUtilityWrapper.runCommand(cmd);

      // long command results need to be split to chunks to feed external log processors (eg.: syslog)
      logCommandResult(Arrays.asList(cmd).toString(), result);

      cmdReport = createCommandReport(result.getExitCode(), result.isSuccessful() ?
          HostRoleStatus.COMPLETED : HostRoleStatus.FAILED, "{}", result.getStdout(), result.getStderr());

      LOGGER.debug("Command report: {}", cmdReport);


    } catch (InterruptedException e) {
      LOGGER.error("The server action thread has been interrupted", e);
      throw e;
    } catch (Exception e) {
      LOGGER.error("Server action is about to quit due to an exception.", e);
      throw new AmbariException("Server action execution failed to complete!", e);
    }

    return cmdReport;
  }

  private void logCommandResult(String command, ShellCommandUtil.Result result) {
    LOGGER.info("Execution of command [ {} ] - {}", command, result.isSuccessful() ? "succeeded" : "failed");
    String stringToLog = result.isSuccessful() ? result.getStdout() : result.getStderr();
    if (stringToLog == null) stringToLog = "";
    List<String> logLines = Splitter.fixedLength(MAX_SYMBOLS_PER_LOG_MESSAGE).splitToList(stringToLog);
    LOGGER.info("BEGIN - {} for command {}", result.isSuccessful() ? "stdout" : "stderr", command);
    for (String line : logLines) {
      LOGGER.info("command output *** : {}", line);
    }
    LOGGER.info("END - {} for command {}", result.isSuccessful() ? "stdout" : "stderr", command);
  }


  private String[] assembleCommand(Map<String, String> params) {
    String[] cmdArray = new String[]{
        params.get(UserHookParams.SCRIPT.param()),
        params.get(UserHookParams.CMD_INPUT_FILE.param()),
        params.get(UserHookParams.CLUSTER_SECURITY_TYPE.param()),
        params.get(UserHookParams.CMD_HDFS_PRINCIPAL.param()),
        params.get(UserHookParams.CMD_HDFS_KEYTAB.param()),
        params.get(UserHookParams.CMD_HDFS_USER.param())
    };
    LOGGER.debug("Server action command to be executed: {}", cmdArray);
    return cmdArray;
  }

  /**
   * Validates command parameters, throws exception in case required parameters are missing
   */
  private void validateCommandParams(Map<String, String> commandParams) {

    LOGGER.info("Validating command parameters ...");

    if (!commandParams.containsKey(UserHookParams.PAYLOAD.param())) {
      LOGGER.error("Missing command parameter: {}; Failing the server action.", UserHookParams.PAYLOAD.param());
      throw new IllegalArgumentException("Missing command parameter: [" + UserHookParams.PAYLOAD.param() + "]");
    }

    if (!commandParams.containsKey(UserHookParams.SCRIPT.param())) {
      LOGGER.error("Missing command parameter: {}; Failing the server action.", UserHookParams.SCRIPT.param());
      throw new IllegalArgumentException("Missing command parameter: [" + UserHookParams.SCRIPT.param() + "]");
    }

    if (!commandParams.containsKey(UserHookParams.CMD_INPUT_FILE.param())) {
      LOGGER.error("Missing command parameter: {}; Failing the server action.", UserHookParams.CMD_INPUT_FILE.param());
      throw new IllegalArgumentException("Missing command parameter: [" + UserHookParams.CMD_INPUT_FILE.param() + "]");
    }

    if (!commandParams.containsKey(UserHookParams.CLUSTER_SECURITY_TYPE.param())) {
      LOGGER.error("Missing command parameter: {}; Failing the server action.", UserHookParams.CLUSTER_SECURITY_TYPE.param());
      throw new IllegalArgumentException("Missing command parameter: [" + UserHookParams.CLUSTER_SECURITY_TYPE.param() + "]");
    }

    if (!commandParams.containsKey(UserHookParams.CMD_HDFS_USER.param())) {
      LOGGER.error("Missing command parameter: {}; Failing the server action.", UserHookParams.CMD_HDFS_USER.param());
      throw new IllegalArgumentException("Missing command parameter: [" + UserHookParams.CMD_HDFS_USER.param() + "]");
    }

    LOGGER.info("Command parameter validation passed.");
  }

  private Map<String, List<String>> getPayload(Map<String, String> commandParams) throws IOException {
    Map<String, List<String>> payload = objectMapper.readValue(commandParams.get(UserHookParams.PAYLOAD.param()), Map.class);
    return payload;
  }

}
