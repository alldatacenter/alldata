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
import org.apache.inlong.manager.client.api.inner.client.InlongClusterClient;
import org.apache.inlong.manager.client.api.inner.client.InlongGroupClient;
import org.apache.inlong.manager.client.api.inner.client.InlongStreamClient;
import org.apache.inlong.manager.client.api.inner.client.StreamSinkClient;
import org.apache.inlong.manager.client.api.inner.client.StreamSourceClient;
import org.apache.inlong.manager.client.api.inner.client.StreamTransformClient;
import org.apache.inlong.manager.client.api.inner.client.UserClient;
import org.apache.inlong.manager.client.cli.util.ClientUtils;
import org.apache.inlong.manager.client.cli.util.PrintUtils;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeResponse;
import org.apache.inlong.manager.pojo.cluster.ClusterTagResponse;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.transform.TransformResponse;
import org.apache.inlong.manager.pojo.user.UserInfo;

import java.util.List;

/**
 * Describe the info of resources.
 */
@Parameters(commandDescription = "Display details of one resource")
public class DescribeCommand extends AbstractCommand {

    @Parameter()
    private List<String> params;

    public DescribeCommand() {
        super("describe");

        jcommander.addCommand("stream", new DescribeStream());
        jcommander.addCommand("group", new DescribeGroup());
        jcommander.addCommand("sink", new DescribeSink());
        jcommander.addCommand("source", new DescribeSource());
        jcommander.addCommand("transform", new DescribeTransform());
        jcommander.addCommand("cluster", new DescribeCluster());
        jcommander.addCommand("cluster-tag", new DescribeClusterTag());
        jcommander.addCommand("cluster-node", new DescribeClusterNode());
        jcommander.addCommand("user", new DescribeUser());
    }

    @Parameters(commandDescription = "Get stream details")
    private static class DescribeStream extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String groupId;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                InlongStreamClient streamClient = ClientUtils.clientFactory.getStreamClient();
                List<InlongStreamInfo> streamInfos = streamClient.listStreamInfo(groupId);
                streamInfos.forEach(PrintUtils::printJson);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get group details")
    private static class DescribeGroup extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-s", "--status"}, description = "inlong group status")
        private int status;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String group;

        @Parameter(names = {"-n", "--num"}, description = "the number displayed")
        private int pageSize;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                InlongGroupClient groupClient = ClientUtils.clientFactory.getGroupClient();
                InlongGroupPageRequest pageRequest = new InlongGroupPageRequest();
                pageRequest.setKeyword(group);
                PageResult<InlongGroupBriefInfo> pageInfo = groupClient.listGroups(pageRequest);
                pageInfo.getList().forEach(PrintUtils::printJson);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get sink details")
    private static class DescribeSink extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-s", "--stream"}, required = true, description = "inlong stream id")
        private String stream;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String group;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                StreamSinkClient sinkClient = ClientUtils.clientFactory.getSinkClient();
                List<StreamSink> streamSinks = sinkClient.listSinks(group, stream);
                streamSinks.forEach(PrintUtils::printJson);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get source details")
    private static class DescribeSource extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-s", "--stream"}, required = true, description = "inlong stream id")
        private String stream;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String group;

        @Parameter(names = {"-t", "--type"}, description = "sink type")
        private String type;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                StreamSourceClient sourceClient = ClientUtils.clientFactory.getSourceClient();
                List<StreamSource> sources = sourceClient.listSources(group, stream, type);
                sources.forEach(PrintUtils::printJson);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get transform details")
    private static class DescribeTransform extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-g", "--group"}, required = true, description = "inlong group id")
        private String groupId;

        @Parameter(names = {"-s", "--stream"}, required = true, description = "inlong stream id")
        private String streamId;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                StreamTransformClient transformClient = ClientUtils.clientFactory.getTransformClient();
                List<TransformResponse> transforms = transformClient.listTransform(groupId, streamId);
                transforms.forEach(PrintUtils::printJson);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get cluster details")
    private static class DescribeCluster extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-id", "--id"}, required = true, description = "cluster id")
        private int clusterId;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                InlongClusterClient clusterClient = ClientUtils.clientFactory.getClusterClient();
                ClusterInfo clusterInfo = clusterClient.get(clusterId);
                PrintUtils.printJson(clusterInfo);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get cluster tag details")
    private static class DescribeClusterTag extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-id", "--id"}, required = true, description = "cluster tag id")
        private int tagId;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                InlongClusterClient clusterClient = ClientUtils.clientFactory.getClusterClient();
                ClusterTagResponse tagInfo = clusterClient.getTag(tagId);
                PrintUtils.printJson(tagInfo);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get cluster node details")
    private static class DescribeClusterNode extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-id", "--id"}, required = true, description = "cluster node id")
        private int nodeId;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                InlongClusterClient clusterClient = ClientUtils.clientFactory.getClusterClient();
                ClusterNodeResponse nodeInfo = clusterClient.getNode(nodeId);
                PrintUtils.printJson(nodeInfo);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Get user details")
    private static class DescribeUser extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-id", "--id"}, required = true, description = "user id")
        private int userId;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                UserClient userClient = ClientUtils.clientFactory.getUserClient();
                UserInfo userInfo = userClient.getById(userId);
                PrintUtils.printJson(userInfo);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
