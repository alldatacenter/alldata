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

package org.apache.ambari.funtest.server.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.ambari.funtest.server.ConnectionParams;
import org.apache.ambari.funtest.server.WebRequest;
import org.apache.ambari.funtest.server.api.cluster.GetRequestStatusWebRequest;
import org.apache.ambari.server.actionmanager.HostRoleStatus;

/**
 * Polls the status of a service component host request.
 */
class RequestStatusPoller implements Runnable {
    private HostRoleStatus hostRoleStatus;
    private ConnectionParams serverParams;
    private String clusterName;
    private int requestId;

    public RequestStatusPoller(ConnectionParams serverParams, String clusterName, int requestId) {
        this.hostRoleStatus = HostRoleStatus.IN_PROGRESS;
        this.serverParams = serverParams;
        this.clusterName = clusterName;
        this.requestId = requestId;
    }

    public HostRoleStatus getHostRoleStatus() {
        return this.hostRoleStatus;
    }

    public static boolean poll(ConnectionParams serverParams, String clusterName, int requestId) throws Exception {
        RequestStatusPoller poller = new RequestStatusPoller(serverParams, clusterName, requestId);
        Thread pollerThread = new Thread(poller);
        pollerThread.start();
        pollerThread.join();
        if (poller.getHostRoleStatus() == HostRoleStatus.COMPLETED)
            return true;

        return false;
    }

    @Override
    public void run() {
        int retryCount = 5;
        while (true) {
            JsonElement jsonResponse;

            try {
                WebRequest webRequest = new GetRequestStatusWebRequest(serverParams, clusterName, requestId);
                jsonResponse = RestApiUtils.executeRequest(webRequest);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            if (!jsonResponse.isJsonNull()) {
                JsonObject jsonObj = jsonResponse.getAsJsonObject();
                JsonObject jsonRequestsObj = jsonObj.getAsJsonObject("Requests");
                String requestStatus = jsonRequestsObj.get("request_status").getAsString();
                hostRoleStatus = HostRoleStatus.valueOf(requestStatus);

                if (hostRoleStatus == HostRoleStatus.COMPLETED ||
                        hostRoleStatus == HostRoleStatus.ABORTED ||
                        hostRoleStatus == HostRoleStatus.TIMEDOUT ||
                        retryCount == 0)
                    break;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                break;
            }

            retryCount--;
        }
    }
}