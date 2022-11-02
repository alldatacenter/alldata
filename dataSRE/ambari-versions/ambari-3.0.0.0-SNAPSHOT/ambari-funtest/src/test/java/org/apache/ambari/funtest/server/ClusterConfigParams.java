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

package org.apache.ambari.funtest.server;

import java.util.Map;

/**
 * Configurations in Ambari are distinguished using type and tag.
 * Once a configuration is created, it can be applied to a service,
 * specifying the type and tag for the configuration.
 *
 * Example:
 * curl -i -X POST -d '{"type": "core-site", "tag": "version1363902625", "properties" : { "fs.default.name" : "localhost:8020"}}'
 * http://<cluster-name>:8080/api/v1/clusters/c1/configurations
 */
public class ClusterConfigParams {
    /**
     * Name of the cluster
     */
    private String clusterName;

    /**
     * Configuration tag
     */
    private String configTag;

    /**
     * Configuration type
     */
    private String configType;

    /**
     * Configuration properties
     */
    private Map<String, String> properties;

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getConfigTag() {
        return configTag;
    }

    public void setConfigTag(String configTag) {
        this.configTag = configTag;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
}
