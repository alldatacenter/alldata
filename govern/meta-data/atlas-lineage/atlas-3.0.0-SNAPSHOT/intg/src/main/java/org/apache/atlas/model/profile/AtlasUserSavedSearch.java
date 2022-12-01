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
import org.apache.atlas.model.discovery.SearchParameters;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasUserSavedSearch extends AtlasBaseModelObject implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum SavedSearchType {
        BASIC,
        ADVANCED;

        public static SavedSearchType to(String val) {
            return SavedSearchType.ADVANCED.name().equalsIgnoreCase(val) ? SavedSearchType.ADVANCED : SavedSearchType.BASIC;
        }
    }

    private String           ownerName;
    private String           name;
    private SavedSearchType  searchType;
    private SearchParameters searchParameters;
    private String uiParameters;


    public AtlasUserSavedSearch() {
        this(null, null, SavedSearchType.BASIC, null);
    }

    public AtlasUserSavedSearch(String name, SavedSearchType searchType, SearchParameters searchParameters) {
        this(null, name, searchType, searchParameters);
    }

    public AtlasUserSavedSearch(String ownerName, String name) {
        this(ownerName, name, SavedSearchType.BASIC, null);
    }

    public AtlasUserSavedSearch(String ownerName, String name, SavedSearchType savedSearchType, SearchParameters searchParameters) {
        setOwnerName(ownerName);
        setName(name);
        setSearchType(savedSearchType);
        setSearchParameters(searchParameters);
    }

    public AtlasUserSavedSearch(String ownerName, String name, SavedSearchType searchType, SearchParameters searchParameters, String uiParameters) {
        this(ownerName, name, searchType, searchParameters);
        setUiParameters(uiParameters);
    }


    public String getOwnerName() {
        return this.ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SavedSearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SavedSearchType searchType) {
        this.searchType = searchType;
    }

    public SearchParameters getSearchParameters() {
        return searchParameters;
    }

    public void setSearchParameters(SearchParameters searchParameters) {
        this.searchParameters = searchParameters;
    }

    public String getUiParameters() {
        return uiParameters;
    }

    public void setUiParameters(String uiParameters) {
        this.uiParameters = uiParameters;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append(", ownerName=").append(ownerName);
        sb.append(", name=").append(name);
        sb.append(", searchType=").append(searchType);
        sb.append(", searchParameters=");
        if (searchParameters == null) {
            sb.append("null");
        } else {
            searchParameters.toString(sb);
        }

        sb.append(", uiParameters=").append(uiParameters);

        return sb;
    }
}
