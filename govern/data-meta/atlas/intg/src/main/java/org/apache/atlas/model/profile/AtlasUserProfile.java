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
package org.apache.atlas.model.profile;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.model.AtlasBaseModelObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.dumpObjects;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasUserProfile extends AtlasBaseModelObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private String                     name;
    private String                     fullName;
    private List<AtlasUserSavedSearch> savedSearches;


    public AtlasUserProfile() {
        this(null, null);
    }

    public AtlasUserProfile(String name) {
        this(name, null);
    }

    public AtlasUserProfile(String name, String fullName) {
        this.name          = name;
        this.fullName      = fullName;
        this.savedSearches = new ArrayList<>();
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setSavedSearches(List<AtlasUserSavedSearch> searches) {
        this.savedSearches = searches;
    }

    public List<AtlasUserSavedSearch> getSavedSearches() {
        return savedSearches;
    }


    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append(", name=").append(name);
        sb.append(", fullName=").append(fullName);
        sb.append(", savedSearches=[");
        if (savedSearches != null) {
            dumpObjects(savedSearches, sb);
        }
        sb.append("]");

        return sb;
    }
}
