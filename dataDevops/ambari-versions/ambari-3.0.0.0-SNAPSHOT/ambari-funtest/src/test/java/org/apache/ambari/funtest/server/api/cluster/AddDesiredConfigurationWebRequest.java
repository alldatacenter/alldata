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

package org.apache.ambari.funtest.server.api.cluster;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.ambari.funtest.server.AmbariHttpWebRequest;
import org.apache.ambari.funtest.server.ClusterConfigParams;
import org.apache.ambari.funtest.server.ConnectionParams;

/**
 * Adds an existing configuration, identified by it's type and tag, to a cluster.
 */
public class AddDesiredConfigurationWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private String configType;
    private String configTag;
    private static String pathFormat = "/api/v1/clusters/%s";

    /**
     *
     * @param serverParams - Ambari server connection information
     * @param configParams - Cluster configuration parameters
     */
    public AddDesiredConfigurationWebRequest(ConnectionParams serverParams, ClusterConfigParams configParams) {
        super(serverParams);
        this.clusterName = configParams.getClusterName();
        this.configType = configParams.getConfigType();
        this.configTag = configParams.getConfigTag();
    }

    /**
     * Gets the cluster name.
     *
     * @return - Cluster name.
     */
    public String getClusterName() { return this.clusterName; }

    /**
     * Gets the configuration type.
     *
     * @return - Configuration type.
     */
    public String getConfigType() { return this.configType; }

    /**
     * Gets the configuration tag.
     *
     * @return - Configuration tag.
     */
    public String getConfigTag() { return this.configTag; }

    /**
     * Gets the REST API method.
     *
     * @return - PUT.
     */
    @Override
    public String getHttpMethod() {
        return "PUT";
    }

    /**
     * Get REST API path fragment for construction full URI.
     *
     * @return - REST API path
     */
    @Override
    protected String getApiPath() {
        return String.format(pathFormat, clusterName);
    }

    /**
     * Gets the request data.
     *
     * @return - Request data.
     */
    @Override
    protected String getRequestData() {
        /**
         * { "Clusters" : {"desired_configs": {"type": "test-site", "tag" : "version1" }}}
         */
        JsonObject jsonObject;
        JsonObject jsonDesiredConfigs = new JsonObject();

        jsonDesiredConfigs.addProperty("type", configType);
        jsonDesiredConfigs.addProperty("tag", configTag);
        jsonObject = createJsonObject("Clusters", createJsonObject("desired_configs", jsonDesiredConfigs));
        Gson gson = new Gson();
        return gson.toJson(jsonObject);
    }
}
