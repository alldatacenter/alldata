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

package org.apache.ambari.funtest.server.api.servicecomponenthost;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.ambari.funtest.server.AmbariHttpWebRequest;
import org.apache.ambari.funtest.server.ConnectionParams;
import org.apache.ambari.server.state.State;

/**
 * Updates the state of the specified set of components.
 */
public class BulkSetServiceComponentHostStateWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private State currentState;
    private State desiredState;

    private static String pathFormat = "/api/v1/clusters/%s/host_components?HostRoles/state=%s";

    /**
     * Updates the state of multiple components in a cluster.
     *
     * @param params - Ambari server connection information.
     * @param clusterName - Existing cluster name.
     * @param currentState - Desired state.
     * @param desiredState - Current state.
     */
    public BulkSetServiceComponentHostStateWebRequest(ConnectionParams params, String clusterName, State currentState,
                                                      State desiredState) {
        super(params);
        this.clusterName = clusterName;
        this.currentState = currentState;
        this.desiredState = desiredState;
    }

    @Override
    public String getHttpMethod() {
        return "PUT";
    }

    public State getCurrentState() { return this. currentState; }

    public State getDesiredState() { return this.desiredState; }

    /**
     * Get REST API path fragment for construction full URI.
     *
     * @return - REST API path
     */
    @Override
    protected String getApiPath() {
        return String.format(pathFormat, clusterName, this.currentState);
    }

    /**
     * Constructs the request data.
     *
     * @return - Request data.
     */
    @Override
    protected String getRequestData() {
        /**
         *     {
         *       "HostRoles" : { "state" : "INSTALLED" }
         *     }
         */
        JsonObject jsonObject =  new JsonObject();

        jsonObject.add("HostRoles", createJsonObject("state", desiredState.toString()));

        Gson gson = new Gson();
        return gson.toJson(jsonObject);
    }
}
