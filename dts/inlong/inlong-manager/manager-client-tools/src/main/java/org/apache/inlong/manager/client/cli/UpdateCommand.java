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
import org.apache.inlong.manager.client.api.inner.client.InlongClusterClient;
import org.apache.inlong.manager.client.api.inner.client.InlongTenantClient;
import org.apache.inlong.manager.client.api.inner.client.InlongTenantRoleClient;
import org.apache.inlong.manager.client.api.inner.client.UserClient;
import org.apache.inlong.manager.client.cli.util.ClientUtils;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagRequest;
import org.apache.inlong.manager.pojo.sort.BaseSortConf;
import org.apache.inlong.manager.pojo.tenant.InlongTenantInfo;
import org.apache.inlong.manager.pojo.tenant.InlongTenantRequest;
import org.apache.inlong.manager.pojo.user.TenantRoleInfo;
import org.apache.inlong.manager.pojo.user.TenantRoleRequest;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserRequest;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

/**
 * The update command used to change the fields of inlong groups.
 * Please refer to the document for parameters
 */
@Parameters(commandDescription = "Update resource by json file")
public class UpdateCommand extends AbstractCommand {

    @Parameter()
    private List<String> params;

    public UpdateCommand() {
        super("update");
        jcommander.addCommand("group", new UpdateCommand.UpdateGroup());
        jcommander.addCommand("cluster", new UpdateCommand.UpdateCluster());
        jcommander.addCommand("cluster-tag", new UpdateCommand.UpdateClusterTag());
        jcommander.addCommand("cluster-node", new UpdateCommand.UpdateClusterNode());
        jcommander.addCommand("user", new UpdateCommand.UpdateUser());
        jcommander.addCommand("tenant", new UpdateCommand.UpdateTenant());
        jcommander.addCommand("tenant-role", new UpdateCommand.UpdateTenantRole());
    }

    @Parameters(commandDescription = "Update group by json file")
    private static class UpdateGroup extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"--group", "-g"}, required = true, description = "inlong group id")
        private String inlongGroupId;

        @Parameter(names = {"-c", "--config"}, required = true, description = "json file")
        private File file;

        @Override
        void run() {
            try {
                InlongClient inlongClient = ClientUtils.getClient();
                InlongGroup group = inlongClient.getGroup(inlongGroupId);
                String fileContent = ClientUtils.readFile(file);
                if (StringUtils.isBlank(fileContent)) {
                    System.out.println("Update group failed: file was empty!");
                    return;
                }
                // first extract group config from the file passed in
                BaseSortConf sortConf = JsonUtils.parseObject(fileContent, BaseSortConf.class);
                group.update(sortConf);
                System.out.println("Update group success!");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Update cluster by json file")
    private static class UpdateCluster extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-f", "--file"}, description = "json file", converter = FileConverter.class)
        private File file;

        @Override
        void run() {
            try {
                String content = ClientUtils.readFile(file);
                ClusterRequest request = JsonUtils.parseObject(content, ClusterRequest.class);
                assert request != null;
                ClientUtils.initClientFactory();
                InlongClusterClient clusterClient = ClientUtils.clientFactory.getClusterClient();
                if (clusterClient.update(request)) {
                    System.out.println("Update cluster success!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Update cluster tag by json file")
    private static class UpdateClusterTag extends AbstractCommandRunner {

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
                if (clusterClient.updateTag(request)) {
                    System.out.println("Update cluster tag success!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Update cluster node by json file")
    private static class UpdateClusterNode extends AbstractCommandRunner {

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
                if (clusterClient.updateNode(request)) {
                    System.out.println("Update cluster node success!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Update User")
    private static class UpdateUser extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-u", "--username"}, description = "username to be modify")
        private String username;

        @Parameter(names = {"-p", "--password"}, description = "new password")
        private String password;

        @Parameter(names = {"-d", "--days"}, description = "new valid days")
        private Integer validDays;

        @Override
        void run() {
            try {
                UserRequest request = new UserRequest();
                request.setName(username);
                ClientUtils.initClientFactory();
                UserClient userClient = ClientUtils.clientFactory.getUserClient();
                UserInfo userInfo = userClient.getByName(username);
                if (userInfo == null) {
                    throw new BusinessException(username + " not exist, please check.");
                }
                request.setId(userInfo.getId());
                request.setNewPassword(password);
                request.setAccountType(userInfo.getAccountType());
                request.setValidDays(validDays);
                request.setVersion(userInfo.getVersion());
                if (userClient.update(request) != null) {
                    System.out.println("Update user success!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Update Tenant")
    private static class UpdateTenant extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-n", "--name"}, description = "name to be modify")
        private String name;

        @Parameter(names = {"-d", "--description"}, description = "new description")
        private String description;

        @Override
        void run() {
            try {
                InlongTenantRequest request = new InlongTenantRequest();
                request.setName(name);
                ClientUtils.initClientFactory();
                InlongTenantClient tenantClient = ClientUtils.clientFactory.getInlongTenantClient();
                InlongTenantInfo tenantInfo = tenantClient.getTenantByName(name);
                if (tenantInfo == null) {
                    throw new BusinessException(name + " not exist, please check.");
                }
                request.setId(tenantInfo.getId());
                request.setDescription(description);
                request.setVersion(tenantInfo.getVersion());
                if (tenantClient.update(request)) {
                    System.out.println("Update user success!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Parameters(commandDescription = "Update Tenant Role")
    private static class UpdateTenantRole extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"-id", "--id"}, description = "id to be modify")
        private Integer id;

        @Parameter(names = {"-rc", "--role-code"}, description = "new role code")
        private String roleCode;

        @Override
        void run() {
            try {
                TenantRoleRequest request = new TenantRoleRequest();
                request.setId(id);
                ClientUtils.initClientFactory();
                InlongTenantRoleClient roleClient = ClientUtils.clientFactory.getInlongTenantRoleClient();
                TenantRoleInfo roleInfo = roleClient.get(id);
                if (roleInfo == null) {
                    throw new BusinessException(id + " not exist, please check.");
                }
                request.setUsername(roleInfo.getUsername());
                request.setDisabled(roleInfo.getDisabled());
                request.setVersion(roleInfo.getVersion());
                if (StringUtils.isNotEmpty(roleCode)) {
                    request.setRoleCode(roleCode);
                } else {
                    request.setRoleCode(roleInfo.getRoleCode());
                }
                request.setTenant(roleInfo.getTenant());
                if (roleClient.update(request)) {
                    System.out.println("Update user success!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
