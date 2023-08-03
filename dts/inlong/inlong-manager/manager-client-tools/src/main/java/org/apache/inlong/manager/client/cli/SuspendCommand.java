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

import org.apache.inlong.manager.client.api.InlongClient;
import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.api.InlongGroupContext;
import org.apache.inlong.manager.client.cli.util.ClientUtils;
import org.apache.inlong.manager.common.enums.SimpleGroupStatus;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.List;

/**
 * Suspend inlong group
 */
@Parameters(commandDescription = "Suspend resource by group id")
public class SuspendCommand extends AbstractCommand {

    @Parameter()
    private List<String> params;

    public SuspendCommand() {
        super("suspend");
        jcommander.addCommand("group", new SuspendCommand.SuspendGroup());
    }

    @Parameters(commandDescription = "Suspend the inlong group task")
    private static class SuspendGroup extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"--group", "-g"}, required = true, description = "inlong group id")
        private String inlongGroupId;

        @Override
        void run() {
            try {
                InlongClient inlongClient = ClientUtils.getClient();
                InlongGroup group = inlongClient.getGroup(inlongGroupId);
                InlongGroupContext context = group.suspend();
                if (!SimpleGroupStatus.STOPPED.equals(context.getStatus())) {
                    throw new Exception("Suspend group failed, current status: " + context.getStatus());
                }
                System.out.println("Suspend group success!");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
