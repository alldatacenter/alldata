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
package org.apache.atlas.repository.userprofile;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.annotation.AtlasService;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.profile.AtlasUserProfile;
import org.apache.atlas.model.profile.AtlasUserSavedSearch;
import org.apache.atlas.repository.ogm.DataAccess;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@AtlasService
public class UserProfileService {
    private static final Logger LOG = LoggerFactory.getLogger(UserProfileService.class);

    private final DataAccess dataAccess;

    @Inject
    public UserProfileService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public AtlasUserProfile saveUserProfile(AtlasUserProfile profile) throws AtlasBaseException {
        return dataAccess.save(profile);
    }

    public AtlasUserProfile getUserProfile(String userName) throws AtlasBaseException {
        AtlasUserProfile profile = new AtlasUserProfile(userName);

        return dataAccess.load(profile);
    }

    public AtlasUserSavedSearch addSavedSearch(AtlasUserSavedSearch savedSearch) throws AtlasBaseException {
        String userName = savedSearch.getOwnerName();
        AtlasUserProfile userProfile = null;

        try {
            userProfile = getUserProfile(userName);
        } catch (AtlasBaseException excp) {
            // ignore
        }

        if (userProfile == null) {
            userProfile = new AtlasUserProfile(userName);
        }

        checkIfQueryAlreadyExists(savedSearch, userProfile);
        userProfile.getSavedSearches().add(savedSearch);
        userProfile = dataAccess.save(userProfile);
        for (AtlasUserSavedSearch s : userProfile.getSavedSearches()) {
            if(s.getName().equals(savedSearch.getName())) {
                return s;
            }
        }

        return savedSearch;
    }

    private void checkIfQueryAlreadyExists(AtlasUserSavedSearch savedSearch, AtlasUserProfile userProfile) throws AtlasBaseException {
        for (AtlasUserSavedSearch exisingSearch : userProfile.getSavedSearches()) {
            if (StringUtils.equals(exisingSearch.getOwnerName(), savedSearch.getOwnerName()) &&
                StringUtils.equals(exisingSearch.getName(), savedSearch.getName())) {
                throw new AtlasBaseException(AtlasErrorCode.SAVED_SEARCH_ALREADY_EXISTS, savedSearch.getName(), savedSearch.getOwnerName());
            }
        }
    }

    public AtlasUserSavedSearch updateSavedSearch(AtlasUserSavedSearch savedSearch) throws AtlasBaseException {
        if (StringUtils.isBlank(savedSearch.getGuid())) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_OBJECT_ID, savedSearch.getGuid());
        }

        AtlasUserSavedSearch existingSearch = getSavedSearch(savedSearch.getGuid());

        // ownerName change is not allowed
        if (!StringUtils.equals(existingSearch.getOwnerName(), savedSearch.getOwnerName())) {
            throw new AtlasBaseException(AtlasErrorCode.SAVED_SEARCH_CHANGE_USER, existingSearch.getName(), existingSearch.getOwnerName(), savedSearch.getOwnerName());
        }

        // if renamed, ensure that there no other search with the new name exists
        if (!StringUtils.equals(existingSearch.getName(), savedSearch.getName())) {
            AtlasUserProfile userProfile = getUserProfile(existingSearch.getOwnerName());

            // check if a another search with this name already exists
            for (AtlasUserSavedSearch search : userProfile.getSavedSearches()) {
                if (StringUtils.equals(search.getName(), savedSearch.getName())) {
                    if (!StringUtils.equals(search.getGuid(), savedSearch.getGuid())) {
                        throw new AtlasBaseException(AtlasErrorCode.SAVED_SEARCH_ALREADY_EXISTS, savedSearch.getName(), savedSearch.getOwnerName());
                    }
                }
            }
        }

        return dataAccess.save(savedSearch);
    }

    public List<AtlasUserSavedSearch> getSavedSearches(String userName) throws AtlasBaseException {
        AtlasUserProfile profile = null;

        try {
            profile = getUserProfile(userName);
        } catch (AtlasBaseException excp) {
            // ignore
        }

        return (profile != null) ? profile.getSavedSearches() : null;
    }

    public AtlasUserSavedSearch getSavedSearch(String userName, String searchName) throws AtlasBaseException {
        AtlasUserSavedSearch ss = new AtlasUserSavedSearch(userName, searchName);

        return dataAccess.load(ss);
    }

    public AtlasUserSavedSearch getSavedSearch(String guid) throws AtlasBaseException {
        AtlasUserSavedSearch ss = new AtlasUserSavedSearch();

        ss.setGuid(guid);

        return dataAccess.load(ss);
    }

    public void deleteUserProfile(String userName) throws AtlasBaseException {
        AtlasUserProfile profile = getUserProfile(userName);

        dataAccess.delete(profile.getGuid());
    }

    public void deleteSavedSearch(String guid) throws AtlasBaseException {
        dataAccess.delete(guid);
    }

    public void deleteSearchBySearchName(String userName, String searchName) throws AtlasBaseException {
        dataAccess.delete(new AtlasUserSavedSearch(userName, searchName));
    }
}
