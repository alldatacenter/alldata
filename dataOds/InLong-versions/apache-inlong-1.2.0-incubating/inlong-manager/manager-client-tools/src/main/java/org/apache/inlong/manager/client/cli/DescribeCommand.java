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
import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.client.api.impl.InlongClientImpl;
import org.apache.inlong.manager.client.api.inner.InnerInlongManagerClient;
import org.apache.inlong.manager.client.cli.pojo.GroupInfo;
import org.apache.inlong.manager.client.cli.util.ClientUtils;
import org.apache.inlong.manager.client.cli.util.PrintUtils;
import org.apache.inlong.manager.common.pojo.group.InlongGroupListResponse;
import org.apache.inlong.manager.common.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.common.pojo.sink.SinkListResponse;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.pojo.stream.FullStreamResponse;

import java.io.IOException;
import java.util.List;

/**
 * Describe the info of resources.
 */
@Parameters(commandDescription = "Display details of one resource")
public class DescribeCommand extends AbstractCommand {

    @Parameter()
    private java.util.List<String> params;

    public DescribeCommand() {
        super("describe");
        InlongClientImpl inlongClient;
        try {
            inlongClient = ClientUtils.getClient();
        } catch (IOException e) {
            System.err.println("get inlong client error");
            System.err.println(e.getMessage());
            return;
        }
        InnerInlongManagerClient managerClient = new InnerInlongManagerClient(inlongClient.getConfiguration());

        jcommander.addCommand("stream", new DescribeStream(managerClient));
        jcommander.addCommand("group", new DescribeGroup(managerClient));
        jcommander.addCommand("sink", new DescribeSink(managerClient));
        jcommander.addCommand("source", new DescribeSource(managerClient));
    }

    @Parameters(commandDescription = "Get stream details")
    private static class DescribeStream extends AbstractCommandRunner {

        private final InnerInlongManagerClient managerClient;

        @Parameter()
        private java.util.List<String> params;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String groupId;

        DescribeStream(InnerInlongManagerClient managerClient) {
            this.managerClient = managerClient;
        }

        @Override
        void run() {
            try {
                List<FullStreamResponse> fullStreamResponseList = managerClient.listStreamInfo(groupId);
                fullStreamResponseList.forEach(response -> PrintUtils.printJson(response.getStreamInfo()));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get group details")
    private static class DescribeGroup extends AbstractCommandRunner {

        private final InnerInlongManagerClient managerClient;

        @Parameter()
        private java.util.List<String> params;

        @Parameter(names = {"-s", "--status"}, description = "inlong group status")
        private int status;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String group;

        @Parameter(names = {"-n", "--num"}, description = "the number displayed")
        private int pageSize;

        DescribeGroup(InnerInlongManagerClient managerClient) {
            this.managerClient = managerClient;
        }

        @Override
        void run() {
            try {
                InlongGroupPageRequest pageRequest = new InlongGroupPageRequest();
                pageRequest.setKeyword(group);
                PageInfo<InlongGroupListResponse> pageInfo = managerClient.listGroups(pageRequest);
                PrintUtils.print(pageInfo.getList(), GroupInfo.class);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get sink details")
    private static class DescribeSink extends AbstractCommandRunner {

        private final InnerInlongManagerClient managerClient;

        @Parameter()
        private java.util.List<String> params;

        @Parameter(names = {"-s", "--stream"}, required = true, description = "inlong stream id")
        private String stream;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String group;

        DescribeSink(InnerInlongManagerClient managerClient) {
            this.managerClient = managerClient;
        }

        @Override
        void run() {
            try {
                List<SinkListResponse> sinkListResponses = managerClient.listSinks(group, stream);
                sinkListResponses.forEach(PrintUtils::printJson);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get source details")
    private static class DescribeSource extends AbstractCommandRunner {

        private final InnerInlongManagerClient managerClient;

        @Parameter()
        private java.util.List<String> params;

        @Parameter(names = {"-s", "--stream"}, required = true, description = "inlong stream id")
        private String stream;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String group;

        @Parameter(names = {"-t", "--type"}, description = "sink type")
        private String type;

        DescribeSource(InnerInlongManagerClient managerClient) {
            this.managerClient = managerClient;
        }

        @Override
        void run() {
            try {
                List<SourceListResponse> sourceListResponses = managerClient.listSources(group, stream, type);
                sourceListResponses.forEach(PrintUtils::printJson);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
