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

package org.apache.ambari.funtest.server.api.servicecomponent;

import org.apache.ambari.funtest.server.AmbariHttpWebRequest;
import org.apache.ambari.funtest.server.ConnectionParams;

/**
 * Adds a component to a service.
 */
public class AddServiceComponentWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private String serviceName;
    private String componentName;
    private static String pathFormat = "/api/v1/clusters/%s/services/%s/components/%s";

    /**
     * Adds the specified service component to the specified service.
     *
     * @param params - Ambari server connection information.
     * @param clusterName - Existing cluster name.
     * @param serviceName - Existing service name.
     * @param componentName - Component to be added to a service.
     */
    public AddServiceComponentWebRequest(ConnectionParams params, String clusterName, String serviceName,
                                         String componentName) {
        super(params);
        this.clusterName = clusterName;
        this.serviceName = serviceName;
        this.componentName = componentName;
    }

    public String getClusterName() { return this.clusterName; }

    public String getServiceName() { return this.serviceName; }

    public String getComponentName() { return this.componentName; }

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
        return String.format(pathFormat, clusterName, serviceName, componentName);
    }
}
