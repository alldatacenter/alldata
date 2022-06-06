/**
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

package org.apache.atlas.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class CommandHandlerUtility {
    public static final Logger LOG = LoggerFactory.getLogger(CommandHandlerUtility.class);

    private static final String SHELL_CMD                   = "/bin/sh";
    private static final String SHELL_CMD_OPTION            = "-c";
    private static final String FIND_PROCESS_ID_CMD_FORMAT  = "lsof -i:%s | tail -n 1 | tr -s ' ' | cut -d' ' -f2";
    private static final String KILL_PROCESS_CMD_FORMAT     = "kill %s %s" ;
    private static final int    SLEEP_AFTER_SOFT_KILL_IN_MS = 4000;

    public static void tryKillingProcessUsingPort(int port, boolean forceKill) {
        String processID = findProcessIdUsingPort(port);
        sendKillToPID(processID, forceKill);
    }

    private static String findProcessIdUsingPort(int port) {
        String         retPID = "";

        final String[] cmd = {
                SHELL_CMD,
                SHELL_CMD_OPTION,
                String.format(FIND_PROCESS_ID_CMD_FORMAT, port)
        };

        try {
            Process p = Runtime.getRuntime().exec(cmd);
            retPID = new BufferedReader(new InputStreamReader(p.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));

            if (StringUtils.isEmpty(retPID)) {
                String errorMsg = new BufferedReader(new InputStreamReader(p.getErrorStream()))
                        .lines().collect(Collectors.joining("\n"));
                throw new IOException(errorMsg);
            }
        } catch (IOException e) {
            LOG.warn("Failed to get process ID which uses the port{}", port, e);
        }

        return retPID;
    }

    private static void sendKillToPID(String pid, boolean forceKill) {
        if (StringUtils.isBlank(pid)) {
            return;
        }

        final String cmd = String.format(KILL_PROCESS_CMD_FORMAT, (forceKill ? "-9 " : ""), pid);

        try {
            Runtime.getRuntime().exec(cmd);

            if (!forceKill) {
                LOG.info("Sleeping for {} milliseconds after soft kill", SLEEP_AFTER_SOFT_KILL_IN_MS);
                Thread.sleep(SLEEP_AFTER_SOFT_KILL_IN_MS);
            }
        } catch (IOException | InterruptedException e) {
            LOG.warn("Failed to kill the process {} which uses the port with hard kill flag{}", pid, forceKill, e);
        }
    }
}
