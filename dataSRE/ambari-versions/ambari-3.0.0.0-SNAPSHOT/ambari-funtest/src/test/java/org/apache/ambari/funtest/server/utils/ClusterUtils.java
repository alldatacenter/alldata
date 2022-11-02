/*
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

package org.apache.ambari.funtest.server.utils;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.funtest.server.AmbariUserRole;
import org.apache.ambari.funtest.server.ClusterConfigParams;
import org.apache.ambari.funtest.server.ConnectionParams;
import org.apache.ambari.funtest.server.WebRequest;
import org.apache.ambari.funtest.server.WebResponse;
import org.apache.ambari.funtest.server.api.cluster.AddDesiredConfigurationWebRequest;
import org.apache.ambari.funtest.server.api.cluster.CreateClusterWebRequest;
import org.apache.ambari.funtest.server.api.cluster.CreateConfigurationWebRequest;
import org.apache.ambari.funtest.server.api.cluster.GetRequestStatusWebRequest;
import org.apache.ambari.funtest.server.api.cluster.SetUserPrivilegeWebRequest;
import org.apache.ambari.funtest.server.api.host.AddHostWebRequest;
import org.apache.ambari.funtest.server.api.host.RegisterHostWebRequest;
import org.apache.ambari.funtest.server.api.service.AddServiceWebRequest;
import org.apache.ambari.funtest.server.api.service.InstallServiceWebRequest;
import org.apache.ambari.funtest.server.api.servicecomponent.AddServiceComponentWebRequest;
import org.apache.ambari.funtest.server.api.servicecomponenthost.BulkAddServiceComponentHostsWebRequest;
import org.apache.ambari.funtest.server.api.servicecomponenthost.BulkSetServiceComponentHostStateWebRequest;
import org.apache.ambari.funtest.server.api.user.CreateUserWebRequest;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.State;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ClusterUtils {

    private static Log LOG = LogFactory.getLog(ClusterUtils.class);

    @Inject
    private Injector injector;

    public void createSampleCluster(ConnectionParams serverParams) throws Exception {
        WebResponse response = null;
        JsonElement jsonResponse;
        String clusterName = "c1";
        String hostName = "host1";
        String clusterVersion = "HDP-2.2.0";

        /**
         * Create a cluster
         */
        jsonResponse = RestApiUtils.executeRequest(new CreateClusterWebRequest(serverParams, clusterName, clusterVersion));

        /**
         * Register a host
         */
        if (injector == null) {
            jsonResponse =  RestApiUtils.executeRequest(new RegisterHostWebRequest(serverParams, hostName));
        }
        else {
            /**
             * Hack: Until we figure out how to get the agent servlet going,
             * register a host directly using the Clusters class.
             */
            Clusters clusters = injector.getInstance(Clusters.class);
            clusters.addHost(hostName);
            Host host1 = clusters.getHost(hostName);
            Map<String, String> hostAttributes = new HashMap<String, String>();
            hostAttributes.put("os_family", "redhat");
            hostAttributes.put("os_release_version", "6.3");
            host1.setHostAttributes(hostAttributes);
        }

        /**
         * Add the registered host to the new cluster
         */
        jsonResponse =  RestApiUtils.executeRequest(new AddHostWebRequest(serverParams, clusterName, hostName));

        /**
         * Create and add a configuration to our cluster
         */

        String configType = "test-hadoop-env";
        String configTag = "version1";
        ClusterConfigParams configParams = new ClusterConfigParams();
        configParams.setClusterName(clusterName);
        configParams.setConfigType(configType);
        configParams.setConfigTag(configTag);
        configParams.setProperties(new HashMap<String, String>() {{
            put("fs.default.name", "localhost:9995");
        }});

        jsonResponse = RestApiUtils.executeRequest(new CreateConfigurationWebRequest(serverParams, configParams));

        /**
         * Apply the desired configuration to our cluster
         */
        jsonResponse = RestApiUtils.executeRequest(new AddDesiredConfigurationWebRequest(serverParams, configParams));

        /**
         * Add a service to the cluster
         */

        String serviceName = "HDFS";
        jsonResponse = RestApiUtils.executeRequest(new AddServiceWebRequest(serverParams, clusterName, serviceName));

        String [] componentNames = new String [] {"NAMENODE", "DATANODE", "SECONDARY_NAMENODE"};

        /**
         * Add components to the service
         */
        for (String componentName : componentNames) {
            jsonResponse = RestApiUtils.executeRequest(new AddServiceComponentWebRequest(serverParams, clusterName,
                    serviceName, componentName));
        }

        /**
         * Install the service
         */
        jsonResponse = RestApiUtils.executeRequest(new InstallServiceWebRequest(serverParams, clusterName, serviceName));

        /**
         * Add components to the hostß
         */

        jsonResponse = RestApiUtils.executeRequest(new BulkAddServiceComponentHostsWebRequest(serverParams, clusterName,
                Arrays.asList(hostName), Arrays.asList(componentNames)));

        /**
         * Install the service component hosts
         */
        jsonResponse = RestApiUtils.executeRequest(new BulkSetServiceComponentHostStateWebRequest(serverParams,
                    clusterName, State.INIT, State.INSTALLED));
        if (!jsonResponse.isJsonNull()) {
            int requestId = parseRequestId(jsonResponse);
            RequestStatusPoller.poll(serverParams, clusterName, requestId);
        }

        /**
         * Start the service component hosts
         */

        jsonResponse = RestApiUtils.executeRequest(new BulkSetServiceComponentHostStateWebRequest(serverParams,
                clusterName, State.INSTALLED, State.STARTED));
        if (!jsonResponse.isJsonNull()) {
            int requestId = parseRequestId(jsonResponse);
            RequestStatusPoller.poll(serverParams, clusterName, requestId);
        }

        /**
         * Start the service
         */
        //jsonResponse = RestApiUtils.executeRequest(new StartServiceWebRequest(serverParams, clusterName, serviceName));
    }

    /**
     * Creates a user with the specified role.
     *
     * @param connectionParams
     * @param clusterName
     * @param userName
     * @param password
     * @param userRole
     * @throws Exception
     */
    public static void createUser(ConnectionParams connectionParams, String clusterName,String userName,
                                 String password, AmbariUserRole userRole ) throws Exception {
        JsonElement jsonResponse;

        jsonResponse = RestApiUtils.executeRequest(new CreateUserWebRequest(connectionParams, userName, password,
                CreateUserWebRequest.ActiveUser.TRUE, CreateUserWebRequest.AdminUser.FALSE));

        LOG.info(jsonResponse);

        if (userRole != AmbariUserRole.NONE) {
            jsonResponse = RestApiUtils.executeRequest(new SetUserPrivilegeWebRequest(connectionParams,
                    clusterName, userName, userRole, PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME));

            LOG.info(jsonResponse);
        }
    }

    /**
     * Creates a user with CLUSTER.USER privilege.
     *
     * @param connectionParams
     * @param clusterName
     * @param userName
     * @param password
     * @throws Exception
     */
    public static void createUserClusterUser(ConnectionParams connectionParams, String clusterName,
                                              String userName, String password) throws Exception {
        createUser(connectionParams, clusterName, userName, password, AmbariUserRole.CLUSTER_USER);
    }

    /**
     * Creates a user with SERVICE.OPERATOR privilege
     *
     * @param connectionParams
     * @param clusterName
     * @param userName
     * @param password
     * @throws Exception
     */
    public static void createUserServiceOperator(ConnectionParams connectionParams, String clusterName,
                                              String userName, String password) throws Exception {
        createUser(connectionParams, clusterName, userName, password, AmbariUserRole.SERVICE_OPERATOR);
    }

    /**
     * Creates a user with SERVICE.ADMINISTRATOR privilege
     *
     * @param connectionParams
     * @param clusterName
     * @param userName
     * @param password
     * @throws Exception
     */
    public static void createUserServiceAdministrator(ConnectionParams connectionParams, String clusterName,
                                              String userName, String password) throws Exception {
        createUser(connectionParams, clusterName, userName, password, AmbariUserRole.SERVICE_ADMINISTRATOR);
    }

    /**
     * Creates a user with CLUSTER.OPERATOR privilege
     *
     * @param connectionParams
     * @param clusterName
     * @param userName
     * @param password
     * @throws Exception
     */
    public static void createUserClusterOperator(ConnectionParams connectionParams, String clusterName,
                                              String userName, String password) throws Exception {
        createUser(connectionParams, clusterName, userName, password, AmbariUserRole.CLUSTER_OPERATOR);
    }

    /**
     * Creates a user with CLUSTER.ADMINISTRATOR privilege.
     *
     * @param connectionParams
     * @param clusterName
     * @param userName
     * @param password
     * @throws Exception
     */
    public static void createUserClusterAdministrator(ConnectionParams connectionParams, String clusterName,
                                              String userName, String password) throws Exception {
        createUser(connectionParams, clusterName, userName, password, AmbariUserRole.CLUSTER_ADMINISTRATOR);
    }

    /**
     * Parses a JSON response string for  { "Requests" : { "id" : "2" } }
     *
     * @param jsonResponse
     * @return - request id
     * @throws IllegalArgumentException
     */
    private static int parseRequestId(JsonElement jsonResponse) throws IllegalArgumentException {
        if (jsonResponse.isJsonNull()) {
            throw new IllegalArgumentException("jsonResponse with request id expected.");
        }

        JsonObject jsonObject = jsonResponse.getAsJsonObject();
        int requestId = jsonObject.get("Requests").getAsJsonObject().get("id").getAsInt();
        return requestId;
    }
}
