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
import org.apache.ambari.funtest.server.AmbariUserRole;
import org.apache.ambari.funtest.server.ConnectionParams;

/**
 * Adds an existing configuration, identified by it's type and tag, to a cluster.
 */
public class SetUserPrivilegeWebRequest extends AmbariHttpWebRequest {
    private final String clusterName;
    private final String userName;
    private final String principalType;
    private final AmbariUserRole userRole;
    private static String pathFormat = "/api/v1/clusters/%s/privileges";

    /**
     *
     * @param serverParams - Ambari server connection information
     * @param clusterName
     * @param userName
     * @param userRole
     * @param principalType - USER or GROUP. Use PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME or
     *                      PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME
     */
    public SetUserPrivilegeWebRequest(ConnectionParams serverParams, String clusterName, String userName,
                                      AmbariUserRole userRole, String principalType) {

        super(serverParams);
        this.clusterName = clusterName;
        this.userName = userName;
        this.principalType = principalType;
        this.userRole = userRole;
    }

    /**
     * Gets the REST API method.
     *
     * @return - PUT.
     */
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
        /**
         * { "PrivilegeInfo" : {"permission_name": userRole, "principal_name": userName, "principal_type": principalType}}
         */
        JsonObject jsonObject;
        JsonObject jsonPrivilegeInfo = new JsonObject();

        jsonPrivilegeInfo.addProperty("permission_name", userRole.toString());
        jsonPrivilegeInfo.addProperty("principal_name", userName);
        jsonPrivilegeInfo.addProperty("principal_type", principalType);
        jsonObject = createJsonObject("PrivilegeInfo", jsonPrivilegeInfo);
        Gson gson = new Gson();
        return gson.toJson(jsonObject);
    }
}
