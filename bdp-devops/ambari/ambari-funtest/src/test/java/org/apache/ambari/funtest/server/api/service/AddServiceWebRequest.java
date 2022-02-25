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

package org.apache.ambari.funtest.server.api.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.ambari.funtest.server.AmbariHttpWebRequest;
import org.apache.ambari.funtest.server.ConnectionParams;

/**
 * Adds a service to the specified cluster. The service name is sent in the request data.
 */
public class AddServiceWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private String serviceName;
    private static String pathFormat = "/api/v1/clusters/%s/services";

    /**
     * Add serviceName to the cluster clusterName
     *
     * @param params - Ambari server connection information
     * @param clusterName - Existing name of a cluster.
     * @param serviceName - Service name to be added.
     */
    public AddServiceWebRequest(ConnectionParams params, String clusterName, String serviceName) {
        super(params);
        this.clusterName = clusterName;
        this.serviceName = serviceName;
    }

    /**
     * Gets the cluster name.
     * @return
     */
    public String getClusterName() { return this.clusterName; }

    /**
     * Gets the service name to be added.
     *
     * @return
     */
    public String getServiceName() { return this.serviceName; }

    /**
     * Gets the REST API method to use.
     *
     * @return - POST.
     */
    @Override
    public String getHttpMethod() {
        return "POST";
    }

    /**
     * Gets the API fragment to be added to the server URL.
     * @return
     */
    @Override
    protected String getApiPath() {
        return String.format(pathFormat, clusterName);
    }

    /**
     * Constructs the request data.
     *
     * @return - Request data.
     */
    @Override
    protected String getRequestData() {
        /**
         * {
         *   "ServiceInfo" : {
         *     "service_name" : serviceName
         *   }
         * }
         */
        JsonObject jsonServiceInfoObj;
        jsonServiceInfoObj = createJsonObject("ServiceInfo", createJsonObject("service_name", serviceName));
        Gson gson = new Gson();
        return gson.toJson(jsonServiceInfoObj);
    }
}
