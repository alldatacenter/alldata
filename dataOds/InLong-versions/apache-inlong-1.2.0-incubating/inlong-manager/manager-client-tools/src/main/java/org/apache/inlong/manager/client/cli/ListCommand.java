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
import org.apache.inlong.manager.client.api.enums.SimpleGroupStatus;
import org.apache.inlong.manager.client.api.impl.InlongClientImpl;
import org.apache.inlong.manager.client.api.inner.InnerInlongManagerClient;
import org.apache.inlong.manager.client.cli.pojo.GroupInfo;
import org.apache.inlong.manager.client.cli.pojo.SinkInfo;
import org.apache.inlong.manager.client.cli.pojo.SourceInfo;
import org.apache.inlong.manager.client.cli.pojo.StreamInfo;
import org.apache.inlong.manager.client.cli.util.ClientUtils;
import org.apache.inlong.manager.client.cli.util.PrintUtils;
import org.apache.inlong.manager.common.pojo.group.InlongGroupListResponse;
import org.apache.inlong.manager.common.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.common.pojo.sink.SinkListResponse;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.pojo.stream.FullStreamResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Get main information of resources.
 */
@Parameters(commandDescription = "Displays summary information about one or more resources")
public class ListCommand extends AbstractCommand {

    @Parameter()
    private List<String> params;

    public ListCommand() {
        super("list");
        InlongClientImpl inlongClient;
        try {
            inlongClient = ClientUtils.getClient();
        } catch (IOException e) {
            System.err.println("get inlong client error");
            System.err.println(e.getMessage());
            return;
        }

        InnerInlongManagerClient managerClient = new InnerInlongManagerClient(inlongClient.getConfiguration());

        jcommander.addCommand("stream", new ListStream(managerClient));
        jcommander.addCommand("group", new ListGroup(managerClient));
        jcommander.addCommand("sink", new ListSink(managerClient));
        jcommander.addCommand("source", new ListSource(managerClient));
    }

    @Parameters(commandDescription = "Get stream summary information")
    private static class ListStream extends AbstractCommandRunner {

        private final InnerInlongManagerClient managerClient;

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String groupId;

        ListStream(InnerInlongManagerClient managerClient) {
            this.managerClient = managerClient;
        }

        @Override
        void run() {
            try {
                List<FullStreamResponse> streamResponseList = managerClient.listStreamInfo(groupId);
                List<InlongStreamInfo> streamInfoList = streamResponseList.stream()
                        .map(FullStreamResponse::getStreamInfo)
                        .collect(Collectors.toList());
                PrintUtils.print(streamInfoList, StreamInfo.class);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get group summary information")
    private static class ListGroup extends AbstractCommandRunner {

        private static final int DEFAULT_PAGE_SIZE = 10;

        private final InnerInlongManagerClient managerClient;

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-s", "--status"}, description = "inlong group status")
        private String status;

        @Parameter(names = {"-g", "--group"}, description = "inlong group id")
        private String group;

        @Parameter(names = {"-n", "--num"}, description = "the number displayed")
        private int pageSize;

        ListGroup(InnerInlongManagerClient managerClient) {
            this.managerClient = managerClient;
        }

        @Override
        void run() {
            try {
                InlongGroupPageRequest pageRequest = new InlongGroupPageRequest();
                pageRequest.setKeyword(group);
                // set default page size to DEFAULT_PAGE_SIZE
                pageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
                pageRequest.setPageNum(1).setPageSize(pageSize);

                // set default status to STARTED
                status = status == null ? SimpleGroupStatus.STARTED.toString() : status;
                List<Integer> statusList = SimpleGroupStatus.parseStatusCodeByStr(status);
                pageRequest.setStatusList(statusList);

                PageInfo<InlongGroupListResponse> groupPageInfo = managerClient.listGroups(pageRequest);
                PrintUtils.print(groupPageInfo.getList(), GroupInfo.class);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get sink summary information")
    private static class ListSink extends AbstractCommandRunner {

        private final InnerInlongManagerClient managerClient;

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-s", "--stream"}, required = true, description = "stream id")
        private String stream;

        @Parameter(names = {"-g", "--group"}, required = true, description = "group id")
        private String group;

        ListSink(InnerInlongManagerClient managerClient) {
            this.managerClient = managerClient;
        }

        @Override
        void run() {
            try {
                List<SinkListResponse> sinkListResponses = managerClient.listSinks(group, stream);
                PrintUtils.print(sinkListResponses, SinkInfo.class);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get source summary information")
    private static class ListSource extends AbstractCommandRunner {

        private final InnerInlongManagerClient managerClient;

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-s", "--stream"}, required = true, description = "inlong stream id")
        private String stream;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String group;

        @Parameter(names = {"-t", "--type"}, description = "sink type")
        private String type;

        ListSource(InnerInlongManagerClient managerClient) {
            this.managerClient = managerClient;
        }

        @Override
        void run() {
            try {
                List<SourceListResponse> sourceListResponses = managerClient.listSources(group, stream, type);
                PrintUtils.print(sourceListResponses, SourceInfo.class);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
