/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.client.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Class for parse command.
 */
public abstract class AbstractCommand {

    protected final JCommander jcommander;

    @Parameter(names = {"-h", "--help"}, help = true, hidden = true)
    private boolean help;

    public AbstractCommand(String cmdName) {
        jcommander = new JCommander();
        jcommander.setProgramName("managerctl " + cmdName);
    }

    public boolean run(String[] args) {

        if (help) {
            jcommander.usage();
            return true;
        }

        try {
            jcommander.parse(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            jcommander.usage();
            return false;
        }

        String cmd = jcommander.getParsedCommand();
        if (cmd == null) {
            jcommander.usage();
            return false;
        } else {
            JCommander obj = jcommander.getCommands().get(cmd);
            AbstractCommandRunner commandRunner = (AbstractCommandRunner) obj.getObjects().get(0);
            try {
                commandRunner.run();
                return true;
            } catch (ParameterException e) {
                System.err.println(e.getMessage() + System.lineSeparator());
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
