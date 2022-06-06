/**
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
package org.apache.atlas.model.impexp;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.model.AtlasBaseModelObject;
import org.apache.atlas.type.AtlasType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasServer extends AtlasBaseModelObject implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String KEY_REPLICATION_DETAILS = "REPL_DETAILS";

    private String              name;
    private String              fullName;
    private String              displayName;
    private Map<String, String> additionalInfo = new HashMap<>();
    private List<String>        urls           = new ArrayList<>();

    public AtlasServer() {
    }

    public AtlasServer(String name, String fullName) {
        this(name, name, fullName);
    }

    public AtlasServer(String name, String displayName, String fullName) {
        this.name        = name;
        this.displayName = displayName;
        this.fullName    = fullName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setAdditionalInfo(Map<String, String> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public Map<String, String> getAdditionalInfo() {
        return this.additionalInfo;
    }

    public String getAdditionalInfo(String key) {
        return additionalInfo.get(key);
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public List<String> getUrls() {
        return this.urls;
    }


    public void setAdditionalInfo(String key, String value) {
        if (additionalInfo == null) {
            additionalInfo = new HashMap<>();
        }

        additionalInfo.put(key, value);
    }

    public void setAdditionalInfoRepl(String guid, long modifiedTimestamp) {
        Map<String, Object> replicationDetailsMap = null;

        if (additionalInfo != null && additionalInfo.containsKey(KEY_REPLICATION_DETAILS)) {
            replicationDetailsMap = AtlasType.fromJson(getAdditionalInfo().get(KEY_REPLICATION_DETAILS), Map.class);
        }

        if (replicationDetailsMap == null) {
            replicationDetailsMap = new HashMap<>();
        }

        if (modifiedTimestamp == 0) {
            replicationDetailsMap.remove(guid);
        } else {
            replicationDetailsMap.put(guid, modifiedTimestamp);
        }

        updateReplicationMap(replicationDetailsMap);
    }

    public Object getAdditionalInfoRepl(String guid) {
        if (additionalInfo == null || !additionalInfo.containsKey(KEY_REPLICATION_DETAILS)) {
            return null;
        }

        String key     = guid;
        String mapJson = additionalInfo.get(KEY_REPLICATION_DETAILS);

        Map<String, String> replicationDetailsMap = AtlasType.fromJson(mapJson, Map.class);

        if (!replicationDetailsMap.containsKey(key)) {
            return null;
        }

        return replicationDetailsMap.get(key);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append(", name=").append(name);
        sb.append(", fullName=").append(fullName);
        sb.append(", displayName=").append(displayName);
        sb.append(", additionalInfo=").append(additionalInfo);
        sb.append(", urls=").append(urls);

        return sb;
    }

    private void updateReplicationMap(Map<String, Object> replicationDetailsMap) {
        String json = AtlasType.toJson(replicationDetailsMap);

        setAdditionalInfo(KEY_REPLICATION_DETAILS, json);
    }
}
