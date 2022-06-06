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

import org.apache.ambari.funtest.server.AmbariHttpWebRequest;
import org.apache.ambari.funtest.server.ConnectionParams;

/**
 * Deletes the specified service from the specified cluster.
 */
public class DeleteServiceWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private String serviceName;
    private static String pathFormat = "/api/v1/clusters/%s/services/%s";

    /**
     * Deletes a service from a cluster.
     *
     * @param params - Ambari server connection information
     * @param clusterName - Cluster from where the service is to be deleted.
     * @param serviceName - Service to be deleted.
     */
    public DeleteServiceWebRequest(ConnectionParams params, String clusterName, String serviceName) {
        super(params);
        this.clusterName = clusterName;
        this.serviceName = serviceName;
    }

    public String getClusterName() {
        return this.clusterName;
    }

    public String getHostName() {
        return this.serviceName;
    }

    @Override
    public String getHttpMethod() {
        return "DELETE";
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
}
