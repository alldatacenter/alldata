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

import org.apache.ambari.funtest.server.AmbariHttpWebRequest;
import org.apache.ambari.funtest.server.ConnectionParams;

/**
 * Gets a service component to a host.
 */
public class GetServiceComponentHostWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private String hostName;
    private String componentName;
    private static String pathFormat = "/api/v1/clusters/%s/hosts/%s/host_components/%s";

    /**
     * Gets the specified service component to the specified host.
     *
     * @param params - Ambari server connection information.
     * @param clusterName - Existing cluster.
     * @param hostName - Existing host.
     * @param componentName - Component to be added to hostName.
     */
    public GetServiceComponentHostWebRequest(ConnectionParams params, String clusterName, String hostName,
                                             String componentName) {
        super(params);
        this.clusterName = clusterName;
        this.hostName = hostName;
        this.componentName = componentName;
    }

    public String getClusterName() { return this.clusterName; }

    public String getHostName() { return this.hostName; }

    public String getComponentName() { return this.componentName; }

    @Override
    public String getHttpMethod() {
        return "GET";
    }

    /**
     * Get REST API path fragment for construction full URI.
     *
     * @return - REST API path
     */
    @Override
    protected String getApiPath() {
        return String.format(pathFormat, clusterName, hostName, componentName);
    }
}
