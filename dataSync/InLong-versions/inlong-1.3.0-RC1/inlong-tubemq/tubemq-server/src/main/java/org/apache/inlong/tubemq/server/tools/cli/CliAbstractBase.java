/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.tools.cli;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.inlong.tubemq.server.common.TubeServerVersion;
import org.apache.inlong.tubemq.server.common.fielddef.CliArgDef;

public abstract class CliAbstractBase {

    protected final String commandName;
    protected Options options = new Options();
    protected CommandLineParser parser = new DefaultParser();
    private final HelpFormatter formatter = new HelpFormatter();

    public CliAbstractBase(String commandName) {
        this.commandName = commandName;
        addCommandOption(CliArgDef.HELP);
        addCommandOption(CliArgDef.VERSION);
        formatter.setWidth(500);
    }

    /**
     * Print help information and exit.
     *
     */
    public void help() {
        formatter.printHelp(commandName, options);
        System.exit(0);
    }

    /**
     * Print tubemq server version.
     *
     */
    public void version() {
        System.out.println("TubeMQ " + TubeServerVersion.SERVER_VERSION);
        System.exit(0);
    }

    public void addCommandOption(CliArgDef cliArgDef) {
        Option option = new Option(cliArgDef.opt,
                cliArgDef.longOpt, cliArgDef.hasArg, cliArgDef.optDesc);
        if (cliArgDef.hasArg) {
            option.setArgName(cliArgDef.argDesc);
        }
        options.addOption(option);
    }

    protected abstract void initCommandOptions();

    public abstract boolean processParams(String[] args) throws Exception;

}
