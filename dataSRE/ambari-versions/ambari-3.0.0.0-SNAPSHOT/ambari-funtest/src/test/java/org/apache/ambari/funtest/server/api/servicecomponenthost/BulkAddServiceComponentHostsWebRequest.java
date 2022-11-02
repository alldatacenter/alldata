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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.ambari.funtest.server.AmbariHttpWebRequest;
import org.apache.ambari.funtest.server.ConnectionParams;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Bulk add a set of components on multiple hosts.
 * Replaces multiple calls to AddServiceComponentHostWebRequest
 */
public class BulkAddServiceComponentHostsWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private List<String> hostNames;
    private List<String> componentNames;
    private static String pathFormat = "/api/v1/clusters/%s/hosts";

    /**
     * Adds multiple componenents to multiple hosts.
     *
     * @param params - Ambari server connection information.
     * @param clusterName - Existing cluster.
     * @param hostNames - Hosts on which components are to be added.
     * @param componentNames - Components to be added.
     */
    public BulkAddServiceComponentHostsWebRequest(ConnectionParams params, String clusterName, List<String> hostNames,
                                                  List<String> componentNames) {
        super(params);
        this.clusterName = clusterName;
        this.hostNames = new ArrayList<>(hostNames);
        this.componentNames = new ArrayList<>(componentNames);
    }

    public String getClusterName() { return this.clusterName; }

    public List<String> getHostNames() { return Collections.unmodifiableList(this.hostNames); }

    public List<String> getComponentNames() { return Collections.unmodifiableList(this.componentNames); }

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
     * Constructs the request data.
     *
     * @return - Request data.
     */
    @Override
    protected String getRequestData() {
        /**
         * {
         *   "RequestInfo" : {
         *     "query":"Hosts/host_name.in(host1,host2)"
         *   },
         *   "Body" : {
         *     "host_components": [
         *     {
         *       "HostRoles" : { "component_name" : "HIVE_CLIENT" }
         *     },
         *     {
         *       "HostRoles" : { "component_name" : "TEZ_CLIENT" }
         *     }
         *     ]
         * }
         */
        JsonObject jsonObject =  new JsonObject();
        JsonArray hostRoles = new JsonArray();

        jsonObject.add("RequestInfo", createJsonObject("query", String.format("Hosts/host_name.in(%s)", toCsv(hostNames))));

        for (String componentName : componentNames) {
            hostRoles.add(createJsonObject("HostRoles", createJsonObject("component_name", componentName)));
        }

        jsonObject.add("Body", createJsonObject("host_components", hostRoles));

        Gson gson = new Gson();
        return gson.toJson(jsonObject);
    }

    private static String toCsv(List<String> list) {
        StringBuilder sb = new StringBuilder();

        for (String item : list) {
            sb.append(String.format("%s,", item));
        }

        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }
}
