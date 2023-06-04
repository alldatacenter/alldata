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
import com.beust.jcommander.converters.FileConverter;
import org.apache.inlong.manager.client.api.InlongClient;
import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.api.InlongGroupContext;
import org.apache.inlong.manager.client.api.InlongStreamBuilder;
import org.apache.inlong.manager.client.api.inner.client.InlongClusterClient;
import org.apache.inlong.manager.client.api.inner.client.UserClient;
import org.apache.inlong.manager.client.cli.pojo.CreateGroupConf;
import org.apache.inlong.manager.client.cli.util.ClientUtils;
import org.apache.inlong.manager.client.cli.validator.UserTypeValidator;
import org.apache.inlong.manager.common.enums.DataFormat;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagRequest;
import org.apache.inlong.manager.pojo.sort.FlinkSortConf;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.user.UserRequest;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.apache.inlong.manager.client.cli.consts.GroupConstants.DEFAULT_DATA_ENCODING;
import static org.apache.inlong.manager.client.cli.consts.GroupConstants.DEFAULT_DATA_SEPARATOR;
import static org.apache.inlong.manager.client.cli.consts.GroupConstants.DEFAULT_IGNORE_PARSE_ERROR;
import static org.apache.inlong.manager.client.cli.consts.GroupConstants.DEFAULT_LIGHTWEIGHT;
import static org.apache.inlong.manager.common.consts.InlongConstants.ADMIN_USER;

/**
 * Create resource by json file.
 */
@Parameters(commandDescription = "Create resource by json file")
public class CreateCommand extends AbstractCommand {

    @Parameter()
    private List<String> params;

    public CreateCommand() {
        super("create");
        jcommander.addCommand("group", new CreateGroup());
        jcommander.addCommand("cluster", new CreateCluster());
        jcommander.addCommand("cluster-tag", new CreateClusterTag());
        jcommander.addCommand("cluster-node", new CreateClusterNode());
        jcommander.addCommand("user", new CreateUser());
    }

    @Parameters(commandDescription = "Create group by json file")
    private static class CreateGroup extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-f", "--file"}, converter = FileConverter.class, description = "json file")
        private File file;

        @Override
        void run() {
            try {
                String content = ClientUtils.readFile(file);

                // first extract group config from the file passed in
                CreateGroupConf groupConf = JsonUtils.parseObject(content, CreateGroupConf.class);
                assert groupConf != null;
                setDefaultConfigInfo(groupConf);

                // get the corresponding inlong group, aka the task to execute
                InlongClient inlongClient = ClientUtils.getClient();
                InlongGroup group = inlongClient.forGroup(groupConf.getGroupInfo());
                InlongStreamBuilder streamBuilder = group.createStream(groupConf.getStreamInfo());
                // put in parameters:source and sink,stream fields, then initialize
                streamBuilder.fields(groupConf.getStreamInfo().getFieldList());
                groupConf.getStreamInfo().getSourceList().forEach(streamBuilder::source);
                groupConf.getStreamInfo().getSinkList().forEach(streamBuilder::sink);
                if (groupConf.getStreamTransform() != null) {
                    streamBuilder.transform(groupConf.getStreamTransform());
                }
                streamBuilder.initOrUpdate();
                // initialize the new stream group
                InlongGroupContext context = group.init();
                System.out.println("Create group success, current status: " + context.getStatus());
            } catch (Exception e) {
                System.out.println("Create group failed!");
                System.out.println(e.getMessage());
            }
        }

        /**
         * Set default value for group conf
         *
         * @param groupConf group conf
         */
        private void setDefaultConfigInfo(CreateGroupConf groupConf) {
            String inlongGroupId = groupConf.getGroupInfo().getInlongGroupId();
            String inlongStreamId = groupConf.getStreamInfo().getInlongStreamId();
            // group
            groupConf.getGroupInfo().setInCharges(ADMIN_USER);
            groupConf.getGroupInfo().setLightweight(DEFAULT_LIGHTWEIGHT);
            groupConf.getGroupInfo().setSortConf(new FlinkSortConf());

            // stream
            InlongStreamInfo streamInfo = groupConf.getStreamInfo();
            groupConf.getStreamInfo().setInlongGroupId(inlongGroupId);
            streamInfo.setDataType(Optional.ofNullable(streamInfo.getDataType()).orElse(DataFormat.CSV.getName()));
            streamInfo.setDataEncoding(Optional.ofNullable(streamInfo.getDataEncoding()).orElse(DEFAULT_DATA_ENCODING));
            streamInfo.setDataSeparator(
                    Optional.ofNullable(streamInfo.getDataSeparator()).orElse(DEFAULT_DATA_SEPARATOR));
            streamInfo.setIgnoreParseError(
                    Optional.ofNullable(streamInfo.getIgnoreParseError()).orElse(DEFAULT_IGNORE_PARSE_ERROR));

            // field
            streamInfo.getFieldList().forEach(field -> {
                field.setInlongGroupId(inlongGroupId);
                field.setInlongStreamId(inlongStreamId);
            });

            // source
            streamInfo.getSourceList().forEach(source -> {
                source.setInlongGroupId(inlongGroupId);
                source.setInlongStreamId(inlongStreamId);
            });

            // sink
            streamInfo.getSinkList().forEach(sink -> {
                sink.setInlongGroupId(inlongGroupId);
                sink.setInlongStreamId(inlongStreamId);
            });
        }
    }

    @Parameters(commandDescription = "Create cluster by json file")
    private static class CreateCluster extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-f", "--file"}, description = "json file", converter = FileConverter.class)
        private File file;

        @Override
        void run() {
            try {
                String content = ClientUtils.readFile(file);
                ClusterRequest request = JsonUtils.parseObject(content, ClusterRequest.class);
                ClientUtils.initClientFactory();
                InlongClusterClient clusterClient = ClientUtils.clientFactory.getClusterClient();
                assert request != null;
                Integer clusterId = clusterClient.saveCluster(request);
                if (clusterId != null) {
                    System.out.println("Create cluster success! ID: " + clusterId);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Create cluster tag by json file")
    private static class CreateClusterTag extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-f", "--file"}, description = "json file", converter = FileConverter.class)
        private File file;

        @Override
        void run() {
            try {
                String content = ClientUtils.readFile(file);
                ClusterTagRequest request = JsonUtils.parseObject(content, ClusterTagRequest.class);
                ClientUtils.initClientFactory();
                InlongClusterClient clusterClient = ClientUtils.clientFactory.getClusterClient();
                Integer tagId = clusterClient.saveTag(request);
                if (tagId != null) {
                    System.out.println("Create cluster tag success! ID: " + tagId);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Create cluster node by json file")
    private static class CreateClusterNode extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-f", "--file"}, description = "json file", converter = FileConverter.class)
        private File file;

        @Override
        void run() {
            try {
                String content = ClientUtils.readFile(file);
                ClusterNodeRequest request = JsonUtils.parseObject(content, ClusterNodeRequest.class);
                ClientUtils.initClientFactory();
                InlongClusterClient clusterClient = ClientUtils.clientFactory.getClusterClient();
                Integer nodeId = clusterClient.saveNode(request);
                if (nodeId != null) {
                    System.out.println("Create cluster node success! ID: " + nodeId);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Create user")
    private static class CreateUser extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-u", "--username"}, description = "username")
        private String username;

        @Parameter(names = {"-p", "--password"}, description = "password")
        private String password;

        @Parameter(names = {"-t", "--type"}, description = "account type", validateWith = UserTypeValidator.class)
        private String type;

        @Parameter(names = {"-d", "--days"}, description = "valid days")
        private Integer validDays;

        @Override
        void run() {
            try {
                UserRequest request = new UserRequest();
                request.setName(username);
                request.setPassword(password);
                request.setAccountType(UserTypeEnum.parseName(type));
                request.setValidDays(validDays);
                ClientUtils.initClientFactory();
                UserClient userClient = ClientUtils.clientFactory.getUserClient();
                Integer userId = userClient.register(request);
                if (userId != null) {
                    System.out.println("Create user success! ID: " + userId);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
