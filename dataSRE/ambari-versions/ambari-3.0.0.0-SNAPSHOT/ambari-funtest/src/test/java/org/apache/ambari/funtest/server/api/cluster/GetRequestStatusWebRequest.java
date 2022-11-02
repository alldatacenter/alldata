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

import org.apache.ambari.funtest.server.AmbariHttpWebRequest;
import org.apache.ambari.funtest.server.ConnectionParams;

/**
 * Gets the status of a request by request id. For example:
 * curl --user admin:admin -i -X GET http://AMBARI_SERVER_HOST:8080/api/v1/clusters/CLUSTER_NAME/requests/9
 * curl --user admin:admin -i -X GET http://AMBARI_SERVER_HOST:8080/api/v1/clusters/CLUSTER_NAME/requests/9/tasks/101
 *
 * Response:
 * {
 * "href" : "http://AMBARI_SERVER_HOST:8080/api/v1/clusters/CLUSTER_NAME/requests/9/tasks/101",
 * "Tasks" : {
 * ...
 * "status" : "COMPLETED",
 * ...
 * }
 * }
 */
public class GetRequestStatusWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private int requestId;
    private int taskId;
    private static String pathFormat = "/api/v1/clusters/%s/requests/%d";
    private static String pathFormatWithTask = "/api/v1/clusters/%s/requests/%d/tasks/%d";

    public GetRequestStatusWebRequest(ConnectionParams params, String clusterName, int requestId) {
        this(params, clusterName, requestId, -1);
    }

    public GetRequestStatusWebRequest(ConnectionParams params, String clusterName, int requestId, int taskId) {
        super(params);
        this.clusterName = clusterName;
        this.requestId = requestId;
        this.taskId = taskId;
    }

    public String getClusterName() { return this.clusterName; }

    public int getRequestId() { return this.requestId; }

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
        if (taskId != -1)
            return String.format(pathFormatWithTask, clusterName, requestId, taskId);

        return String.format(pathFormat, clusterName, requestId);
    }
}
