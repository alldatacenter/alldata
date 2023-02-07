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
import org.apache.inlong.manager.client.api.InlongClient;
import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.api.inner.client.InlongClusterClient;
import org.apache.inlong.manager.client.api.inner.client.UserClient;
import org.apache.inlong.manager.client.cli.util.ClientUtils;

import java.util.List;

/**
 * The delete command used for deleting inlong group instances.
 * Please refer to the document for parameters
 */
@Parameters(commandDescription = "Delete resource by json file")
public class DeleteCommand extends AbstractCommand {

    @Parameter()
    private List<String> params;

    public DeleteCommand() {
        super("delete");
        jcommander.addCommand("group", new DeleteGroup());
        jcommander.addCommand("cluster", new DeleteCluster());
        jcommander.addCommand("cluster-tag", new DeleteClusterTag());
        jcommander.addCommand("cluster-node", new DeleteClusterNode());
        jcommander.addCommand("user", new DeleteUser());
    }

    @Parameters(commandDescription = "Delete group by group id")
    private static class DeleteGroup extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"--group", "-g"}, required = true, description = "inlong group id")
        private String inlongGroupId;

        @Override
        void run() {
            // get the group and the corresponding context(snapshot)
            // TODO: handle and/or classify the exceptions
            try {
                InlongClient inlongClient = ClientUtils.getClient();
                InlongGroup group = inlongClient.getGroup(inlongGroupId);
                group.delete();
                System.out.println("Delete group success!");
            } catch (Exception e) {
                System.out.format("Delete group failed! message: %s \n", e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Delete cluster by cluster id")
    private static class DeleteCluster extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-id", "--id"}, required = true, description = "cluster id")
        private int clusterId;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                InlongClusterClient clusterClient = ClientUtils.clientFactory.getClusterClient();
                if (clusterClient.delete(clusterId)) {
                    System.out.println("Delete cluster success!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Delete cluster tag by tag id")
    private static class DeleteClusterTag extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-id", "--id"}, required = true, description = "cluster tag id")
        private int tagId;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                InlongClusterClient clusterClient = ClientUtils.clientFactory.getClusterClient();
                if (clusterClient.deleteTag(tagId)) {
                    System.out.println("Delete cluster tag success!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Delete cluster node by node id")
    private static class DeleteClusterNode extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-id", "--id"}, required = true, description = "cluster node id")
        private int nodeId;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                InlongClusterClient clusterClient = ClientUtils.clientFactory.getClusterClient();
                if (clusterClient.deleteNode(nodeId)) {
                    System.out.println("Delete cluster node success!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Delete user by user id")
    private static class DeleteUser extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-id", "--id"}, required = true, description = "user id")
        private int userId;

        @Override
        void run() {
            try {
                ClientUtils.initClientFactory();
                UserClient userClient = ClientUtils.clientFactory.getUserClient();
                if (userClient.delete(userId)) {
                    System.out.println("Delete user success!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
