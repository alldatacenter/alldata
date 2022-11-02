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

package org.apache.ambari.funtest.server.tests;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;

import org.apache.ambari.funtest.server.AmbariUserRole;
import org.apache.ambari.funtest.server.ClusterConfigParams;
import org.apache.ambari.funtest.server.ConnectionParams;
import org.apache.ambari.funtest.server.WebRequest;
import org.apache.ambari.funtest.server.WebResponse;
import org.apache.ambari.funtest.server.api.cluster.CreateClusterWebRequest;
import org.apache.ambari.funtest.server.api.cluster.CreateConfigurationWebRequest;
import org.apache.ambari.funtest.server.api.cluster.DeleteClusterWebRequest;
import org.apache.ambari.funtest.server.api.cluster.GetAllClustersWebRequest;
import org.apache.ambari.funtest.server.api.user.DeleteUserWebRequest;
import org.apache.ambari.funtest.server.utils.ClusterUtils;
import org.apache.ambari.funtest.server.utils.RestApiUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.JsonElement;

/**
 * Tests operations with users with different levels of privileges
 */
@Ignore
public class RoleBasedAccessControlBasicTest extends ServerTestBase {

    private String clusterName = "c1";
    private String hostName = "host1";
    private String clusterVersion = "HDP-2.2.0";

    private static Log LOG = LogFactory.getLog(RoleBasedAccessControlBasicTest.class);

    @Override
    public void setup() throws Exception {
        super.setup();
        setupCluster();
    }

    @Override
    public void teardown() throws Exception {
        teardownCluster();
        super.teardown();
    }

    /**
     * Creates an anonymous user (user with no role). Attempts to get the list of clusters
     *
     * @throws Exception
     */
    @Test
    public void testGetClustersAsAnonUser() throws Exception {
        JsonElement jsonResponse;
        ConnectionParams adminConnectionParams = createAdminConnectionParams();
        String anonUserName = "nothing";
        String anonUserPwd = "nothing";

        /**
         * Create a new user (non-admin)
         */
        ClusterUtils.createUser(adminConnectionParams, clusterName, anonUserName, anonUserPwd, AmbariUserRole.NONE);

        /**
         * Attempt to query all the clusters using this user's privilege. Right now we should be
         * able to get the list of clusters, though this user should not be able to. But this is
         * required for UI to display the clusters.
         *
         * todo: Fix this when UI is fixed.
         */
        ConnectionParams anonUserParams = createConnectionParams(anonUserName, anonUserPwd);
        jsonResponse = RestApiUtils.executeRequest(new GetAllClustersWebRequest(anonUserParams));

        assertFalse(jsonResponse.isJsonNull());

        /**
         * Delete the user
         */
        jsonResponse = RestApiUtils.executeRequest(new DeleteUserWebRequest(adminConnectionParams, anonUserName));
        LOG.info(jsonResponse);
    }

    /**
     * Creates an anonymous user and uses the user to add a cluster configuration.
     *
     * @throws Exception
     */
    @Test
    public void testAddClusterConfigAsAnonUser() throws Exception {
        ConnectionParams adminConnectionParams = createAdminConnectionParams();
        String anonUserName = "nothing";
        String anonUserPwd = "nothing";

        /**
         * Create a new user (non-admin)
         */
        ClusterUtils.createUser(adminConnectionParams, clusterName, anonUserName, anonUserPwd, AmbariUserRole.NONE);

        /**
         * Create and add a configuration to our cluster using the new user's privilege
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

        /**
         * Attempting to create the configuration should fail with 403
         */
        ConnectionParams anonUserParams = createConnectionParams(anonUserName, anonUserPwd);
        WebRequest webRequest = new CreateConfigurationWebRequest(anonUserParams, configParams);
        WebResponse webResponse = webRequest.getResponse();
        assertEquals(HttpStatus.SC_FORBIDDEN, webResponse.getStatusCode());

        /**
         * Delete the user
         */
        JsonElement jsonResponse = RestApiUtils.executeRequest(new DeleteUserWebRequest(adminConnectionParams, "nothing"));
        LOG.info(jsonResponse);
    }

    /**
     * Creates a user with cluster administrator privilege and adds a cluster configuration.
     *
     * @throws Exception
     */
    @Test
    public void testAddClusterConfigAsClusterAdmin() throws Exception {
        ConnectionParams adminConnectionParams = createAdminConnectionParams();

        String clusterAdminName = "clusterAdmin";
        String clusterAdminPwd = "clusterAdmin";

        /**
         * Create a user with cluster admin role
         */
        ClusterUtils.createUserClusterAdministrator(adminConnectionParams, clusterName,
                clusterAdminName, clusterAdminPwd);

        /**
         * Create and add a configuration to our cluster using the new user's privilege
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

        /**
         * This user has enough privilege to create the cluster configuration. Should succeed with 201.
         */
        ConnectionParams userConnectionParams = createConnectionParams(clusterAdminName, clusterAdminPwd);
        WebRequest webRequest = new CreateConfigurationWebRequest(userConnectionParams, configParams);
        WebResponse webResponse = webRequest.getResponse();
        assertEquals(HttpStatus.SC_CREATED, webResponse.getStatusCode());

        /**
         * Delete the user
         */
        RestApiUtils.executeRequest(new DeleteUserWebRequest(adminConnectionParams, clusterAdminName));
    }

    /**
     * Create a cluster with name "c1". Does not have any hosts.
     *
     * @throws Exception
     */
    private void setupCluster() throws Exception {
        JsonElement jsonResponse;
        ConnectionParams params = createAdminConnectionParams();

        /**
         * Create a cluster as admin:admin
         */
        jsonResponse = RestApiUtils.executeRequest(new CreateClusterWebRequest(params, clusterName, clusterVersion));

        LOG.info(jsonResponse);
    }

    private void teardownCluster() throws Exception {
        JsonElement jsonResponse;
        ConnectionParams params = createAdminConnectionParams();

        jsonResponse = RestApiUtils.executeRequest(new DeleteClusterWebRequest(params, clusterName));

        LOG.info(jsonResponse);
    }

    /**
     * Helper method to create administrator connection parameters to the server.
     *
     * @return
     */
    private ConnectionParams createAdminConnectionParams() {
        return createConnectionParams(getAdminUserName(), getAdminPassword());
    }

    /**
     * Helper method to create connection parameters to the server based on the
     * specified user credentials.
     *
     * @param userName
     * @param password
     * @return
     */
    private ConnectionParams createConnectionParams(String userName, String password) {
        ConnectionParams params = new ConnectionParams();

        params.setServerName("localhost");
        params.setServerApiPort(serverPort);
        params.setServerAgentPort(serverAgentPort);
        params.setUserName(userName);
        params.setPassword(password);

        return params;
    }
}
