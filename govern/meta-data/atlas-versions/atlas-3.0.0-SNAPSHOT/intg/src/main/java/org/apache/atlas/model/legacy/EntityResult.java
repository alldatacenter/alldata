/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.model.legacy;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.type.AtlasType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class EntityResult {
    public static final String OP_CREATED = "created";
    public static final String OP_UPDATED = "updated";
    public static final String OP_DELETED = "deleted";

    Map<String, List<String>> entities = new HashMap<>();

    public EntityResult() {
        //For gson
    }

    public EntityResult(List<String> created, List<String> updated, List<String> deleted) {
        set(OP_CREATED, created);
        set(OP_UPDATED, updated);
        set(OP_DELETED, deleted);
    }

    public void set(String type, List<String> list) {
        if (list != null && list.size() > 0) {
            entities.put(type, list);
        }
    }

    private List<String> get(String type) {
        List<String> list = entities.get(type);
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public Map<String, List<String>> getEntities(){
        return entities;
    }

    public void setEntities(Map<String, List<String>> entities){
        this.entities = entities;
    }

    @JsonIgnore
    public List<String> getCreatedEntities() {
        return get(OP_CREATED);
    }

    @JsonIgnore
    public List<String> getUpdateEntities() {
        return get(OP_UPDATED);
    }

    @JsonIgnore
    public List<String> getDeletedEntities() {
        return get(OP_DELETED);
    }

    @Override
    public String toString() { return AtlasType.toV1Json(this); }

    public static EntityResult fromString(String json) {
        return AtlasType.fromV1Json(json, EntityResult.class);
    }
}
