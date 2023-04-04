/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer;

import com.netease.arctic.AmsClient;
import com.netease.arctic.PooledAmsClient;
import com.netease.arctic.ams.api.OptimizeManager;
import com.netease.arctic.ams.api.client.OptimizeManagerClientPools;
import com.netease.arctic.ams.server.maintainer.command.CallCommand;
import com.netease.arctic.ams.server.maintainer.command.CallFactory;
import com.netease.arctic.ams.server.maintainer.command.CommandParser;
import com.netease.arctic.ams.server.maintainer.command.Context;
import com.netease.arctic.ams.server.maintainer.command.DefaultCallFactory;
import com.netease.arctic.ams.server.maintainer.command.SimpleRegexCommandParser;
import com.netease.arctic.ams.server.utils.terminal.TerminalOutput;
import com.netease.arctic.ams.server.utils.terminal.TerminalService;
import com.netease.arctic.catalog.CatalogManager;

public class TableMaintenanceService implements TerminalService {

  private static final String PROMPT_PREFIX = "repair:";

  private static final String PROMPT_SUFFIX = ">";

  private String amsAddress;

  private CommandParser commandParser;

  private Context context;

  public TableMaintenanceService(MaintainerConfig maintainerConfig) {
    this.amsAddress = maintainerConfig.getThriftUrl();

    this.context = new Context();
    if (maintainerConfig.getCatalogName() != null) {
      context.setCatalog(maintainerConfig.getCatalogName());
    }
    CatalogManager catalogManager = new CatalogManager(amsAddress);
    OptimizeManager.Iface client = OptimizeManagerClientPools.getClient(amsAddress);
    AmsClient amsClient = new PooledAmsClient(amsAddress);

    CallFactory callFactory =
        new DefaultCallFactory(maintainerConfig, catalogManager, amsClient);
    this.commandParser = new SimpleRegexCommandParser(callFactory);
  }

  @Override
  public void resolve(String line, TerminalOutput terminalOutput) throws Exception {
    CallCommand callCommand = commandParser.parse(line);
    String result = callCommand.call(context);
    terminalOutput.output(result);
  }

  @Override
  public void close() {

  }

  @Override
  public String welcome() {
    return
            "█████╗ ██████╗  ██████╗████████╗██╗ ██████╗     ███╗   ███╗ █████╗ ██╗███╗   ██╗████████╗ █████╗ ██╗███╗   ██╗███████╗██████╗ \n" +
            "██╔══██╗██╔══██╗██╔════╝╚══██╔══╝██║██╔════╝    ████╗ ████║██╔══██╗██║████╗  ██║╚══██╔══╝██╔══██╗██║████╗  ██║██╔════╝██╔══██╗\n" +
            "███████║██████╔╝██║        ██║   ██║██║         ██╔████╔██║███████║██║██╔██╗ ██║   ██║   ███████║██║██╔██╗ ██║█████╗  ██████╔╝\n" +
            "██╔══██║██╔══██╗██║        ██║   ██║██║         ██║╚██╔╝██║██╔══██║██║██║╚██╗██║   ██║   ██╔══██║██║██║╚██╗██║██╔══╝  ██╔══██╗\n" +
            "██║  ██║██║  ██║╚██████╗   ██║   ██║╚██████╗    ██║ ╚═╝ ██║██║  ██║██║██║ ╚████║   ██║   ██║  ██║██║██║ ╚████║███████╗██║  ██║\n" +
            "╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝   ╚═╝   ╚═╝ ╚═════╝    ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝   ╚═╝   ╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝\n" +
            "\n" +
            "Please enter 'help' for usage instructions!";
  }

  @Override
  public String[] keyWord() {
    return commandParser.keywords();
  }

  /**
   * Like Repair:{catalog}.{db}>
   */
  @Override
  public String prompt() {
    if (context.getCatalog() == null) {
      return PROMPT_PREFIX + PROMPT_SUFFIX;
    }
    if (context.getDb() == null) {
      return PROMPT_PREFIX + context.getCatalog() + PROMPT_SUFFIX;
    }
    return PROMPT_PREFIX + context.getCatalog() + "." + context.getDb() + PROMPT_SUFFIX;
  }
}
