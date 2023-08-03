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
package org.apache.atlas.repository.ogm.profiles;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.profile.AtlasUserProfile;
import org.apache.atlas.model.profile.AtlasUserSavedSearch;
import org.apache.atlas.repository.ogm.AbstractDataTransferObject;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;

@Component
public class AtlasUserProfileDTO extends AbstractDataTransferObject<AtlasUserProfile> {
    public  static final String ENTITY_TYPE_NAME        = "__AtlasUserProfile";
    public  static final String PROPERTY_USER_NAME      = "name";
    private static final String PROPERTY_FULL_NAME      = "fullName";
    private static final String PROPERTY_SAVED_SEARCHES = "savedSearches";

    private final AtlasSavedSearchDTO savedSearchDTO;

    @Inject
    public AtlasUserProfileDTO(AtlasTypeRegistry typeRegistry, AtlasSavedSearchDTO savedSearchDTO) {
        super(typeRegistry, AtlasUserProfile.class, ENTITY_TYPE_NAME);

        this.savedSearchDTO = savedSearchDTO;
    }

    public AtlasUserProfile from(AtlasEntity entity) {
        AtlasUserProfile profile = new AtlasUserProfile();

        profile.setGuid(entity.getGuid());
        profile.setName((String) entity.getAttribute(PROPERTY_USER_NAME));
        profile.setFullName((String) entity.getAttribute(PROPERTY_FULL_NAME));

        return profile;
    }

    public AtlasUserProfile from(AtlasEntityWithExtInfo entityWithExtInfo) {
        AtlasUserProfile userProfile = from(entityWithExtInfo.getEntity());

        Object savedSearches = entityWithExtInfo.getEntity().getAttribute(PROPERTY_SAVED_SEARCHES);

        if (savedSearches instanceof Collection) {
            for (Object o : (Collection) savedSearches) {
                if (o instanceof AtlasObjectId) {
                    AtlasObjectId ssObjId  = (AtlasObjectId) o;
                    AtlasEntity   ssEntity = entityWithExtInfo.getReferredEntity(ssObjId.getGuid());

                    if (ssEntity != null && ssEntity.getStatus() == AtlasEntity.Status.ACTIVE) {
                        AtlasUserSavedSearch savedSearch = savedSearchDTO.from(ssEntity);
                        userProfile.getSavedSearches().add(savedSearch);
                    }
                }
            }
        }

        return userProfile;
    }

    @Override
    public AtlasEntity toEntity(AtlasUserProfile obj) throws AtlasBaseException {
        AtlasEntity entity = getDefaultAtlasEntity(obj);

        entity.setAttribute(PROPERTY_USER_NAME, obj.getName());
        entity.setAttribute(PROPERTY_FULL_NAME, obj.getFullName());

        return entity;
    }

    @Override
    public AtlasEntityWithExtInfo toEntityWithExtInfo(AtlasUserProfile obj) throws AtlasBaseException {
        AtlasEntity            entity            = toEntity(obj);
        AtlasEntityWithExtInfo entityWithExtInfo = new AtlasEntityWithExtInfo(entity);

        AtlasObjectId userProfileId = new AtlasObjectId(entity.getGuid(), AtlasUserProfileDTO.ENTITY_TYPE_NAME,
                                                        Collections.singletonMap(AtlasUserProfileDTO.PROPERTY_USER_NAME, obj.getName()));

        List<AtlasObjectId> objectIds = new ArrayList<>();

        for (AtlasUserSavedSearch ss : obj.getSavedSearches()) {
            AtlasEntity ssEntity = savedSearchDTO.toEntity(ss);

            ssEntity.setAttribute(AtlasSavedSearchDTO.PROPERTY_USER_PROFILE, userProfileId);

            entityWithExtInfo.addReferredEntity(ssEntity);

            objectIds.add(new AtlasObjectId(ssEntity.getGuid(), savedSearchDTO.getEntityType().getTypeName(), savedSearchDTO.getUniqueAttributes(ss)));
        }

        if (objectIds.size() > 0) {
            entity.setAttribute(PROPERTY_SAVED_SEARCHES, objectIds);
        }

        return entityWithExtInfo;
    }

    @Override
    public Map<String, Object> getUniqueAttributes(AtlasUserProfile obj) {
        Map<String, Object> ret = new HashMap<>();

        ret.put(PROPERTY_USER_NAME, obj.getName());

        return ret;
    }
}
