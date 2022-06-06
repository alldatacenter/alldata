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
import org.apache.ambari.funtest.server.ConnectionParams;

/**
 * Creates a new cluster.
 */
public class CreateClusterWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private String clusterVersion;
    private static String pathFormat = "/api/v1/clusters/%s";

    /**
     * Creates a new cluster with the specified name and version.
     *
     * @param params - Ambari server connection information.
     * @param clusterName - Cluster name, like "test-cluster"
     * @param clusterVersion - Cluster version, like "HDP-2.2.0"
     */
    public CreateClusterWebRequest(ConnectionParams params, String clusterName, String clusterVersion) {
        super(params);
        this.clusterName = clusterName;
        this.clusterVersion = clusterVersion;
    }

    /**
     * Gets the cluster name.
     *
     * @return - Cluster name.
     */
    public String getClusterName() { return this.clusterName; }

    /**
     * Gets the cluster version.
     *
     * @return - Cluster version.
     */
    public String getClusterVersion() { return this.clusterVersion; }

    @Override
    public String getHttpMethod() {
        return "POST";
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
        JsonObject jsonClustersObj = new JsonObject();
        jsonClustersObj.add("Clusters", createJsonObject("version", getClusterVersion()));
        Gson gson = new Gson();
        return gson.toJson(jsonClustersObj);
    }
}
