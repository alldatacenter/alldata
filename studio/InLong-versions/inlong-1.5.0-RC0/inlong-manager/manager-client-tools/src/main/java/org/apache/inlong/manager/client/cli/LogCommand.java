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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.client.api.inner.client.InlongGroupClient;
import org.apache.inlong.manager.client.cli.pojo.GroupInfo;
import org.apache.inlong.manager.client.cli.util.ClientUtils;
import org.apache.inlong.manager.client.cli.util.PrintUtils;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;

import java.util.List;

/**
 * The log command was used to get log info for specified inlong groups.
 */
@Parameters(commandDescription = "Log resource")
public class LogCommand extends AbstractCommand {

    @Parameter()
    private List<String> params;

    public LogCommand() {
        super("log");
        jcommander.addCommand("group", new CreateGroup());
    }

    @Parameters(commandDescription = "Log group")
    private static class CreateGroup extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"--query"}, required = true, description = "condition filters")
        private String input;

        @Override
        void run() {
            final int MAX_LOG_SIZE = 100;
            try {
                // for now only filter by one condition. TODO:support OR and AND, make a condition filter.
                // sample input: inlongGroupId:test_group
                if (StringUtils.isNotBlank(input)) {
                    System.err.println("input cannot be empty, for example: inlongGroupId:test_group");
                    return;
                }
                String[] inputs = input.split(":");
                if (inputs.length < 2 || StringUtils.isBlank(inputs[1])) {
                    System.err.println("the input must contain ':'");
                    return;
                }

                ClientUtils.initClientFactory();
                InlongGroupClient groupClient = ClientUtils.clientFactory.getGroupClient();
                InlongGroupPageRequest pageRequest = new InlongGroupPageRequest();
                pageRequest.setKeyword(inputs[1]);
                PageResult<InlongGroupBriefInfo> pageResult = groupClient.listGroups(pageRequest);
                if (pageResult.getPageSize() > MAX_LOG_SIZE) {
                    System.err.println("the log is too large to print, please change the filter condition");
                    return;
                }
                PrintUtils.print(pageResult.getList(), GroupInfo.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
