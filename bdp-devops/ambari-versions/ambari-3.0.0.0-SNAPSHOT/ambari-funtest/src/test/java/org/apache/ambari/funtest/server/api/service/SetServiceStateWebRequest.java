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
import org.apache.ambari.server.state.State;

/**
 * Updates the state of a service in a cluster.
 */
public class SetServiceStateWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private String serviceName;
    private State serviceState;
    private String requestContext;
    private static String pathFormat = "/api/v1/clusters/%s/services/%s";

    /**
     * Updates the state of the specified service in the specified cluster.
     *
     * @param params - Ambari connection information.
     * @param clusterName - Existing cluster name.
     * @param serviceName - Service name whose state is to be updated.
     * @param serviceState - New service state.
     * @param requestContext - Comments or remarks.
     */
    public SetServiceStateWebRequest(ConnectionParams params, String clusterName, String serviceName, State serviceState,
                                     String requestContext) {
        super(params);
        this.clusterName = clusterName;
        this.serviceName = serviceName;
        this.serviceState = serviceState;
        this.requestContext = requestContext;
    }

    public String getClusterName() { return this.clusterName; }

    public String getHostName() { return this.serviceName; }

    public State getServiceState() { return this.serviceState; }

    public String getRequestContext() { return this.requestContext; }

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
        return String.format(pathFormat, clusterName, serviceName);
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
         * "RequestInfo" : {"context" : requestContext},
         * "Body" : {"ServiceInfo" : {"state" : serviceState}}
         * }
         */
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("RequestInfo", createJsonObject("context", requestContext));
        jsonObject.add("Body", createJsonObject("ServiceInfo", createJsonObject("state", serviceState.toString())));
        Gson gson = new Gson();
        return gson.toJson(jsonObject);
    }
}
