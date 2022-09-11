/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.sort.entity;

import java.util.Objects;

public class CacheZoneCluster {

    private String clusterId;
    private String bootstraps;
    private String token;

    public CacheZoneCluster(String clusterId, String bootstraps, String token) {
        this.clusterId = clusterId;
        this.bootstraps = bootstraps;
        this.token = token;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getBootstraps() {
        return bootstraps;
    }

    public void setBootstraps(String bootstraps) {
        this.bootstraps = bootstraps;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CacheZoneCluster that = (CacheZoneCluster) o;
        return clusterId.equals(that.clusterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterId);
    }

    @Override
    public String toString() {
        return "CacheZoneCluster>>>" + clusterId + "|" + bootstraps + "|" + token;
    }
}
