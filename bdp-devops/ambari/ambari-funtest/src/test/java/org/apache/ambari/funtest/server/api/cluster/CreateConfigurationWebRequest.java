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
import org.apache.ambari.funtest.server.ClusterConfigParams;
import org.apache.ambari.funtest.server.ConnectionParams;

import java.util.HashMap;
import java.util.Map;

public class CreateConfigurationWebRequest extends AmbariHttpWebRequest {
    private String clusterName;
    private String configType;
    private String configTag;
    private Map<String, String> properties;
    private static String pathFormat = "/api/v1/clusters/%s/configurations";

    public CreateConfigurationWebRequest(ConnectionParams serverParams, ClusterConfigParams configParams) {
        super(serverParams);
        this.clusterName = configParams.getClusterName();
        this.configType = configParams.getConfigType();
        this.configTag = configParams.getConfigTag();
        this.properties = new HashMap<>(configParams.getProperties());
    }

    public String getClusterName() { return this.clusterName; }

    public String getConfigType() { return this.configType; }

    public String getConfigTag() { return this.configTag; }

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
         *  {"type": "core-site", "tag": "version1363902625", "properties" : { "fs.default.name" : "localhost:8020"}}
         */
        JsonObject jsonPropertiesObj = new JsonObject();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            jsonPropertiesObj.addProperty(property.getKey(), property.getValue());
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", configType);
        jsonObject.addProperty("tag", configTag);
        jsonObject.add("properties", jsonPropertiesObj);
        Gson gson = new Gson();
        return gson.toJson(jsonObject);
    }
}
